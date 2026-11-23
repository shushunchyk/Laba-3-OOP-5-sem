package com.example.smartassistant.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.example.smartassistant.R
import java.util.Locale

/**
 * Абстракція доступу до локалізованих ресурсів.
 * Дає змогу тестувати парсер команд без Android (див. CommandParserTest).
 */
interface StringProvider {
    fun getString(@StringRes id: Int, vararg args: Any): String
    fun getQuantityString(@PluralsRes id: Int, quantity: Int, vararg args: Any): String
    fun getStringArray(@ArrayRes id: Int): List<String>

    /** Локаль, якою фактично користується застосунок (для дат, часу й нижнього регістру). */
    fun locale(): Locale
}

/**
 * Реалізація поверх Android-ресурсів. Мова береться **в момент виклику** з
 * [AppCompatDelegate.getApplicationLocales], тому перемикання мови в застосунку
 * діє одразу — навіть у ViewModel, що пережила перестворення Activity.
 */
class AndroidStringProvider(private val appContext: Context) : StringProvider {

    override fun getString(id: Int, vararg args: Any): String = localizedContext().getString(id, *args)

    override fun getQuantityString(id: Int, quantity: Int, vararg args: Any): String =
        localizedContext().resources.getQuantityString(id, quantity, *args)

    override fun getStringArray(id: Int): List<String> =
        localizedContext().resources.getStringArray(id).toList()

    override fun locale(): Locale = Locale.forLanguageTag(getString(R.string.locale_tag))

    private fun localizedContext(): Context {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (appLocales.isEmpty) return appContext // мова не вибрана вручну → мова системи
        val config = Configuration(appContext.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(appLocales.toLanguageTags()))
        return appContext.createConfigurationContext(config)
    }
}
