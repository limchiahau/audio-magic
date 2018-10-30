/*
 * Developed by Lim Chia Hau on 10/29/18 6:54 PM.
 * Last modified 10/29/18 6:53 PM.
 * Copyright (c) 2018. All rights reserved.
 */

package com.chl.audiomagic


import kotlin.concurrent.timer


fun main(arg: Array<String>) {

    EventLoop(AudioManager::autoSwitchOutput)
            .start()

}


///////////////////////////////////////////////////////////////////////////////////////////


class EventLoop(val fn: () -> Unit) {

    private val delay: Long = 1000 // in miliseconds

    fun start() {
        timer(period = delay) {
            fn()
        }
    }

}


////////////////////////////////////////////////////////////////////////////////////////////


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

    private fun getPrioritizedAudioOutput(list: List<AudioOutput>): AudioOutput {
        return list
                .asSequence() //suggested by ide
                .sortedBy(AudioOutput::priority)
                .last()
    }

    private fun getEnabledAudioOutput(list: List<AudioOutput>): AudioOutput? {
        return list.find(AudioOutput::enabled)
    }
}


//////////////////////////////////////////////////////////////////////////////////////////////


object PacTL {

    fun getAudioOutputs(): List<AudioOutput> {

        val rawAudioOut = bash("pactl list sinks")

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
                    id = it.get(0).parseId(),
                    name = it.get(1).parseName(),
                    priority = it.get(2).parsePriority(),
                    enabled = it.get(3).parseEnabled()
            )

        }
    }

    fun setAudioOutputDeviceTo(output: AudioOutput) {
        setDefaultSink(output)
        getAudioInputs().forEach {
            moveSinkInput(it, output)
        }
    }

    private fun getAudioInputs(): List<AudioInput> {
        val rawAudioIn = bash("pactl list sink-inputs")

        return "Sink Input #[0-9]+"
                .regexp(rawAudioIn)
                .map({it.parseId()})
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
}


typealias Grid = List<List<String>>


//this is a recursive function that ?
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
fun Grid.firstRow(): Grid {
    return map {
        it.first()
    }.let(::listOf)
}


//All the inner lists are empty
fun Grid.hasNoRows(): Boolean = first().isEmpty()


fun Grid.dropAll(): Grid {
    return dropWhile { true }
}


fun Grid.dropFirstRow(): Grid {
    return map {
        it.drop(1)
    }
}


fun bash(cmd: String): String {
    return Runtime
            .getRuntime()
            .exec(cmd)
            .inputStream
            .reader()
            .readText()
}


/////////////////////////////////////////////////////////////////////////////////////////DATA


data class AudioOutput(val name: String, val id: Int, val priority: Int, val enabled: Boolean)


data class AudioInput(val id: Int)



