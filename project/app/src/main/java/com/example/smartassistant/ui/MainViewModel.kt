package com.example.smartassistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartassistant.R
import com.example.smartassistant.command.CommandExecutor
import com.example.smartassistant.command.CommandParser
import com.example.smartassistant.i18n.AndroidStringProvider
import com.example.smartassistant.i18n.StringProvider
import com.example.smartassistant.voice.VoiceError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Стан чату + обробка команд. Не знає про Activity, мікрофон чи динаміки:
 * отримує розпізнаний текст, а відповідь віддає у [state] (показ) і [speech] (озвучення).
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val strings: StringProvider = AndroidStringProvider(application)
    private val parser = CommandParser(strings)
    private val executor = CommandExecutor(application, strings)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _speech = Channel<String>(Channel.BUFFERED)

    /** Одноразові події «озвуч цей текст». */
    val speech: Flow<String> = _speech.receiveAsFlow()

    private var nextId = 0L

    init {
        Timber.d("MainViewModel created")
        addMessage(strings.getString(R.string.welcome_message), Author.ASSISTANT)
    }

    fun onListeningStarted() = setStatus(Status.LISTENING)

    fun onListeningStopped() = setStatus(Status.IDLE)

    fun onSpeechRecognized(text: String) {
        onListeningStopped()
        Timber.d("Recognized speech: '%s'", text)
        addMessage(text, Author.USER)
        val command = parser.parse(text)
        Timber.i("Parsed command: %s", command)
        reply(executor.execute(command))
    }

    fun onVoiceError(error: VoiceError) {
        onListeningStopped()
        Timber.w("Voice error: %s", error)
        reply(strings.getString(error.messageRes))
    }

    private fun reply(text: String) {
        addMessage(text, Author.ASSISTANT)
        _speech.trySend(text)
    }

    private fun setStatus(status: Status) = _state.update { it.copy(status = status) }

    private fun addMessage(text: String, author: Author) {
        val message = ChatMessage(nextId++, text, author)
        _state.update { it.copy(messages = it.messages + message) }
    }
}
