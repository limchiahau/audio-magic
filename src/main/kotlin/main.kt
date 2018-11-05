/*
 * Developed by Lim Chia Hau on 10/29/18 6:54 PM.
 * Last modified 10/29/18 6:53 PM.
 * Copyright (c) 2018. All rights reserved.
 */


package com.chl.audiomagic


import kotlin.concurrent.timer


/*
 Simple UML
 Main -> EventLoop -> AudioManager <=> PacTL
 */


/*
 The Main Object
 This Object is responsible for user interaction.
 */
fun main(arg: Array<String>) {

    EventLoop(AudioManager::autoSwitchOutput)
            .start()

}


///////////////////////////////////////////////////////////////////////////////////////////


/*
 The Event Loop
 This class represents the main event loop of the program.
 */
class EventLoop(val fn: () -> Unit) {

    private val delay: Long = 1000 // in miliseconds

    fun start() {
        timer(period = delay) {
            fn()
        }
    }

}


////////////////////////////////////////////////////////////////////////////////////////////


/*
 The Audio Manager
 This object contains most of the logic of this program.
 It determines when the audio output device should be changed
 and changes it.
 */
object AudioManager {

    //there is no need to switch if the number of outputs are lower than 2
    //1) if there are no outputs there is nothing to switch to.
    //2) if there is 1 output it will automatically be of the highest priority.
    fun autoSwitchOutput() {
        if (hasOutput(2)) autoSwitch()
    }

    private fun hasOutput(numOutputs: Int): Boolean {
        return PacTL.getAudioOutputs().size >= numOutputs
    }

    private fun autoSwitch() {
        val allOutput = PacTL.getAudioOutputs()
        val enabledOutput = getEnabledAudioOutput(allOutput)
        val prioritizedOutput = getPrioritizedAudioOutput(allOutput)

        if (enabledOutput != prioritizedOutput)
            PacTL.setAudioOutputDeviceTo(prioritizedOutput)
    }

    private fun getPrioritizedAudioOutput(list: List<PacTL.AudioOutput>): PacTL.AudioOutput {
        return list
                .asSequence() //suggested by ide
                .sortedBy(PacTL.AudioOutput::priority)
                .last()
    }

    private fun getEnabledAudioOutput(list: List<PacTL.AudioOutput>): PacTL.AudioOutput? {
        return list.find(PacTL.AudioOutput::enabled)
    }
}


//////////////////////////////////////////////////////////////////////////////////////////////


/**
 PacTL
 This object is a wrapper for the pactl command line tool that interacts
 with the pulseaudio server.
 */
object PacTL {

    /**
     AudioOutput and AudioInput are nested in PacTL and given a internal
     constructor to guarantee that both of these objects can only
     be created by PacTL.
     This avoids situations where AudioOutput and AudioInput are created
     with invalid data and passed to PacTl.
     */
    data class AudioOutput internal constructor(
            val name: String,
            val id: Int,
            val priority: Int,
            val enabled: Boolean)

    data class AudioInput internal constructor(
            val id: Int
    )

    private val cache = Cache<List<AudioOutput>>()

    fun getAudioOutputs(): List<AudioOutput> {
        val rawAudioOut = bash("pactl list sinks")

        val listOfAudioOutput = cache.get(rawAudioOut)

        //println("hitrate : ${cache.hitRate()}")

        return listOfAudioOutput ?:

            parseRawAudioOut(rawAudioOut)
                    .apply {
                        cache.cache(rawAudioOut,this)
                    }

    }

    fun setAudioOutputDeviceTo(output: AudioOutput) {
        setDefaultSink(output)
        getAudioInputs().forEach {
            moveSinkInput(it, output)
        }
    }

