package com.android.libredialer

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.android.libredialer.controller.util.DefaultDialerManager
import com.android.libredialer.controller.util.DialerCodeType
import com.android.libredialer.controller.util.classifyDialerCode
import com.android.libredialer.controller.util.carrierCodeNeedsSimSelection
import com.android.libredialer.controller.util.configuredCarrierCodeAccount
import com.android.libredialer.controller.util.dialerCodeNeedsCallPhonePermission
import com.android.libredialer.controller.util.dispatchCarrierDialerCode
import com.android.libredialer.controller.util.handleLocalDialerCode
import com.android.libredialer.controller.util.PreferenceManager
import com.android.libredialer.controller.util.placeCallHonoringContactSim
import com.android.libredialer.controller.util.makeCall
import com.android.libredialer.controller.util.numbersLikelyMatch
import com.android.libredialer.controller.util.shouldRequestPhoneStateForDeviceIdentifier
import com.android.libredialer.modal.`interface`.IContactsRepository
import com.android.libredialer.view.components.SimPickerDialog
import com.android.libredialer.controller.util.enqueueApkDownload
import com.android.libredialer.controller.util.fetchLatestRelease
import com.android.libredialer.controller.util.getApkDestinationFile
import com.android.libredialer.controller.util.installApkAndScheduleDelete
import com.android.libredialer.controller.util.isNewerVersion
import com.android.libredialer.view.components.Android14WelcomeDialog
import com.android.libredialer.view.components.FullScreenIntentDialog
import com.android.libredialer.view.theme.Rivo4Theme
import com.android.libredialer.view.newui.navigation.NewUiAppShell
import com.android.libredialer.view.newui.theme.NewUiTheme
import kotlinx.coroutines.delay
import org.koin.core.context.GlobalContext

