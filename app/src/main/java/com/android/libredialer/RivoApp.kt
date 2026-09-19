package com.android.libredialer

import com.coolappstore.evercallrecorder.by.svhp.ShizuApplication
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.view.screen.settings.applyIcon
import com.android.libredialer.view.screen.settings.buildIcons
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

// Extends ShizuApplication (Ever Call Recorder's Application class) so its own
// startup init (AppLogger, etc.) still runs now that Recorder is bundled in-app.
class RivoApp : ShizuApplication() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RivoApp)
            modules(appModule)
        }
        restoreSavedAppIcon()
        com.android.libredialer.controller.FakeCallConnectionService.ensureRegistered(this)
        initMissedCallBadgeObserver()
    }

    private fun initMissedCallBadgeObserver() {
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                contentResolver.registerContentObserver(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    true,
                    object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            com.android.libredialer.controller.util.MissedCallBadgeManager.updateBadge(this@RivoApp)
                        }
                    }
                )
                com.android.libredialer.controller.util.MissedCallBadgeManager.updateBadge(this)
            }
        } catch (_: Throwable) {}
    }

    private fun restoreSavedAppIcon() {
        try {
            val prefs = PreferenceManager(this)
            val savedKey = prefs.getString(com.android.libredialer.view.screen.settings.KEY_SELECTED_APP_ICON, "radium_green_phone") ?: "radium_green_phone"
            val icons = buildIcons(this)
            val entry = icons.find { it.key == savedKey } ?: icons.first()
            applyIcon(this, prefs, entry)
        } catch (_: Exception) {}
    }
}
