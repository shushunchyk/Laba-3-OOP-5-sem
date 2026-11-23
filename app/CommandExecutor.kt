package com.example.smartassistant.command

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import com.example.smartassistant.R
import com.example.smartassistant.i18n.StringProvider
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Виконує [Command] і повертає локалізовану відповідь, яку показують і озвучують. */
class CommandExecutor(
    private val context: Context,
    private val strings: StringProvider,
) {

    fun execute(command: Command): String {
        Timber.i("Executing command: %s", command)
        return when (command) {
            Command.Greeting -> strings.getString(R.string.reply_greeting)
            Command.Time -> strings.getString(R.string.reply_time, formatTime())
            Command.Date -> strings.getString(R.string.reply_date, formatDate())
            Command.Battery -> batteryReply()
            Command.OpenCamera -> openCamera()
            Command.Help -> strings.getString(R.string.reply_help)
            is Command.SetTimer -> setTimer(command.minutes)
            is Command.WebSearch -> webSearch(command.query)
            is Command.Unknown -> strings.getString(R.string.reply_unknown, command.text)
        }
    }

    private fun formatTime(): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(strings.locale()).format(LocalTime.now())

    private fun formatDate(): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(strings.locale()).format(LocalDate.now())

    private fun batteryReply(): String {
        val manager = context.getSystemService(BatteryManager::class.java)
        if (manager == null) {
            Timber.w("BatteryManager is unavailable")
            return strings.getString(R.string.reply_action_failed)
        }
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (level !in 0..100) {
            Timber.w("Battery level is unavailable (value=%d)", level)
            return strings.getString(R.string.reply_action_failed)
        }
        val id = if (manager.isCharging) R.string.reply_battery_charging else R.string.reply_battery
        return strings.getString(id, level)
    }

    private fun openCamera(): String {
        val ok = launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        return strings.getString(if (ok) R.string.reply_camera else R.string.reply_action_failed)
    }

    private fun setTimer(minutes: Int?): String {
        if (minutes == null) return strings.getString(R.string.reply_timer_need_minutes)
        if (minutes !in 1..MAX_TIMER_MINUTES) return strings.getString(R.string.reply_timer_invalid)
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
            .putExtra(AlarmClock.EXTRA_MESSAGE, strings.getString(R.string.app_name))
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return if (launch(intent)) {
            strings.getQuantityString(R.plurals.reply_timer_set, minutes, minutes)
        } else {
            strings.getString(R.string.reply_action_failed)
        }
    }

    private fun webSearch(query: String?): String {
        if (query == null) return strings.getString(R.string.reply_search_need_query)
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        return if (launch(intent)) {
            strings.getString(R.string.reply_search, query)
        } else {
            strings.getString(R.string.reply_action_failed)
        }
    }

    /** Запускає системну дію; відсутність потрібного застосунку не повинна валити програму. */
    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No activity found for %s", intent.action)
        false
    } catch (e: SecurityException) {
        Timber.w(e, "Not permitted to start %s", intent.action)
        false
    }

    private companion object {
        const val MAX_TIMER_MINUTES = 24 * 60
    }
}