class MainActivity : FragmentActivity() {

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        try {
            GlobalContext.get().getOrNull<com.android.libredialer.controller.ContactsViewModel>()?.fetchContacts()
            GlobalContext.get().getOrNull<com.android.libredialer.controller.CallLogViewModel>()?.refreshLogs()
        } catch (_: Throwable) {}
        // When permission prompt dismisses, prompt for default dialer if not already held and welcome dialog not needed
        val prefs = GlobalContext.get().get<PreferenceManager>()
        val isFirstLaunch = !prefs.getBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, false)
        val needsWelcome = !DefaultDialerManager.isDefaultDialer(this) && (isFirstLaunch || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        if (!DefaultDialerManager.isDefaultDialer(this) && !needsWelcome) {
            requestDefaultDialer()
        }
    }

    // If a third-party direct-call shortcut hands us ACTION_CALL before CALL_PHONE happens to be
    // granted yet (e.g. very first run, right as the default-dialer role prompt from
    // requestDefaultDialer() is still pending an answer), don't silently fall back to just
    // opening the dialpad with the number filled in — that reads as "the shortcut did nothing."
    // Stash the number, ask for CALL_PHONE directly, and complete the call the moment it's
    // granted; only fall back to ACTION_DIAL if the user actually denies it.
    private var pendingExternalCallNumber: String? = null
    private var pendingExternalCallContactKey: String? = null
    private var pendingDeviceIdentifierCode: String? = null
    private var showSimPicker by mutableStateOf(false)
    private var pendingSimPickerNumber by mutableStateOf<String?>(null)
    private var pendingSimPickerDialerCode by mutableStateOf<String?>(null)

    private val requestCallPhonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val number = pendingExternalCallNumber
        val contactKey = pendingExternalCallContactKey ?: number
        pendingExternalCallNumber = null
        pendingExternalCallContactKey = null
        if (number != null) {
            if (granted) {
                placeDirectCall(number, contactKey)
            } else if (dialerCodeNeedsCallPhonePermission(number)) {
                android.widget.Toast.makeText(
                    this,
                    "Phone permission is required to run this dialer code",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.fromParts("tel", number, null))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

    private val requestDeviceIdentifierPhoneStateLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pendingDeviceIdentifierCode?.let { code ->
            pendingDeviceIdentifierCode = null
            handleLocalDialerCode(this, code)
        }
    }

    // Holds whatever intent should currently be processed by LibreUI. Set from onCreate's
    // initial intent and re-set from onNewIntent() so Compose actually reacts to intents delivered
    // to an already-running instance (contact/dial shortcuts, "call back" from other apps like
    // Truecaller, widgets, etc.) — a plain onNewIntent() { setIntent(intent) } does NOT retrigger
    // the LaunchedEffect below, since Compose has no way to observe a mutation of the Activity's
    // own `intent` field.
    private var pendingIntent by mutableStateOf<Intent?>(null)
    private var launcherResetVersion by mutableIntStateOf(0)
    private var isAppUnlocked by mutableStateOf(true)
    private var callAuthRequired by mutableStateOf(false)
    private var pendingDirectCallAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() triggers Adreno GPU driver SIGSEGV on first RenderThread draw.
        // Edge-to-edge is set via theme XML instead (windowDrawsSystemBarBackgrounds etc).
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = GlobalContext.get().get<PreferenceManager>()
        val biometricType = prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: ""
        val appLockEnabled = prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)
        isAppUnlocked = !(biometricType.isNotEmpty() && appLockEnabled)

        val hasBasicPermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        val isFirstLaunch = !prefs.getBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, false)
        val needsWelcome = !DefaultDialerManager.isDefaultDialer(this) && (isFirstLaunch || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

        if (!hasBasicPermissions) {
            requestRequiredPermissions()
        } else if (!DefaultDialerManager.isDefaultDialer(this) && !needsWelcome) {
            requestDefaultDialer()
        }

        // Auto refresh system wallpaper if enabled
        try {
            val appPrefs = org.koin.core.context.GlobalContext.get().get<PreferenceManager>()
            com.android.libredialer.controller.util.WallpaperExportHelper.refreshAutoWallpaperIfEnabled(this, appPrefs)
        } catch (_: Exception) {}

        pendingIntent = intent

        setContent {
            Rivo4Theme {
                val prefs = remember {
                    GlobalContext.get().get<PreferenceManager>()
                }

                // ── Biometric app-lock ──────────────────────────────────────
                val settingsVer by prefs.settingsChanged.collectAsState()
                val biometricType = remember(settingsVer) {
                    prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: ""
                }
                val appLockEnabled = remember(settingsVer) {
                    prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)
                }
                LaunchedEffect(settingsVer) {
                    if (biometricType.isEmpty() || !appLockEnabled) {
                        isAppUnlocked = true
                    }
                }

                val isFirstLaunch = remember {
                    !prefs.getBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, false)
                }
                val needsWelcomeDialog = remember {
                    !DefaultDialerManager.isDefaultDialer(this@MainActivity) &&
                    (isFirstLaunch || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                }

                // ── First Launch / Android 14 Welcome Dialog ─────────────────
                var showWelcomeDialog by remember { mutableStateOf(false) }
                var showFullScreenIntentDialog by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (needsWelcomeDialog) {
                        kotlinx.coroutines.delay(400)
                        showWelcomeDialog = true
                    } else if (needsFullScreenIntentPermission()) {
                        kotlinx.coroutines.delay(800)
                        showFullScreenIntentDialog = true
                    }
                }

                if (showWelcomeDialog) {
                    Android14WelcomeDialog(
                        onAppInfo = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        },
                        onContinue = {
                            prefs.setBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, true)
                            showWelcomeDialog = false
                            requestDefaultDialer()
                            if (needsFullScreenIntentPermission()) {
                                showFullScreenIntentDialog = true
                            }
                        }
                    )
                }

                // On subsequent launches, requestDefaultDialer is called in onCreate

                // ── Full-Screen Intent Permission Dialog ─────────────────────
                if (showFullScreenIntentDialog) {
                    FullScreenIntentDialog(
                        onEnable = {
                            showFullScreenIntentDialog = false
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            try {
                                startActivity(intent)
                            } catch (_: Exception) {
                                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(fallback)
                            }
                        },
                        onSkip = { showFullScreenIntentDialog = false }
                    )
                }

                var autoUpdateVersion by remember { mutableStateOf<String?>(null) }
                var autoUpdateApkUrl by remember { mutableStateOf<String?>(null) }
                var showAutoUpdateDialog by remember { mutableStateOf(false) }
                var autoDownloadId by remember { mutableStateOf<Long?>(null) }
                var autoDownloadProgress by remember { mutableFloatStateOf(0f) }
                var showAutoDownloadProgress by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val autoCheck = prefs.getBoolean(PreferenceManager.KEY_AUTO_UPDATE_CHECK, true)
                    if (autoCheck) {
                        val release = fetchLatestRelease(GITHUB_API_RELEASES)
                        if (release != null && isNewerVersion(release.tagName, APP_VERSION)) {
                            autoUpdateVersion = release.tagName
                            autoUpdateApkUrl = release.apkUrl
                            showAutoUpdateDialog = true
                        }
                    }
                }

                if (showAutoDownloadProgress) {
                    val dlId = autoDownloadId
                    if (dlId != null) {
                        LaunchedEffect(dlId) {
                            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            while (true) {
                                delay(300)
                                val query = DownloadManager.Query().setFilterById(dlId)
                                val cursor = dm.query(query)
                                if (!cursor.moveToFirst()) { cursor.close(); break }
                                val dmStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                cursor.close()
                                when (dmStatus) {
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        showAutoDownloadProgress = false
                                        autoDownloadId = null
                                        val file = getApkDestinationFile()
                                        installApkAndScheduleDelete(this@MainActivity, file)
                                        break
                                    }
                                    DownloadManager.STATUS_FAILED -> {
                                        showAutoDownloadProgress = false
                                        autoDownloadId = null
                                        break
                                    }
                                    else -> {
                                        autoDownloadProgress = if (total > 0L)
                                            (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAutoUpdateDialog) {
                    com.android.libredialer.view.components.UpdateAvailableDialog(
                        currentVersion = com.android.libredialer.APP_VERSION,
                        latestVersion = autoUpdateVersion ?: "",
                        readyToInstall = false,
                        onAction = {
                            showAutoUpdateDialog = false
                            val apkUrl = autoUpdateApkUrl
                            if (apkUrl == null) {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    "The update download is unavailable.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val downloadId = enqueueApkDownload(this@MainActivity, apkUrl)
                                if (downloadId == null) {
                                    android.widget.Toast.makeText(
                                        this@MainActivity,
                                        "Couldn't start the update download.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    autoDownloadId = downloadId
                                    autoDownloadProgress = 0f
                                    showAutoDownloadProgress = true
                                }
                            }
                        },
                        onDismiss = { showAutoUpdateDialog = false }
                    )
                }

                if (showAutoDownloadProgress) {
                    com.android.libredialer.view.components.UpdateDownloadingDialog(
                        latestVersion = autoUpdateVersion ?: "",
                        progress = autoDownloadProgress
                    )
                }

                // ── Biometric blur + lock ─────────────────────────────────
                val blurRadius by animateDpAsState(
                    targetValue = if (!isAppUnlocked || callAuthRequired) 22.dp else 0.dp,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "biometricBlur"
                )

                Box(modifier = Modifier.fillMaxSize()) {
                // Main LibreUI content, blurred when locked.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (blurRadius > 0.dp)
                                Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            else
                                Modifier
                        )
                ) {
                    NewUiTheme {
                        NewUiAppShell(
                            launcherResetVersion = launcherResetVersion,
                            onCall = { number, contactKey -> placeDirectCall(number, contactKey) },
                            onOpenRecording = { recordingUri ->
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        com.coolappstore.evercallrecorder.by.svhp.MainActivity::class.java
                                    ).putExtra(
                                        com.coolappstore.evercallrecorder.by.svhp.MainActivity.EXTRA_OPEN_RECORDING_URI,
                                        recordingUri
                                    )
                                )
                            },
                            externalIntent = pendingIntent,
                            onExternalCallIntent = { externalIntent ->
                                val target = resolveCallTargetFromIntent(this@MainActivity, externalIntent)
                                if (target != null && target.number.isNotBlank()) {
                                    placeDirectCall(target.number, target.contactId)
                                    true
                                } else {
                                    android.util.Log.w(
                                        "LibreDialerCall",
                                        "External call intent had no resolvable target: ${externalIntent.action}"
                                    )
                                    false
                                }
                            },
                            onResolveDialIntent = { externalIntent ->
                                externalIntent.data?.let { data ->
                                    resolveCallTargetFromUri(this@MainActivity, data)?.number
                                }
                            },
                            onExternalIntentHandled = { pendingIntent = null }
                        )
                    }
                } // end blurred Column

                    // ── Biometric overlay (above blur, inside Box) ─────────
                    val showAuthOverlay = callAuthRequired || !isAppUnlocked
                    if (showAuthOverlay) {
                        val activity = this@MainActivity
                        LaunchedEffect(biometricType, callAuthRequired) {
                            if (biometricType.isEmpty()) {
                                isAppUnlocked = true
                                callAuthRequired = false
                                val action = pendingDirectCallAction
                                pendingDirectCallAction = null
                                action?.invoke()
                                return@LaunchedEffect
                            }
                            if (biometricType == "system") {
                                val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
                                val prompt = androidx.biometric.BiometricPrompt(
                                    activity, executor,
                                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(r: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                            isAppUnlocked = true
                                            callAuthRequired = false
                                            val action = pendingDirectCallAction
                                            pendingDirectCallAction = null
                                            action?.invoke()
                                        }
                                        override fun onAuthenticationError(code: Int, msg: CharSequence) {
                                            pendingDirectCallAction = null
                                            callAuthRequired = false
                                            finish()
                                        }
                                        override fun onAuthenticationFailed() {}
                                    }
                                )
                                prompt.authenticate(
                                    androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("LibreDialer")
                                        .setSubtitle(if (callAuthRequired) "Verify your identity to place call" else "Verify your identity to continue")
                                        .setNegativeButtonText("Cancel")
                                        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                                        .build()
                                )
                            }
                        }
                        if (biometricType == "pin") {
                            com.android.libredialer.view.components.security.PinSetupDialog(
                                title = if (callAuthRequired) "Verify PIN to Call" else "Enter PIN",
                                isVerify = true,
                                expectedPin = prefs.getString(PreferenceManager.KEY_BIOMETRICS_PIN, "") ?: "",
                                onConfirm = {
                                    isAppUnlocked = true
                                    callAuthRequired = false
                                    val action = pendingDirectCallAction
                                    pendingDirectCallAction = null
                                    action?.invoke()
                                },
                                onDismiss = {
                                    pendingDirectCallAction = null
                                    callAuthRequired = false
                                    finish()
                                }
                            )
                        } else if (biometricType == "password") {
                            com.android.libredialer.view.components.security.PasswordSetupDialog(
                                title = if (callAuthRequired) "Verify Password to Call" else "Enter Password",
                                isVerify = true,
                                expectedPassword = prefs.getString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "") ?: "",
                                onConfirm = {
                                    isAppUnlocked = true
                                    callAuthRequired = false
                                    val action = pendingDirectCallAction
                                    pendingDirectCallAction = null
                                    action?.invoke()
                                },
                                onDismiss = {
                                    pendingDirectCallAction = null
                                    callAuthRequired = false
                                    finish()
                                }
                            )
                        }
                    }

                } // end outer Box

                if (showSimPicker && pendingSimPickerNumber != null) {
                    SimPickerDialog(
                        onDismissRequest = {
                            showSimPicker = false
                            pendingSimPickerDialerCode = null
                            pendingSimPickerNumber = null
                        },
                        onSimSelected = { handle ->
                            val dialerCode = pendingSimPickerDialerCode
                            if (dialerCode != null) {
                                dispatchCarrierDialerCode(this@MainActivity, dialerCode, handle)
                                pendingSimPickerDialerCode = null
                                pendingSimPickerNumber = null
                            } else {
                                makeCall(this@MainActivity, pendingSimPickerNumber!!, handle)
                            }
                            showSimPicker = false
                        }
                    )
                }

            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            launcherResetVersion++
        }
        pendingIntent = intent
    }

    private data class ResolvedCallTarget(
        val number: String,
        val contactId: String? = null,
        val displayName: String? = null
    )

    private fun resolveCallTargetByContactId(context: Context, contactId: String): ResolvedCallTarget? {
        val prefs = GlobalContext.get().get<PreferenceManager>()
        val defaultNumber = prefs.getContactDefaultNumber(contactId)

        return try {
            val cr = context.contentResolver
            cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
                ),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC, ${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
            )?.use { cursor ->
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                var firstNum: String? = null
                var contactName: String? = null
                while (cursor.moveToNext()) {
                    val num = if (numIdx >= 0) cursor.getString(numIdx) else null
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    if (contactName == null && !name.isNullOrBlank()) contactName = name
                    if (!num.isNullOrBlank()) {
                        if (firstNum == null) firstNum = num
                        if (defaultNumber != null && numbersLikelyMatch(num, defaultNumber)) {
                            return@use ResolvedCallTarget(number = num, contactId = contactId, displayName = contactName)
                        }
                    }
                }
                if (firstNum != null) {
                    ResolvedCallTarget(number = firstNum, contactId = contactId, displayName = contactName)
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun resolveCallTargetFromUri(context: Context, uri: Uri): ResolvedCallTarget? {
        if (uri.scheme == "tel" || uri.scheme == "sip") {
            val num = uri.schemeSpecificPart?.let { Uri.decode(it) }?.trim()
            if (!num.isNullOrBlank()) return ResolvedCallTarget(number = num)
        }

        // Try querying the URI directly first
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numColIdx = cursor.columnNames.indexOfFirst {
                        it.equals(ContactsContract.CommonDataKinds.Phone.NUMBER, ignoreCase = true) ||
                        it.equals(ContactsContract.Data.DATA1, ignoreCase = true) ||
                        it.equals("number", ignoreCase = true) ||
                        it.equals("data1", ignoreCase = true)
                    }
                    val cidColIdx = cursor.columnNames.indexOfFirst {
                        it.equals(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ignoreCase = true) ||
                        it.equals(ContactsContract.Contacts._ID, ignoreCase = true) ||
                        it.equals("contact_id", ignoreCase = true) ||
                        it.equals("_id", ignoreCase = true)
                    }
                    val nameColIdx = cursor.columnNames.indexOfFirst {
                        it.equals(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ignoreCase = true) ||
                        it.equals(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ignoreCase = true) ||
                        it.equals("display_name", ignoreCase = true)
                    }

                    val number = if (numColIdx >= 0) cursor.getString(numColIdx) else null
                    val cid = if (cidColIdx >= 0) cursor.getString(cidColIdx) else null
                    val name = if (nameColIdx >= 0) cursor.getString(nameColIdx) else null

                    if (!number.isNullOrBlank()) {
                        return ResolvedCallTarget(number = number.trim(), contactId = cid, displayName = name)
                    }

                    if (!cid.isNullOrBlank()) {
                        val byId = resolveCallTargetByContactId(context, cid)
                        if (byId != null) return byId
                    }
                }
            }
        } catch (_: Exception) {}

        // If direct query didn't return a phone number, try lookup / contact ID extraction
        try {
            val lastSegment = uri.lastPathSegment
            val id = lastSegment?.toLongOrNull()
            if (id != null) {
                // If it's a data ID
                val isData = uri.pathSegments.contains("data")
                if (isData) {
                    val dataUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id)
                    context.contentResolver.query(
                        dataUri,
                        arrayOf(ContactsContract.Data.DATA1, ContactsContract.Data.CONTACT_ID, ContactsContract.Data.DISPLAY_NAME_PRIMARY),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val num = cursor.getString(0)
                            val cid = cursor.getString(1)
                            val name = cursor.getString(2)
                            if (!num.isNullOrBlank()) {
                                return ResolvedCallTarget(number = num.trim(), contactId = cid, displayName = name)
                            }
                        }
                    }
                }

                // If it's a contact ID
                val byId = resolveCallTargetByContactId(context, id.toString())
                if (byId != null) return byId
            }

            // Try lookupContact
            val lookupUri = try { ContactsContract.Contacts.lookupContact(context.contentResolver, uri) } catch (_: Exception) { null }
            if (lookupUri != null) {
                val lookupId = lookupUri.lastPathSegment
                if (lookupId != null) {
                    val byId = resolveCallTargetByContactId(context, lookupId)
                    if (byId != null) return byId
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun resolveCallTargetFromIntent(context: Context, intent: Intent): ResolvedCallTarget? {
        val data = intent.data

        // 1. Direct extras
        val extraPhone = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            ?: intent.getStringExtra("android.intent.extra.PHONE_NUMBER")
            ?: intent.getStringExtra("phone")
            ?: intent.getStringExtra("phone_number")
            ?: intent.getStringExtra("number")
        val extraContactId = intent.getStringExtra("contact_id")

        if (!extraPhone.isNullOrBlank()) {
            return ResolvedCallTarget(extraPhone.trim(), extraContactId)
        }

        // 2. Data with tel: / sip: scheme
        if (data?.scheme == "tel" || data?.scheme == "sip") {
            val num = data.schemeSpecificPart?.let { Uri.decode(it) }?.trim()
            if (!num.isNullOrBlank()) {
                return ResolvedCallTarget(num, extraContactId)
            }
        }

        // 3. Data with content: scheme or other URI
        if (data != null) {
            val fromUri = resolveCallTargetFromUri(context, data)
            if (fromUri != null) {
                return ResolvedCallTarget(
                    number = fromUri.number,
                    contactId = extraContactId ?: fromUri.contactId,
                    displayName = fromUri.displayName
                )
            }
        }

        // 4. Fallback by contact_id extra if number wasn't provided yet
        if (!extraContactId.isNullOrBlank()) {
            val byId = resolveCallTargetByContactId(context, extraContactId)
            if (byId != null) return byId
        }

        return null
    }

    private fun placeDirectCall(targetNumber: String, contactKey: String? = null) {
        val cleanNumber = targetNumber.trim()
        if (cleanNumber.isBlank()) return
        when (classifyDialerCode(cleanNumber)) {
            DialerCodeType.LOCAL, DialerCodeType.SECRET -> {
                dispatchLocalDialerCode(cleanNumber)
                return
            }
            DialerCodeType.USSD, DialerCodeType.MMI -> {
                placeCarrierDialerCode(cleanNumber)
                return
            }
            DialerCodeType.NONE -> Unit
        }

        val prefs = GlobalContext.get().get<PreferenceManager>()
        val biometricType = prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: ""
        val isCallGated = biometricType.isNotEmpty() && prefs.shouldGateCallWithBiometric(cleanNumber)
        val appLockEnabled = prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)
        val isAppGated = biometricType.isNotEmpty() && appLockEnabled && !isAppUnlocked

        if (isCallGated || isAppGated) {
            pendingDirectCallAction = {
                executeDirectCall(cleanNumber, contactKey)
            }
            if (isCallGated) {
                callAuthRequired = true
            }
            return
        }

        executeDirectCall(cleanNumber, contactKey)
    }

    private fun dispatchLocalDialerCode(code: String) {
        val hasPhoneStatePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED
        if (shouldRequestPhoneStateForDeviceIdentifier(code, Build.VERSION.SDK_INT, hasPhoneStatePermission)) {
            pendingDeviceIdentifierCode = code
            requestDeviceIdentifierPhoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            return
        }
        handleLocalDialerCode(this, code)
    }

    private fun placeCarrierDialerCode(
        code: String,
        selectedAccount: android.telecom.PhoneAccountHandle? = null
    ) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pendingExternalCallNumber = code
            pendingExternalCallContactKey = null
            requestCallPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }

        val prefs = GlobalContext.get().get<PreferenceManager>()
        if (selectedAccount == null && carrierCodeNeedsSimSelection(this, prefs)) {
            pendingSimPickerDialerCode = code
            pendingSimPickerNumber = code
            showSimPicker = true
            return
        }
        val account = selectedAccount ?: configuredCarrierCodeAccount(this, prefs)
        dispatchCarrierDialerCode(this, code, account)
    }

    private fun executeDirectCall(cleanNumber: String, contactKey: String?) {
        val prefs = GlobalContext.get().get<PreferenceManager>()
        // If contactKey wasn't supplied, try finding the matching contact by phone number
        val resolvedKey = contactKey?.takeIf { it.isNotBlank() } ?: run {
            try {
                val contactsRepo = GlobalContext.get().get<IContactsRepository>()
                contactsRepo.getContactByNumber(cleanNumber)?.id
            } catch (_: Exception) { null }
        } ?: cleanNumber

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            placeCallHonoringContactSim(this, prefs, resolvedKey, cleanNumber) {
                pendingSimPickerNumber = cleanNumber
                showSimPicker = true
            }
        } else {
            pendingExternalCallNumber = cleanNumber
            pendingExternalCallContactKey = resolvedKey
            requestCallPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    fun requestDefaultDialer() {
        if (!DefaultDialerManager.isDefaultDialer(this)) {
            DefaultDialerManager.requestDefaultDialer(requestRoleLauncher, this)
        }
    }

    fun needsFullScreenIntentPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val nm = getSystemService(NotificationManager::class.java)
        return !nm.canUseFullScreenIntent()
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            com.android.libredialer.controller.util.MissedCallBadgeManager.markMissedCallsAsRead(this)
        }
    }
}