    private fun parseRawAudioOut(rawAudioOut: String): List<AudioOutput> {

        //list of regexp to use against rawAudioOut
        return listOf(
            "Sink #[0-9]+",    //ID
            "Name: .+",         //Name
            "priority: [0-9]+",  //Priority
            "State: [A-W]+"   //Enabled

        ).map {
            it.regexp(rawAudioOut)

        }.toRows().map {
            AudioOutput(
                    id = it[0].parseId(),
                    name = it[1].parseName(),
                    priority = it[2].parsePriority(),
                    enabled = it[3].parseEnabled()
            )

        }
    }

    private fun getAudioInputs(): List<AudioInput> {
        val rawAudioIn = bash("pactl list sink-inputs")

        return "Sink Input #[0-9]+"
                .regexp(rawAudioIn)
                .map {it.parseId()}
                .map(::AudioInput)
    }

    private fun moveSinkInput(input: AudioInput, output: AudioOutput) {
        bash("pactl move-sink-input ${input.id} ${output.id}")
    }

    private fun setDefaultSink(output: AudioOutput) {
        bash("pactl set-default-sink ${output.name}")
    }

    private fun String.parseId(): Int {
        return split("#")
                .last()
                .toInt()
    }

    private fun String.parseName(): String {
        return split(" ")
                .last()
    }

    private fun String.parsePriority(): Int {
        return split(" ")
                .last()
                .toInt()
    }

    //indicates if the audio device is enabled or not
    //in the data, enabled audio output devices will have a * as the prefix
    private fun String.parseEnabled(): Boolean {
        return contains("RUNNING")
    }

    private fun String.regexp(match: String): List<String> {
        return toRegex()
                .findAll(match)
                .toList()
                .map(MatchResult::value)
    }

    private fun bash(cmd: String): String {
        return Runtime
                .getRuntime()
                .exec(cmd)
                .inputStream
                .reader()
                .readText()
    }
}


class Cache<T>(
        private val sizeLimit: Int = 1000,
        private val shouldRecordHitRate: Boolean = false
    ) {

    private val datastore = HashMap<String, T>()
    private var numAccess = 0.0
    private var numHits= 0.0


    fun get(key: String): T? {
        return datastore[key].apply {
            if (shouldRecordHitRate) { recordHitRate(this) }
        }
    }

    fun cache(key: String, data: T) {
        println(datastore.size)
        if (isFull()) flushCache()
        datastore.putIfAbsent(key, data)
    }

    //Output the hitRate as a double.
    fun hitRate() : Double =
      numHits/numAccess

    private fun recordHitRate(obj: T?) =
        if (obj == null) recordMiss() else recordHit()

    private fun recordHit() {
        numAccess += 1
        numHits += 1
    }

    private fun recordMiss() {
        numAccess += 1
    }

    private fun isFull(): Boolean =
            when(datastore.size) {
                0 -> false
                else -> datastore.size >= sizeLimit
            }

    private fun flushCache() =
        datastore.clear()
}


typealias Grid = List<List<String>>


/**
 * This is a recursive function that converts a list like this:
 *
 * [[1, 1, 1],
 * [2, 2, 2],
 * [3, 3, 3]]
 *
 * to
 *
 * [[1, 2, 3],
 * [1, 2, 3],
 * [1, 2, 3]]
 *
 * The purpose of this function is to make the string data obtained from
 * pactl easier to parse. As the information for each audio output device
 * are now grouped together in a list.
 */
fun Grid.toRows(): Grid {
    return if (hasNoRows()) {
        //remove the empty lists
        dropAll()
    } else {
        //call this function again but without the first row
        dropFirstRow().toRows()
                .plus(firstRow())
    }
}


//create a grid composing of the first row of the original grid
private fun Grid.firstRow(): Grid {
    return map {
        it.first()
    }.let(::listOf)
}


//All the inner lists are empty
private fun Grid.hasNoRows(): Boolean = first().isEmpty()


private fun Grid.dropAll(): Grid {
    return dropWhile { true }
}


private fun Grid.dropFirstRow(): Grid {
    return map {
        it.drop(1)
    }
}




