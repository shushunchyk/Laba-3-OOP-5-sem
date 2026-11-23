package com.example.smartassistant.command

import com.example.smartassistant.R
import com.example.smartassistant.i18n.StringProvider
import timber.log.Timber
import java.text.Normalizer

/**
 * Перетворює розпізнаний текст на [Command].
 *
 * Ключові фрази беруться з локалізованих ресурсів (res/values-xx/commands.xml),
 * тож підтримка нової мови не потребує змін у коді.
 *
 * Правило збігу фрази: усі її слова присутні в сказаному (порядок не важливий).
 */
class CommandParser(private val strings: StringProvider) {

    fun parse(rawText: String): Command {
        val locale = strings.locale()
        val text = Normalizer.normalize(rawText, Normalizer.Form.NFC).lowercase(locale).trim()
        val words = text.split(NON_WORD).filter { it.isNotEmpty() }.toSet()

        // Пошук першим: у запиті можуть траплятися слова інших команд («знайди камеру»).
        parseSearch(text)?.let { return it }

        val command = when {
            matches(words, R.array.kw_timer) -> parseTimer(text, words)
            matches(words, R.array.kw_camera) -> Command.OpenCamera
            matches(words, R.array.kw_battery) -> Command.Battery
            matches(words, R.array.kw_time) -> Command.Time
            matches(words, R.array.kw_date) -> Command.Date
            matches(words, R.array.kw_help) -> Command.Help
            matches(words, R.array.kw_greeting) -> Command.Greeting
            else -> Command.Unknown(rawText.trim())
        }
        Timber.v("parse('%s') -> %s", rawText, command)
        return command
    }

    private fun matches(words: Set<String>, keywordsRes: Int): Boolean {
        val locale = strings.locale()
        return strings.getStringArray(keywordsRes).any { phrase ->
            val phraseWords = phrase.lowercase(locale).split(' ').filter { it.isNotEmpty() }
            phraseWords.isNotEmpty() && words.containsAll(phraseWords)
        }
    }

    private fun parseTimer(text: String, words: Set<String>): Command.SetTimer {
        val number = DIGITS.find(text)?.value?.toIntOrNull()?.coerceAtMost(MAX_NUMBER)
        val isHours = matches(words, R.array.kw_unit_hour)
        return Command.SetTimer(number?.let { if (isHours) it * 60 else it })
    }

    private fun parseSearch(text: String): Command.WebSearch? {
        val locale = strings.locale()
        val triggers = strings.getStringArray(R.array.kw_search)
            .map { it.lowercase(locale) }
            .sortedByDescending { it.length } // довші тригери мають пріоритет
        for (trigger in triggers) {
            val match = Regex("(?<![\\p{L}\\p{N}])${Regex.escape(trigger)}(?![\\p{L}\\p{N}])").find(text) ?: continue
            val query = text.substring(match.range.last + 1).trim()
            return Command.WebSearch(query.ifEmpty { null })
        }
        return null
    }

    private companion object {
        val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        val DIGITS = Regex("\\d+")
        const val MAX_NUMBER = 100_000
    }
}
