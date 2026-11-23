package com.example.smartassistant

import android.app.Application
import com.example.smartassistant.logging.ReleaseTree
import timber.log.Timber

/**
 * Точка входу застосунку. Єдине місце, де налаштовується логування.
 *
 *  - debug   → [Timber.DebugTree]: усі рівні, тег = ім’я класу, що викликав лог;
 *  - release → [ReleaseTree]: тільки WARN і ERROR.
 */
class SmartAssistantApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initLogging()
        Timber.i("Application started: version %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}
