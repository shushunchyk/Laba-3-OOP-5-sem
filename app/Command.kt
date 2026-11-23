package com.example.smartassistant.command

/** Усі команди, які розуміє помічник. Нова команда = новий підтип + гілка в парсері й виконавці. */
sealed interface Command {
    data object Greeting : Command
    data object Time : Command
    data object Date : Command
    data object Battery : Command
    data object OpenCamera : Command
    data object Help : Command

    /** [minutes] == null, якщо користувач не назвав тривалість. */
    data class SetTimer(val minutes: Int?) : Command

    /** [query] == null, якщо після тригера нічого не сказано. */
    data class WebSearch(val query: String?) : Command

    data class Unknown(val text: String) : Command
}
