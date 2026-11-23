package com.example.smartassistant.voice

import androidx.annotation.StringRes
import com.example.smartassistant.R

/** Помилки голосового введення, незалежні від кодів Android; кожна має локалізоване повідомлення. */
enum class VoiceError(@StringRes val messageRes: Int) {
    NO_MATCH(R.string.error_no_match),
    SPEECH_TIMEOUT(R.string.error_speech_timeout),
    NETWORK(R.string.error_network),
    AUDIO(R.string.error_audio),
    PERMISSION(R.string.error_permission),
    BUSY(R.string.error_busy),
    UNAVAILABLE(R.string.error_unavailable),
    LANGUAGE(R.string.error_language),
    UNKNOWN(R.string.error_unknown),
}
