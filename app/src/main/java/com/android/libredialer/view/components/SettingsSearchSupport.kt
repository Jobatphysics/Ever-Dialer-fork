package com.android.libredialer.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.NetworkCell
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.libredialer.APP_VERSION
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Favorite
import com.android.libredialer.view.newui.navigation.NewUiSettingsDestination
import com.android.libredialer.view.newui.navigation.NewUiSettingsNavigation
import com.android.libredialer.view.newui.navigation.NewUiNavigationRequests
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.controller.util.SearchHistoryManager

/**
 * Applied to a settings row so that tapping a search result can reveal *where* that setting
 * lives (scrolling it into view and flashing it) instead of silently firing its action.
 *
 * Shared across LibreUI Settings and every settings sub-screen so a search result that points
 * into a nested screen (e.g. "Auto Redial" inside Call Settings) can be scrolled to and
 * highlighted there too, after navigating with a `highlightKey` nav arg.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.settingsSearchHighlight(
    key: String,
    highlightedKey: String?,
    onConsumed: () -> Unit
): Modifier {
    val requester = remember(key) { BringIntoViewRequester() }
    val isHighlighted = highlightedKey == key
    val flash = remember(key) { Animatable(0f) }
    val highlightColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            // The target row may not have completed its first layout pass yet — this fires
            // right when we switch from search results back to the full list, or right as a
            // freshly-navigated screen composes. Wait a few frames and retry so bringIntoView()
            // always has valid layout coordinates to scroll to.
            var attempt = 0
            var succeeded = false
            while (!succeeded && attempt < 6) {
                try {
                    requester.bringIntoView()
                    succeeded = true
                } catch (_: Exception) {
                    // Not laid out yet — wait and retry.
                    delay(if (attempt == 0) 120L else 60L)
                }
                attempt++
            }
            flash.snapTo(1f)
            flash.animateTo(0f, animationSpec = tween(1200))
            onConsumed()
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .drawWithContent {
            drawContent()
            if (flash.value > 0f) {
                drawRoundRect(
                    color = highlightColor.copy(alpha = 0.30f * flash.value),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
        }
}

/**
 * A search result for the cross-page settings search box (see [SettingsSearchEntryPoint]).
 * Unlike the local search on the main Settings screen (which can just flash a row in place),
 * every entry here always navigates somewhere — either into the sub-screen that owns the
 * setting, or back to the main Settings screen with `highlightKey` set, so it works no matter
 * which settings page the search was performed from.
 */
data class GlobalSettingsSearchEntry(
    val title: String,
    val subtitle: String,
    val key: String,
    val icon: ImageVector,
    val iconContainerColor: Color,
    val titleLower: String = title.lowercase(),
    val subtitleLower: String = subtitle.lowercase(),
    val navigateTo: () -> Unit
)

typealias SettingsSearchEntry = GlobalSettingsSearchEntry

private val GsColorPurple  = Color(0xFF9C27B0)
private val GsColorBlue    = Color(0xFF2196F3)
private val GsColorGreen   = Color(0xFF4CAF50)
private val GsColorTeal    = Color(0xFF009688)
private val GsColorIndigo  = Color(0xFF3F51B5)
private val GsColorBluGrey = Color(0xFF607D8B)
private val GsColorAmber   = Color(0xFFFFC107)
private val GsColorCyan    = Color(0xFF00BCD4)
private val GsColorRed     = Color(0xFFE53935)
private val GsColorPink    = Color(0xFFE91E63)

