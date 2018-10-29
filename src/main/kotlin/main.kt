package com.chl.audiomagic

fun main(arg: Array<String>) {
    val outputs = getListOfAudioOutput()

    //do nothing if there are no audio output devices
    if (outputs.isNotEmpty()) {
        switchAudioOutput(
                getAudioInput(),
                getEnabledAudioOutput(outputs),
                getPrioritizedAudioOutput(outputs)
        )
    }
}

///////////////////////////////////////////////////////////////////FUNCTIONS


fun getListOfAudioOutput(): List<AudioOutput> {
    val rawAudioOut = bash("pacmd list-sinks")

    //list of regexp to use against rawAudioOut
    return listOf(
            "index: [0-9]+",    //ID
            "name: .+",         //Name
            "priority [0-9]+",  //Priority
            ". index: [0-9]+"   //Enabled

    ).map {
        it.regexp(rawAudioOut)

    }.let {
        it.toRows()

    }.map {
        AudioOutput(
                id = it.get(0).parseId(),
                name = it.get(1).parseName(),
                priority = it.get(2).parsePriority(),
                enabled = it.get(3).parseEnabled()
        )

    }
}


fun getPrioritizedAudioOutput(list: List<AudioOutput>): AudioOutput {
    return list
            .asSequence() //suggested by ide
            .sortedBy(AudioOutput::priority)
            .last()
}


fun getEnabledAudioOutput(list: List<AudioOutput>): AudioOutput {
    val device = list.find(AudioOutput::enabled)
    if (device == null) {
        throw Exception("No audio output device found")
    } else {
        return device
    }
}


fun getAudioInput(): AudioInput {
    val rawAudioIn = bash("pacmd list-sink-inputs")

    return "index: [0-9]+"
            .regexp(rawAudioIn)
            .map(String::parseId)
            .let(::AudioInput)
}


////////////////////////////////////////////////////////////////////////EXTENSIONS


fun String.parseId(): Int {
    return split(" ")
            .last()
            .toInt()
}


fun String.parseName(): String {
    return split(" ")
            .last()
            .drop(1) //remove the "<" in front
            .dropLast(1) //"remove the ">" from the back
}


fun String.parsePriority(): Int {
    return split(" ")
            .last()
            .toInt()
}


//indicates if the audio device is enabled or not
//in the data, enabled audio output devices will have a * as the prefix
fun String.parseEnabled(): Boolean {
    return contains('*')
}


fun String.regexp(match: String): List<String> {
    return toRegex()
            .findAll(match)
            .toList()
            .map(MatchResult::value)
}


/////////////////////////////////////////////////////////////////////////////CLASS


fun switchAudioOutput(audioInput: AudioInput,
                      enabledAudioOutput: AudioOutput,
                      prioritizedAudioOutput: AudioOutput) {

    fun switchAudioOutputTo(audioOut: AudioOutput) {
        audioInput.setAudioOutputDeviceTo(audioOut)
    }

    fun isNotPrioritized(audioOut: AudioOutput): Boolean
            = audioOut != prioritizedAudioOutput

    if (isNotPrioritized(enabledAudioOutput))
        switchAudioOutputTo(prioritizedAudioOutput)
}


data class AudioOutput(val name: String, val id: Int, val priority: Int, val enabled: Boolean)


//use id default if there are no available sink inputs
class AudioInput(private val ids: List<Int>) {
    fun setAudioOutputDeviceTo(audioOut: AudioOutput) {
        setDefaultSink(audioOut)
        moveSinkInput(audioOut)
    }

    private fun moveSinkInput(audioOut: AudioOutput) {
        if (thereAreActiveSinkInputs()) {
            ids.forEach {
                bash("pacmd move-sink-input $it ${audioOut.id}")
            }
        }
    }

    private fun thereAreActiveSinkInputs(): Boolean {
        return ids.isNotEmpty()
    }

    private fun setDefaultSink(audioOut: AudioOutput) {
        println("Setting default audio output to: ${audioOut.name}")
        bash("pacmd set-default-sink ${audioOut.name}")
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


////////////////////////////////////////////////////////////////////////////////////HELPER


fun bash(cmd: String): String {
    return Runtime
            .getRuntime()
            .exec(cmd)
            .inputStream
            .reader()
            .readText()
}
