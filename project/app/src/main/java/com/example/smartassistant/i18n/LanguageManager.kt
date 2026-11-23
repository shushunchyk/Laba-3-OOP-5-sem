package com.example.smartassistant.i18n

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.smartassistant.R
import timber.log.Timber
import java.util.Locale

data class AppLanguage(val tag: String, @StringRes val nativeNameRes: Int)

/**
 * Мови застосунку та їх перемикання через Per-app language API (AppCompat):
 *  - Android 13+ — системні налаштування «Мова застосунку» + locales_config.xml;
 *  - Android 8–12 — AppCompat зберігає вибір сам (AppLocalesMetadataHolderService у маніфесті).
 *
 * Щоб додати мову: values-xx/{strings,commands}.xml + запис нижче + locales_config.xml.
 */
object LanguageManager {

    val supported: List<AppLanguage> = listOf(
        AppLanguage("en", R.string.language_en),
        AppLanguage("uk", R.string.language_uk),
        AppLanguage("sk", R.string.language_sk),
    )

    /** Мова, яку реально показують ресурси (з урахуванням fallback на англійську). */
    fun current(context: Context): AppLanguage {
        val language = Locale.forLanguageTag(context.getString(R.string.locale_tag)).language
        return supported.firstOrNull { it.tag == language } ?: supported.first()
    }

    fun apply(language: AppLanguage) {
        Timber.i("Switching app language to '%s'", language.tag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