val globalSettingsSearchEntries: List<GlobalSettingsSearchEntry> by lazy {
    listOf(
        // ── Rows that live directly on the main Settings screen ─────────────────
        GlobalSettingsSearchEntry("Themes", "Choose LibreDialer's appearance", "interface", Icons.Outlined.Palette, GsColorPurple) {
            NewUiNavigationRequests.openSettings(NewUiSettingsDestination.ColorsAndTheme, "theme_mode")
        },
        GlobalSettingsSearchEntry("Liquid Glass Tint", "Adjust the intensity of the neutral grey tint applied to Liquid Glass surfaces", "interface", Icons.Outlined.Palette, GsColorPurple) {
            NewUiNavigationRequests.openSettings(NewUiSettingsDestination.ColorsAndTheme, "liquid_glass_tint")
        },
        GlobalSettingsSearchEntry("Authentication", "App lock, biometrics, and PIN/password", "authentication", Icons.Default.Fingerprint, Color(0xFF6750A4)) {
            NewUiNavigationRequests.openSettings(NewUiSettingsDestination.BiometricAndAppLock, "auth_method")
        },
        GlobalSettingsSearchEntry("Call Behavior & Integrations", "Call features, network switcher, and recording notes", "app_settings", Icons.Outlined.Tune, GsColorTeal) {
            NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior)
        },
        GlobalSettingsSearchEntry("Contacts Hider", "Hide selected contacts behind an access code", "contacts_hider", Icons.Outlined.Lock, Color(0xFF5E35B1)) {
            NewUiNavigationRequests.openSettings(NewUiSettingsDestination.ContactsHider)
        },
        GlobalSettingsSearchEntry("Call Recording", "Configure call recording", "call_recording", Icons.Default.FiberManualRecord, Color(0xFFE53935)) {
            NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "call_recording")
        },
        GlobalSettingsSearchEntry("Backup & Restore", "Create, share, or restore an app backup", "backup_restore", Icons.Default.Backup, GsColorBlue) {
            NewUiNavigationRequests.openSettings(NewUiSettingsDestination.BackupRestore, "backup_restore")
        },
        GlobalSettingsSearchEntry("Fake Calls", "Schedule test incoming calls", "fake_calls", Icons.Outlined.Call, GsColorTeal) {
            NewUiNavigationRequests.openSettings(NewUiSettingsDestination.FakeCalls, "fake_calls")
        },

        // ── App Settings screen ──────────────────────────────────────────────────
        GlobalSettingsSearchEntry("4G/5G Switcher", "Quickly switch network mode per app", "network_switcher", Icons.Outlined.NetworkCell, GsColorBlue) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.NetworkSwitcher, "network_switcher") },
        GlobalSettingsSearchEntry("Integrate Notes Section", "Show notes alongside call recordings", "integrate_notes", Icons.Outlined.Notes, GsColorGreen) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "integrate_notes") },
        GlobalSettingsSearchEntry("Delete Notes With Recording", "Remove the note when its recording is deleted", "delete_notes_with_recording", Icons.Outlined.NoteAlt, GsColorRed) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "delete_notes_with_recording") },

        // ── Sim And Call Placement screen ──────────────────────────────────────
        GlobalSettingsSearchEntry("Sim And Call Placement", "Default SIM, SIM colors, confirm calls, contacts", "sim_and_call_placement", Icons.Outlined.SimCard, GsColorGreen) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SimAndCallPlacement, "sim_and_call_placement") },
        GlobalSettingsSearchEntry("Sound & Vibration", "Ringtones and dialpad tones", "sound_vibration_link", Icons.Outlined.VolumeUp, GsColorBlue) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SoundAndVibration, "sound_vibration_link") },
        GlobalSettingsSearchEntry("Default SIM", "Which SIM is used to place calls", "default_sim", Icons.Outlined.SimCard, GsColorGreen) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SimAndCallPlacement, "default_sim") },
        GlobalSettingsSearchEntry("Confirm placing a call", "Ask for confirmation before placing any outgoing call", "confirm_placing_call", Icons.Outlined.CheckCircle, Color(0xFF3F51B5)) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SimAndCallPlacement, "confirm_placing_call") },
        GlobalSettingsSearchEntry("Show SIM buttons", "Show SIM 1 and SIM 2 buttons instead of dial button in dialpad", "show_sim_buttons_in_dialpad", Icons.Outlined.Dialpad, GsColorTeal) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SimAndCallPlacement, "show_sim_buttons_in_dialpad") },
        GlobalSettingsSearchEntry("Use SIM based on call logs history on any call", "Automatically select the same SIM from call log history", "use_sim_from_call_log", Icons.Outlined.History, Color(0xFF3F51B5)) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SimAndCallPlacement, "use_sim_from_call_log") },
        GlobalSettingsSearchEntry("Customize SIM Colors", "Choose custom colors for SIM 1 and SIM 2", "customize_sim_colors", Icons.Outlined.Palette, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SimAndCallPlacement, "customize_sim_colors") },
        GlobalSettingsSearchEntry("Contacts to display", "Choose which accounts' contacts are shown", "contacts_to_display", Icons.Outlined.Contacts, GsColorBlue) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SimAndCallPlacement, "contacts_to_display") },
        GlobalSettingsSearchEntry("Missed Call Notification", "Show missed call notifications through LibreDialer", "missed_call_notification", Icons.AutoMirrored.Filled.CallMissed, GsColorRed) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SimAndCallPlacement, "missed_call_notification") },

        // ── Call Features & Behavior ─────────────────────────────────────────────
        GlobalSettingsSearchEntry("Device Orientation with Proximity Sensor", "Combine orientation and proximity to prevent false screen-offs during a call", "proximity_orientation_bg", Icons.Outlined.ScreenLockPortrait, GsColorPink) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "proximity_orientation_bg") },
        GlobalSettingsSearchEntry("Proximity Sensor on in background", "Turn off screen when phone is near ear during a call", "proximity_sensor_bg", Icons.Outlined.Sensors, GsColorTeal) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "proximity_sensor_bg") },
        GlobalSettingsSearchEntry("Pocket Mode Prevention", "Block accidental answer/decline when phone is in pocket", "pocket_mode_prevention", Icons.Outlined.Sensors, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "pocket_mode_prevention") },
        GlobalSettingsSearchEntry("Floating Ongoing Call", "Draggable floating bubble during calls", "floating_ongoing_call", Icons.Outlined.Sensors, GsColorBlue) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "floating_ongoing_call") },
        GlobalSettingsSearchEntry("Direct Call on Tap", "Tap a call log entry to call directly", "direct_call_on_tap", Icons.Outlined.Call, GsColorGreen) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "direct_call_on_tap") },
        GlobalSettingsSearchEntry("Auto Speaker", "Switch to loudspeaker when phone is away from ear", "auto_speaker", Icons.Outlined.VolumeUp, GsColorRed) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "auto_speaker") },
        GlobalSettingsSearchEntry("Rain Mode", "Answer/decline calls by shaking your device", "rain_mode_link", Icons.Outlined.WaterDrop, Color(0xFF0288D1)) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RainMode) },
        GlobalSettingsSearchEntry("Auto Redial", "Automatically redial on rejected/unanswered/busy calls", "auto_redial", Icons.Default.Replay, GsColorBlue) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "auto_redial") },
        GlobalSettingsSearchEntry("Missed Call Popup", "Show interactive popup over other apps on missed calls", "missed_call_popup", Icons.AutoMirrored.Filled.CallMissed, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "missed_call_popup") },
        GlobalSettingsSearchEntry("Always Show Popup After Every Call Ends", "Show popup after every call ends with only custom response", "always_show_after_call_ends", Icons.AutoMirrored.Filled.CallMissed, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.AppAndCallBehavior, "missed_call_popup") },
        GlobalSettingsSearchEntry("Volume DND", "Toggle Do Not Disturb using volume button combination", "volume_dnd", Icons.Outlined.VolumeUp, GsColorPurple) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.VolumeDnd, "volume_dnd") },

        // ── Rain Mode screen ─────────────────────────────────────────────────────
        GlobalSettingsSearchEntry("Enable Rain Mode", "Answer or decline calls by shaking device", "enable_rain_mode", Icons.Outlined.WaterDrop, Color(0xFF0288D1)) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RainMode, "enable_rain_mode") },
        GlobalSettingsSearchEntry("Rain Mode Shake Sensitivity", "Adjust shake intensity threshold for Rain Mode", "rain_mode_intensity", Icons.Outlined.Tune, Color(0xFF0288D1)) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RainMode, "rain_mode_intensity") },
        GlobalSettingsSearchEntry("Rain Mode Vibration Feedback", "Vibrate when call is answered or declined via shake gesture", "rain_mode_vibrate", Icons.Outlined.Vibration, GsColorPurple) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RainMode, "rain_mode_vibrate") },

        // ── Raise to Answer screen ───────────────────────────────────────────────
        GlobalSettingsSearchEntry("Enable Raise to Answer", "Answer calls by raising the phone to your ear", "enable_raise_to_answer", Icons.Outlined.Vibration, GsColorTeal) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RaiseToAnswer, "enable_raise_to_answer") },
        GlobalSettingsSearchEntry("Answer at Any Angle", "Raise to Answer sensitivity", "answer_any_angle", Icons.Outlined.Vibration, GsColorTeal) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RaiseToAnswer, "answer_any_angle") },
        GlobalSettingsSearchEntry("Decline by Flipping", "Flip the phone face down to decline a call", "decline_by_flipping", Icons.Outlined.Vibration, GsColorRed) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RaiseToAnswer, "decline_by_flipping") },
        GlobalSettingsSearchEntry("Raise to Answer Beep Feedback", "Play a beep when raise/flip is detected", "raise_beep_feedback", Icons.Outlined.Vibration, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RaiseToAnswer, "raise_beep_feedback") },
        GlobalSettingsSearchEntry("Raise to Answer Vibrate Feedback", "Vibrate when raise/flip is detected", "raise_vibrate_feedback", Icons.Outlined.Vibration, GsColorPurple) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.RaiseToAnswer, "raise_vibrate_feedback") },

        // ── Call Recording settings ──────────────────────────────────────────────
        GlobalSettingsSearchEntry("Call Recording Master Switch", "Enable or disable all background call recording", "call_recording_master", Icons.Default.FiberManualRecord, Color(0xFFE53935)) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "call_recording_master") },
        GlobalSettingsSearchEntry("Auto Record Incoming Calls", "Automatically record incoming calls from all or specific contacts", "auto_record_incoming", Icons.Outlined.CallReceived, GsColorGreen) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "auto_record_incoming") },
        GlobalSettingsSearchEntry("Auto Record Outgoing Calls", "Automatically record outgoing calls to all or specific contacts", "auto_record_outgoing", Icons.Outlined.CallMade, GsColorBlue) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "auto_record_outgoing") },
        GlobalSettingsSearchEntry("Recording Storage Location", "Choose custom folder or app-private storage for recordings", "recording_storage", Icons.Outlined.Folder, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "recording_storage") },
        GlobalSettingsSearchEntry("Recording Audio Source", "Microphone, media projection, or internal call stream", "recording_audio_source", Icons.Outlined.Mic, GsColorTeal) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "recording_audio_source") },
        GlobalSettingsSearchEntry("Recording Audio Codec", "Audio recording format (AAC, Opus, etc.)", "recording_audio_codec", Icons.Outlined.GraphicEq, GsColorPurple) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "recording_audio_codec") },
        GlobalSettingsSearchEntry("Recording Sample Rate", "Audio quality sample rate (e.g. 48kHz, 44.1kHz)", "recording_sample_rate", Icons.Outlined.Equalizer, GsColorIndigo) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "recording_sample_rate") },
        GlobalSettingsSearchEntry("Auto Delete Old Recordings", "Clean up call recordings older than 7/30/90 days", "auto_delete_recordings", Icons.Outlined.DeleteSweep, GsColorRed) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "auto_delete_recordings") },
        GlobalSettingsSearchEntry("Recording Notifications", "Show persistent notification while recording calls", "recording_notifications", Icons.Outlined.Notifications, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "recording_notifications") },
        GlobalSettingsSearchEntry("Post-Recording File Actions", "Quick play, share, or delete notification after call ends", "post_recording_actions", Icons.Outlined.DoneAll, GsColorGreen) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "post_recording_actions") },
        GlobalSettingsSearchEntry("Call Recording App Lock", "Require PIN or biometric authentication for recordings", "recording_app_lock", Icons.Default.Fingerprint, Color(0xFF6750A4)) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "recording_app_lock") },
        GlobalSettingsSearchEntry("Separate Audio Channels", "Record caller and receiver on left and right channels", "separate_channels", Icons.Outlined.Headphones, GsColorCyan) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.CallRecording, "separate_channels") },

        // ── 4G/5G Network Switch Settings ─────────────────────────────────────────
        GlobalSettingsSearchEntry("4G/5G Network Switcher", "Force LTE/NR network mode or per-app automation", "network_switcher_app", Icons.Outlined.NetworkCell, GsColorBlue) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.NetworkSwitcher, "network_switcher") },
        GlobalSettingsSearchEntry("Per-App Network Mode Automation", "Automatically switch network mode per application", "network_automation", Icons.Outlined.AutoMode, GsColorIndigo) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.NetworkSwitcher, "network_switcher") },
        GlobalSettingsSearchEntry("Preferred Network Mode", "Select 5G NR, 4G LTE, 3G, or 2G network modes", "preferred_network_mode", Icons.Outlined.SignalCellularAlt, GsColorTeal) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.NetworkSwitcher, "network_switcher") },
        GlobalSettingsSearchEntry("Network Switcher Shizuku Mode", "Switch network modes without root using Shizuku", "network_shizuku", Icons.Outlined.Security, GsColorGreen) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.NetworkSwitcher, "network_switcher") },
        GlobalSettingsSearchEntry("Network Switcher Root Mode", "Direct shell network mode execution with root access", "network_root", Icons.Outlined.AdminPanelSettings, GsColorRed) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.NetworkSwitcher, "network_switcher") },
        GlobalSettingsSearchEntry("Network Switch Quick Settings Tile", "Toggle 4G/5G directly from Android notification shade", "network_tile", Icons.Outlined.ViewStream, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.NetworkSwitcher, "network_switcher") },
        GlobalSettingsSearchEntry("Network Switch Floating Hint", "Show on-screen network mode floating indicator", "network_floating_hint", Icons.Outlined.PictureInPicture, GsColorCyan) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.NetworkSwitcher, "network_switcher") },

        // ── Sound & Vibration screen ─────────────────────────────────────────────
        GlobalSettingsSearchEntry("DTMF Tone", "Play tones when dialing digits", "dtmf_tone", Icons.Outlined.VolumeUp, GsColorBlue) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SoundAndVibration, "dtmf_tone") },
        GlobalSettingsSearchEntry("Dial Pad Tone", "Choose the dialpad key tone", "dialpad_tone", Icons.Outlined.VolumeUp, GsColorTeal) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SoundAndVibration, "dialpad_tone") },
        GlobalSettingsSearchEntry("Ringtone Settings", "Choose your incoming call ringtone", "ringtone_settings", Icons.Outlined.VolumeUp, GsColorAmber) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SoundAndVibration, "ringtone_settings") },
        GlobalSettingsSearchEntry("Do Not Disturb", "Manage Do Not Disturb access", "dnd_settings", Icons.Outlined.VolumeUp, GsColorIndigo) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.SoundAndVibration, "dnd_settings") },

        // ── Authentication (Biometric) screen ────────────────────────────────────
        GlobalSettingsSearchEntry("Authentication Method", "System biometrics, PIN, or password", "auth_method", Icons.Default.Fingerprint, Color(0xFF6750A4)) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.BiometricAndAppLock, "auth_method") },
        GlobalSettingsSearchEntry("Lock App on Open", "Require authentication whenever the app opens", "lock_app_open", Icons.Default.Fingerprint, GsColorRed) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.BiometricAndAppLock, "lock_app_open") },
        GlobalSettingsSearchEntry("Lock Call Actions", "Require authentication for sensitive call actions", "lock_call_actions", Icons.Default.Fingerprint, GsColorTeal) { NewUiNavigationRequests.openSettings(NewUiSettingsDestination.BiometricAndAppLock, "lock_call_actions") },

    )
}

