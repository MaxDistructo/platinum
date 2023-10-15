package io.dedyn.engineermantra.omega.bot.voice

import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.StreamSpeechRecognizer
import java.nio.ByteBuffer
import java.nio.ByteOrder

object VoiceProcessing {
    //Raw 48KHz 16bit stereo signed BigEndian PCM
    public val audioBuffer = mutableMapOf<Long, Byte>()

    //Name is generic enough here so put the full class path in.
    val speechConfig: edu.cmu.sphinx.api.Configuration = Configuration()
    init {
        speechConfig.acousticModelPath = "resource:/edu/cmu/sphinx/models/en-us/en-us"
        speechConfig.dictionaryPath = "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict"
        speechConfig.languageModelPath = "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin"
    }

    class VoiceProcessingThread: Thread(){

        override fun run() {
            //Parsing Fun!
            //Break all recorded audio up by user and split it out


            val audioBuffer = ByteBuffer.allocate(audioBuffer.size*16)
            audioBuffer.order(ByteOrder.LITTLE_ENDIAN)
            //Errors are saying that we need to reallocated this EVERY TIME. Annoying af
            val speechRecognizer: StreamSpeechRecognizer = StreamSpeechRecognizer(speechConfig)
            speechRecognizer.startRecognition(ByteBufferInputStream(audioBuffer))
            while(speechRecognizer.result != null)
            {
                if(speechRecognizer.result.hypothesis != null) {
                    //println(userAudio.user.name + " : " + speechRecognizer.result.hypothesis)
                }
            }
        }
    }
}