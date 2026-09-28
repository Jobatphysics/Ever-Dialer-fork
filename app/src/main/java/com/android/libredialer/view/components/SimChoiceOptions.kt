package com.android.libredialer.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.libredialer.controller.util.PreferenceManager

data class SimChoiceOption(
    val value: String,
    val label: String,
    val subLabel: String? = null,
    val icon: ImageVector
)

val SIM_CHOICE_OPTIONS = listOf(
    SimChoiceOption(PreferenceManager.SIM_CHOICE_SETTINGS, "According to Settings", "Use the app-wide default SIM setting", Icons.Default.Tune),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_ASK, "Ask Every Time", "Show the SIM picker on every call", Icons.Default.HelpOutline),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_SIM1, "SIM 1", null, Icons.Default.SimCard),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_SIM2, "SIM 2", null, Icons.Default.SimCard),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_CALL_LOG, "Use SIM based on call logs", "Automatically select the same SIM from call log history", Icons.Default.History),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_LAST_FOR_CONTACT, "Last Used SIM for This Contact", "Reuse the SIM from the most recent call with them", Icons.Default.History),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_LAST_IN_CALL, "Last Used SIM in Previous Call", "Reuse the SIM from the last call made from the app", Icons.Default.PhoneCallback)
)

fun simChoiceLabel(value: String): String =
    SIM_CHOICE_OPTIONS.firstOrNull { it.value == value }?.label ?: SIM_CHOICE_OPTIONS.first().label