/** The full, flat list of every setting reachable from Settings, used by [SettingsSearchEntryPoint]
 *  so the same search box can be dropped onto any settings page. */
fun buildGlobalSettingsSearchEntries(): List<GlobalSettingsSearchEntry> = globalSettingsSearchEntries

@Composable
fun SettingsSearchHeaderAction(onNavigateSettings: NewUiSettingsNavigation) {
    var showSearch by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { showSearch = true }) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search settings",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }

    if (showSearch) {
        Dialog(
            onDismissRequest = { showSearch = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            SettingsSearchFloatingPopup(
                onNavigateSettings = onNavigateSettings,
                onClose = { showSearch = false }
            )
        }
    }
}

@Composable
private fun SettingsSearchFloatingPopup(
    onNavigateSettings: NewUiSettingsNavigation,
    onClose: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val prefs: PreferenceManager = koinInject()
    val settingsState by prefs.settingsChanged.collectAsState()
    val history = remember(settingsState) {
        SearchHistoryManager.getHistory(prefs, SearchHistoryManager.Type.SETTINGS)
    }
    val results = remember(query) {
        val normalized = query.trim()
        if (normalized.isBlank()) emptyList()
        else globalSettingsSearchEntries.filter { entry ->
            com.android.libredialer.controller.util.matchesFuzzySearch(entry.title, normalized) ||
                com.android.libredialer.controller.util.matchesFuzzySearch(entry.subtitle, normalized)
        }
    }

    fun saveQuery() {
        if (query.isNotBlank()) {
            SearchHistoryManager.addHistory(prefs, SearchHistoryManager.Type.SETTINGS, query)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val searchCenterY = maxHeight * 0.25f
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .offset(y = searchCenterY - 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(34.dp))
                }
            }
            SearchPillInput(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp)
                    .height(56.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                placeholder = "Search",
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                placeholderTextAlign = TextAlign.Center,
                placeholderTextOffsetX = (-28).dp,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        saveQuery()
                        keyboardController?.hide()
                    }
                )
            )
            SearchClearButton(
                onClick = onClose,
                contentDescription = if (query.isNotEmpty()) "Clear search" else "Close search",
                buttonSize = 56.dp,
                iconSize = 34.dp
            )
        }

        if (query.isNotBlank()) {
            val resultsTop = searchCenterY + 36.dp
            val resultsMaxHeight = (maxHeight - resultsTop - 24.dp).coerceAtLeast(96.dp)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .offset(y = resultsTop)
                    .heightIn(max = resultsMaxHeight),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp
            ) {
                if (results.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No settings found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = resultsMaxHeight),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = results,
                            key = { index, entry -> "floating_settings_${entry.key}_$index" }
                        ) { _, entry ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                RivoListItem(
                                    headline = entry.title,
                                    supporting = entry.subtitle,
                                    leadingIcon = entry.icon,
                                    iconContainerColor = entry.iconContainerColor,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = {
                                        saveQuery()
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        onClose()
                                        entry.navigateTo()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else if (isFocused && history.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .offset(y = searchCenterY + 36.dp)
            ) {
                SearchHistorySection(
                    history = history,
                    onItemClick = { item ->
                        query = item
                        saveQuery()
                    },
                    onRemoveItem = {
                        SearchHistoryManager.removeHistoryItem(
                            prefs,
                            SearchHistoryManager.Type.SETTINGS,
                            it
                        )
                    },
                    onClearAll = {
                        SearchHistoryManager.clearHistory(
                            prefs,
                            SearchHistoryManager.Type.SETTINGS
                        )
                    }
                )
            }
        }
    }
}

