package com.example.smartassistant.logging

import android.util.Log
import timber.log.Timber

/**
 * Логер для release-збірки.
 *
 * Політика рівнів:
 *  - VERBOSE / DEBUG / INFO у release **відкидаються** (там можуть бути розпізнані
 *    фрази користувача — це персональні дані);
 *  - WARN / ERROR потрапляють у Logcat і (за потреби) передаються в систему
 *    збору збоїв, наприклад Firebase Crashlytics.
 */
class ReleaseTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val fullMessage = if (t == null) message else "$message\n${Log.getStackTraceString(t)}"
        Log.println(priority, tag ?: DEFAULT_TAG, fullMessage)
        // TODO: FirebaseCrashlytics.getInstance().log(fullMessage) / recordException(t)
    }

    private companion object {
        const val DEFAULT_TAG = "SmartAssistant"
    }
}
