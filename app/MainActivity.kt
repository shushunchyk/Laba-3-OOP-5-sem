package com.example.smartassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartassistant.R
import com.example.smartassistant.databinding.ActivityMainBinding
import com.example.smartassistant.i18n.LanguageManager
import com.example.smartassistant.voice.SpeechSynthesizer
import com.example.smartassistant.voice.VoiceError
import com.example.smartassistant.voice.VoiceRecognizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val adapter = ChatAdapter()

    private lateinit var recognizer: VoiceRecognizer
    private lateinit var speaker: SpeechSynthesizer

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Timber.i("RECORD_AUDIO granted")
                startListening()
            } else {
                Timber.w("RECORD_AUDIO denied")
                viewModel.onVoiceError(VoiceError.PERMISSION)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Timber.d("MainActivity created")

        // Мова розпізнавання й озвучення = мова поточного набору ресурсів (res/values-xx).
        val languageTag = getString(R.string.locale_tag)
        speaker = SpeechSynthesizer(this, languageTag)
        recognizer = VoiceRecognizer(
            context = this,
            onListening = viewModel::onListeningStarted,
            onText = viewModel::onSpeechRecognized,
            onFailure = viewModel::onVoiceError,
        )

        binding.messages.layoutManager = LinearLayoutManager(this)
        binding.messages.adapter = adapter
        binding.btnLanguage.text = getString(LanguageManager.current(this).nativeNameRes)
        binding.btnLanguage.setOnClickListener { showLanguageDialog() }
        binding.fabMic.setOnClickListener { onMicClicked() }

        // Озвучення — поки жива Activity; оновлення UI — поки вона видима.
        lifecycleScope.launch { viewModel.speech.collect { speaker.speak(it) } }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.state.collect { render(it) } }
        }
    }

    private fun render(state: UiState) {
        adapter.submitList(state.messages) {
            if (state.messages.isNotEmpty()) binding.messages.scrollToPosition(state.messages.lastIndex)
        }
        val listening = state.status == Status.LISTENING
        binding.tvStatus.setText(if (listening) R.string.status_listening else R.string.status_idle)
        binding.fabMic.setImageResource(if (listening) R.drawable.ic_stop else R.drawable.ic_mic)
        binding.fabMic.contentDescription =
            getString(if (listening) R.string.action_stop_listening else R.string.action_start_listening)
    }

    private fun onMicClicked() {
        when {
            viewModel.state.value.status == Status.LISTENING -> {
                recognizer.cancel()
                viewModel.onListeningStopped()
            }
            hasAudioPermission() -> startListening()
            else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        speaker.stop() // не слухати власний голос
        recognizer.start(getString(R.string.locale_tag))
    }

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun showLanguageDialog() {
        val languages = LanguageManager.supported
        val names = languages.map { getString(it.nativeNameRes) }.toTypedArray()
        val checked = languages.indexOf(LanguageManager.current(this))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_language)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                dialog.dismiss()
                if (which != checked) LanguageManager.apply(languages[which]) // Activity перестворюється
            }
            .show()
    }

    override fun onDestroy() {
        recognizer.release()
        speaker.shutdown()
        viewModel.onListeningStopped()
        Timber.d("MainActivity destroyed")
        super.onDestroy()
    }
}
