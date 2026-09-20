package com.android.libredialer.view.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

val SettingsSmoothEase = FastOutSlowInEasing
val SettingsSmoothEaseIn = FastOutLinearInEasing
val SettingsSmoothEaseOut = LinearOutSlowInEasing

const val SETTINGS_ANIM_DURATION_ENTER = 400
const val SETTINGS_ANIM_DURATION_EXIT = 400
const val SETTINGS_SCALE_ENTER_FROM = 0.92f
const val SETTINGS_SCALE_EXIT_TO = 0.96f

val settingsRoutes = setOf(
    "settings_screen",
    "contact_details_screen",
    "call_log_detail_screen",
    "search_screen",
    "contact_edit_screen",
    "hidden_contacts_screen",
    "interface_screen",
    "app_settings_screen",
    "sound_vibration_screen",
    "biometric_screen",
    "call_settings_screen",
    "caller_ui_screen",
    "caller_u_i_screen",
    "incoming_call_ui_screen",
    "incoming_call_u_i_screen",
    "rain_mode_screen",
    "fake_call_screen",
    "contacts_hider_screen",
    "custom_background_picker_screen",
    "call_accounts_screen",
    "default_message_app_screen",
    "raise_to_answer_screen",
    "updates_screen",
    "about_app_screen",
    "contributors_screen"
)

fun isSettingsRoute(route: String?): Boolean {
    if (route == null) return false
    val base = route.substringBefore("?").substringBefore("/")
    return base in settingsRoutes ||
        base.contains("settings", ignoreCase = true) ||
        base.contains("about", ignoreCase = true) ||
        base.contains("contact_details", ignoreCase = true) ||
        base.contains("call_log_detail", ignoreCase = true) ||
        base.contains("search", ignoreCase = true) ||
        base.contains("contact_edit", ignoreCase = true)
}

object SettingsTransitionStyle : NavHostAnimatedDestinationStyle() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        scaleIn(
            animationSpec = tween(SETTINGS_ANIM_DURATION_ENTER, easing = SettingsSmoothEase),
            initialScale = SETTINGS_SCALE_ENTER_FROM,
            transformOrigin = TransformOrigin.Center
        ) + fadeIn(tween(SETTINGS_ANIM_DURATION_ENTER, easing = SettingsSmoothEaseOut))
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        scaleOut(
            animationSpec = tween(SETTINGS_ANIM_DURATION_EXIT, easing = SettingsSmoothEase),
            targetScale = SETTINGS_SCALE_EXIT_TO,
            transformOrigin = TransformOrigin.Center
        ) + fadeOut(tween(SETTINGS_ANIM_DURATION_EXIT, easing = SettingsSmoothEaseIn))
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        scaleIn(
            animationSpec = tween(SETTINGS_ANIM_DURATION_ENTER, easing = SettingsSmoothEase),
            initialScale = SETTINGS_SCALE_EXIT_TO,
            transformOrigin = TransformOrigin.Center
        ) + fadeIn(tween(SETTINGS_ANIM_DURATION_ENTER, easing = SettingsSmoothEaseOut))
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        scaleOut(
            animationSpec = tween(SETTINGS_ANIM_DURATION_EXIT, easing = SettingsSmoothEase),
            targetScale = SETTINGS_SCALE_ENTER_FROM,
            transformOrigin = TransformOrigin.Center
        ) + fadeOut(tween(SETTINGS_ANIM_DURATION_EXIT, easing = SettingsSmoothEaseIn))
    }
}
