package com.example.smartassistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import timber.log.Timber
import java.util.Locale

/** Обгортка над [TextToSpeech]. Якщо для мови немає голосу — відповідь лишається лише текстовою. */
class SpeechSynthesizer(context: Context, private val languageTag: String) {

    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        onInit(status)
    }

    private fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Timber.e("TextToSpeech initialization failed (status=%d)", status)
            return
        }
        when (val result = tts.setLanguage(Locale.forLanguageTag(languageTag))) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED ->
                Timber.w("TTS language '%s' is not available (result=%d)", languageTag, result)
            else -> {
                ready = true
                Timber.d("TextToSpeech ready (language=%s)", languageTag)
            }
        }
    }

    fun speak(text: String) {
        if (!ready) {
            Timber.w("TextToSpeech is not ready; reply will not be spoken")
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        ready = false
    }

    private companion object {
        const val UTTERANCE_ID = "assistant-reply"
    }
}
