package de.develappers.facerecognition.TTS

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import de.develappers.facerecognition.listeners.SpeechListener
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text
import java.util.*

class TTS () {

    var textToSpeech: TextToSpeech? = null
    var listener: SpeechListener? = null

    fun initialize(context: Context, text: String) {
        textToSpeech =  TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val ttsLang = textToSpeech!!.setLanguage(Locale.US)
                if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                    || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e("TTS", "The Language is not supported!")
                } else {
                    Log.i("TTS", "Language Supported.")
                }
                Log.i("TTS", "Initialization success.")

                speak(text)
            } else {
                Toast.makeText(context, "TTS Initialization failed!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun speak(text: String, utteranceId: String? = null){
        val speechStatus = textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!")
        }
    }

    fun setSpeechListener(listener: SpeechListener){
        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

            override fun onDone(utteranceId: String?) {
                listener.onSpeechFinished()
            }

            override fun onError(utteranceId: String?) {
                //do whatever you want if TTS makes an error.
            }

            override fun onStart(utteranceId: String?) {
                //do whatever you want when TTS start speaking.
            }
        })
    }
}