internal fun settingsDestinationForKey(key: String): NewUiSettingsDestination = when (key) {
    "interface" -> NewUiSettingsDestination.ColorsAndTheme
    "authentication", "auth_method", "lock_app_open", "lock_call_actions" ->
        NewUiSettingsDestination.BiometricAndAppLock
    "app_settings" -> NewUiSettingsDestination.AppAndCallBehavior
    "contacts_hider" -> NewUiSettingsDestination.ContactsHider
    "call_recording", "call_recording_master", "auto_record_incoming",
    "auto_record_outgoing", "recording_storage", "recording_audio_source",
    "recording_audio_codec", "recording_sample_rate", "auto_delete_recordings",
    "recording_notifications", "post_recording_actions", "recording_app_lock",
    "separate_channels" -> NewUiSettingsDestination.CallRecording
    "sim_and_call_placement", "default_sim", "confirm_placing_call",
    "show_sim_buttons_in_dialpad", "use_sim_from_call_log",
    "customize_sim_colors", "contacts_to_display", "missed_call_notification" ->
        NewUiSettingsDestination.SimAndCallPlacement
    "sound_vibration_link", "dtmf_tone", "dialpad_tone", "ringtone_settings",
    "dnd_settings" -> NewUiSettingsDestination.SoundAndVibration
    "rain_mode_link", "enable_rain_mode", "rain_mode_intensity",
    "rain_mode_vibrate" -> NewUiSettingsDestination.RainMode
    "enable_raise_to_answer", "answer_any_angle", "decline_by_flipping",
    "raise_beep_feedback", "raise_vibrate_feedback" ->
        NewUiSettingsDestination.RaiseToAnswer
    "volume_dnd" -> NewUiSettingsDestination.VolumeDnd
    "network_switcher", "network_switcher_app", "network_automation",
    "preferred_network_mode", "network_shizuku", "network_root",
    "network_tile", "network_floating_hint" -> NewUiSettingsDestination.NetworkSwitcher
    else -> error("No LibreUI destination for settings search key: $key")
}

