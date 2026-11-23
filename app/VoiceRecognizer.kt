package com.example.smartassistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import timber.log.Timber

/**
 * Обгортка над [SpeechRecognizer]: одна фраза за один запуск (push-to-talk).
 * Усі виклики — з головного потоку.
 */
class VoiceRecognizer(
    private val context: Context,
    private val onListening: () -> Unit,
    private val onText: (String) -> Unit,
    private val onFailure: (VoiceError) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    fun start(languageTag: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Timber.e("Speech recognition service is not available")
            onFailure(VoiceError.UNAVAILABLE)
            return
        }
        release()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        Timber.d("startListening (language=%s)", languageTag)
        recognizer?.startListening(intent)
    }

    /** Скасовує поточне розпізнавання без результату. */
    fun cancel() {
        Timber.d("cancel listening")
        recognizer?.cancel()
        release()
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Timber.d("Ready for speech")
            onListening()
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
            release()
            if (text == null) {
                Timber.w("Recognition finished with empty result")
                onFailure(VoiceError.NO_MATCH)
            } else {
                onText(text)
            }
        }

        override fun onError(error: Int) {
            val mapped = mapError(error)
            Timber.w("Recognition error: code=%d -> %s", error, mapped)
            release()
            onFailure(mapped)
        }

        override fun onBeginningOfSpeech() = Timber.v("Beginning of speech")
        override fun onEndOfSpeech() = Timber.v("End of speech")
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun mapError(code: Int): VoiceError = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> VoiceError.NO_MATCH
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceError.SPEECH_TIMEOUT
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER -> VoiceError.NETWORK
        SpeechRecognizer.ERROR_AUDIO -> VoiceError.AUDIO
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PERMISSION
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceError.BUSY
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> VoiceError.LANGUAGE
        else -> VoiceError.UNKNOWN
    }
}
