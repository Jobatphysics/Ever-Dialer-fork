package com.android.libredialer.view.components

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView
import com.android.libredialer.controller.util.PreferenceManager
import org.koin.compose.koinInject

enum class AppHapticEvent {
    CLICK,
    SELECTION,
    LONG_PRESS,
    THRESHOLD
}

fun shouldPerformAppHaptic(enabled: Boolean, changed: Boolean): Boolean = enabled && changed

fun hapticEventForStrength(strength: String, customIntensity: Float): AppHapticEvent =
    when (strength) {
        "strong" -> AppHapticEvent.LONG_PRESS
        "custom" -> if (customIntensity >= 0.75f) AppHapticEvent.CLICK else AppHapticEvent.SELECTION
        else -> AppHapticEvent.CLICK
    }

fun performSystemHaptic(view: View, event: AppHapticEvent, enabled: Boolean = true) {
    if (!shouldPerformAppHaptic(enabled, changed = true)) return
    val constant = when (event) {
        AppHapticEvent.CLICK -> HapticFeedbackConstants.CONTEXT_CLICK
        AppHapticEvent.SELECTION,
        AppHapticEvent.THRESHOLD -> HapticFeedbackConstants.CLOCK_TICK
        AppHapticEvent.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
    }
    view.performHapticFeedback(constant)
}

fun previewSystemHaptic(context: Context, event: AppHapticEvent) {
    var base = context
    while (base is android.content.ContextWrapper && base !is android.app.Activity) {
        base = base.baseContext
    }
    val view = (base as? android.app.Activity)?.window?.decorView ?: return
    performSystemHaptic(view, event)
}

@Composable
fun rememberAppHapticFeedback(): (AppHapticEvent, Boolean) -> Unit {
    val preferenceManager = koinInject<PreferenceManager>()
    val settingsVersion by preferenceManager.settingsChanged.collectAsState()
    val enabled = remember(settingsVersion) {
        preferenceManager.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)
    }
    val view = LocalView.current
    return remember(view, enabled) {
        { event: AppHapticEvent, changed: Boolean ->
            if (shouldPerformAppHaptic(enabled, changed)) {
                performSystemHaptic(view, event, enabled = true)
            }
        }
    }
}

fun performAppHaptic(
    context: Context,
    strength: String,
    customIntensity: Float = 0.5f
) {
    var base = context
    while (base is android.content.ContextWrapper && base !is android.app.Activity) {
        base = base.baseContext
    }
    val prefs = PreferenceManager(context)
    val enabled = prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)
    val view = (base as? android.app.Activity)?.window?.decorView ?: return
    performSystemHaptic(view, hapticEventForStrength(strength, customIntensity), enabled)
}

fun performScrollHaptic(context: Context, amplitude: Int = 60) {
    var base = context
    while (base is android.content.ContextWrapper && base !is android.app.Activity) {
        base = base.baseContext
    }
    val enabled = PreferenceManager(context).getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)
    val view = (base as? android.app.Activity)?.window?.decorView ?: return
    performSystemHaptic(view, AppHapticEvent.THRESHOLD, enabled)
}

@Composable
fun rememberComposeAppHaptic(): (HapticFeedbackType) -> Unit {
    val prefs = koinInject<PreferenceManager>()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val enabled = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true) }
    val view = LocalView.current
    return remember(view, enabled) {
        { type ->
            val event = when (type) {
                HapticFeedbackType.LongPress -> AppHapticEvent.LONG_PRESS
                HapticFeedbackType.TextHandleMove -> AppHapticEvent.SELECTION
                else -> AppHapticEvent.CLICK
            }
            performSystemHaptic(view, event, enabled)
        }
    }
}