/**
 * A "Search settings" box that can be dropped as the first item on any settings page (main
 * Settings screen included, in principle, though it has its own richer local search). Typing
 * here searches every setting across every settings page; tapping a result navigates straight
 * to it (or back to the main Settings screen with that row highlighted, if it lives there).
 */
@Composable
fun SettingsSearchEntryPoint(
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    val entries = globalSettingsSearchEntries
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else entries.filter {
            com.android.libredialer.controller.util.matchesFuzzySearch(it.title, q) ||
                    com.android.libredialer.controller.util.matchesFuzzySearch(it.subtitle, q)
        }.take(8)
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isFocused by remember { mutableStateOf(false) }
    val prefs: PreferenceManager = koinInject()
    val settingsState by prefs.settingsChanged.collectAsState()
    val settingsSearchHistory = remember(settingsState) {
        SearchHistoryManager.getHistory(prefs, SearchHistoryManager.Type.SETTINGS)
    }
    fun saveQuery(q: String = query) {
        if (q.isNotBlank()) {
            SearchHistoryManager.addHistory(prefs, SearchHistoryManager.Type.SETTINGS, q)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchPillContainer(Modifier.weight(1f)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    placeholder = {
                        Text(
                            "Search settings",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            saveQuery()
                            keyboardController?.hide()
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    maxLines = 1
                )
            }
            SearchClearButton(
                onClick = {
                    if (query.isNotEmpty()) query = "" else onClose?.invoke()
                },
                contentDescription = if (query.isNotEmpty()) "Clear search" else "Close search"
            )
        }

        AnimatedVisibility(
            visible = isFocused && query.isBlank() && settingsSearchHistory.isNotEmpty(),
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)) +
                    androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(300)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) +
                    androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(250))
        ) {
            Column {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                SearchHistorySection(
                    history = settingsSearchHistory,
                    onItemClick = { item ->
                        query = item
                        saveQuery(item)
                    },
                    onRemoveItem = { item ->
                        com.android.libredialer.controller.util.SearchHistoryManager.removeHistoryItem(
                            prefs,
                            com.android.libredialer.controller.util.SearchHistoryManager.Type.SETTINGS,
                            item
                        )
                    },
                    onClearAll = {
                        com.android.libredialer.controller.util.SearchHistoryManager.clearHistory(
                            prefs,
                            com.android.libredialer.controller.util.SearchHistoryManager.Type.SETTINGS
                        )
                    }
                )
            }
        }

        if (query.isNotBlank()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No settings found for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                RivoExpressiveCard {
                    filtered.forEachIndexed { index, entry ->
                        RivoListItem(
                            headline = entry.title,
                            supporting = entry.subtitle,
                            leadingIcon = entry.icon,
                            iconContainerColor = entry.iconContainerColor,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = {
                                saveQuery()
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                entry.navigateTo()
                            }
                        )
                        if (index < filtered.size - 1) CardDivider()
                    }
                }
            }
        }
    }
}
