package io.dedyn.engineermantra.omega.bot.voice

import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.StreamSpeechRecognizer
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.UserAudio
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ReceivingHandler: AudioReceiveHandler {


    override fun canReceiveUser(): Boolean {
        return true
    }

    override fun handleUserAudio(userAudio: UserAudio) {
        //Audio from Discord is: 48KHz 16bit stereo signed BigEndian PCM
        //We need: 16khz 16bit little-endian mono PCM
        val audioData = userAudio.getAudioData(1.0)
        //VoiceProcessing.audioBuffer.addAll(userAudio.getAudioData(1.0).toList())

    }
}