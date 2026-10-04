package com.android.libredialer.controller.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.net.URI

enum class DialerCodeType {
    LOCAL,
    SECRET,
    USSD,
    MMI,
    NONE
}

internal enum class DialerCodeDispatchPath {
    DEVICE_IDENTIFIER_DISPLAY,
    LOCAL_SETTINGS,
    ANDROID_SECRET_CODE,
    TELEPHONY_USSD,
    TELECOM_MMI,
    ORDINARY_CALL
}

internal enum class DeviceIdentifierFailure {
    PHONE_STATE_PERMISSION_REQUIRED,
    ACCESS_RESTRICTED,
    NOT_AVAILABLE
}

internal enum class DeviceInfoSettingsLaunchFailure {
    ACTIVITY_NOT_FOUND,
    NOT_ALLOWED
}

internal fun buildTelUriString(number: String): String =
    URI("tel", number, null).toASCIIString()

internal fun classifyDialerCode(input: String): DialerCodeType {
    val code = input.trim()
    if (code == "*#06#" || code == "*#07#") return DialerCodeType.LOCAL
    if (Regex("^\\*#\\*#\\d+#\\*#\\*$").matches(code)) return DialerCodeType.SECRET
    if (!Regex("^[*#][*#\\d+*]+#$").matches(code)) return DialerCodeType.NONE

    val mmiPrefix = code.startsWith("*#") || code.startsWith("#*") ||
        code.startsWith("##") || code.startsWith("**")
    val serviceCode = Regex("^[*#](\\d{2,3})(?:\\*[^#]*)?#$").matchEntire(code)
        ?.groupValues?.get(1)
    val supplementaryServiceCodes = setOf(
        "21", "30", "31", "33", "35", "43", "61", "62", "67", "76", "77",
        "002", "004", "330", "331", "332", "333", "351", "353"
    )
    return if (mmiPrefix || serviceCode in supplementaryServiceCodes) {
        DialerCodeType.MMI
    } else {
        DialerCodeType.USSD
    }
}

internal fun selectDialerCodeDispatchPath(input: String): DialerCodeDispatchPath =
    when (classifyDialerCode(input)) {
        DialerCodeType.LOCAL ->
            if (input.trim() == "*#06#") DialerCodeDispatchPath.DEVICE_IDENTIFIER_DISPLAY
            else DialerCodeDispatchPath.LOCAL_SETTINGS
        DialerCodeType.SECRET -> DialerCodeDispatchPath.ANDROID_SECRET_CODE
        DialerCodeType.USSD -> DialerCodeDispatchPath.TELEPHONY_USSD
        DialerCodeType.MMI -> DialerCodeDispatchPath.TELECOM_MMI
        DialerCodeType.NONE -> DialerCodeDispatchPath.ORDINARY_CALL
    }

internal fun dialerCodeNeedsCallPhonePermission(input: String): Boolean =
    when (selectDialerCodeDispatchPath(input)) {
        DialerCodeDispatchPath.TELEPHONY_USSD,
        DialerCodeDispatchPath.TELECOM_MMI -> true
        else -> false
    }

internal fun shouldRequestPhoneStateForDeviceIdentifier(
    input: String,
    sdkInt: Int,
    permissionGranted: Boolean
): Boolean =
    input.trim() == "*#06#" && sdkInt < Build.VERSION_CODES.Q && !permissionGranted

internal fun deviceIdentifierFallbackMessage(failure: DeviceIdentifierFailure): String =
    when (failure) {
        DeviceIdentifierFailure.PHONE_STATE_PERMISSION_REQUIRED ->
            "Phone permission is needed to read the device identifier. Check Device or SIM information in Settings; the IMEI may not be shown."
        DeviceIdentifierFailure.ACCESS_RESTRICTED ->
            "Android restricts IMEI access for this app. Check Device or SIM information in Settings; the IMEI may not be shown."
        DeviceIdentifierFailure.NOT_AVAILABLE ->
            "The IMEI is not available to this app. Check Device or SIM information in Settings; the IMEI may not be shown."
    }

internal fun deviceInfoSettingsLaunchFailureMessage(
    failure: DeviceInfoSettingsLaunchFailure
): String =
    when (failure) {
        DeviceInfoSettingsLaunchFailure.ACTIVITY_NOT_FOUND ->
            "Device information Settings are unavailable on this device"
        DeviceInfoSettingsLaunchFailure.NOT_ALLOWED ->
            "Android did not allow Device Information Settings to open"
    }

/**
 * Handles local Android codes through their local device-information or settings path.
 */
fun handleLocalDialerCode(context: Context, input: String): Boolean {
    val code = input.trim()
    when (selectDialerCodeDispatchPath(code)) {
        DialerCodeDispatchPath.DEVICE_IDENTIFIER_DISPLAY ->
            return showDeviceIdentifier(context)
        DialerCodeDispatchPath.LOCAL_SETTINGS -> {
            val actions = listOf(
                "android.settings.SAR_INFORMATION",
                "android.settings.RF_EXPOSURE_SETTINGS",
                android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS
            )
            for (action in actions) {
                if (startSettings(context, action)) return true
            }
            return true
        }
        DialerCodeDispatchPath.ANDROID_SECRET_CODE -> Unit
        else -> return false
    }

    val match = Regex("^\\*#\\*#(\\d+)#\\*#\\*$").matchEntire(code) ?: return false
    val secretCode = match.groupValues[1]
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
                ?.sendDialerSpecialCode(secretCode)
        } catch (exception: Exception) {
            Log.w("DialerCode", "Unable to send dialer special code", exception)
        }
    }

    val uri = android.net.Uri.parse("android_secret_code://$secretCode")
    listOf("android.provider.Telephony.SECRET_CODE", "android.telephony.action.SECRET_CODE")
        .forEach { action ->
            try {
                context.sendBroadcast(
                    Intent(action, uri).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                )
            } catch (exception: Exception) {
                Log.w("DialerCode", "Unable to broadcast secret code using $action", exception)
            }
        }
    return true
}

private fun showDeviceIdentifier(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
    ) {
        return openDeviceInfoSettingsFallback(
            context,
            DeviceIdentifierFailure.PHONE_STATE_PERMISSION_REQUIRED
        )
    }

    val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return openDeviceInfoSettingsFallback(
                context,
                DeviceIdentifierFailure.NOT_AVAILABLE
            )

    val imei = try {
        telephonyManager.imei?.takeIf(String::isNotBlank)
    } catch (exception: SecurityException) {
        Log.w("DialerCode", "Android restricted IMEI access", exception)
        return openDeviceInfoSettingsFallback(
            context,
            DeviceIdentifierFailure.ACCESS_RESTRICTED
        )
    } catch (exception: UnsupportedOperationException) {
        Log.w("DialerCode", "IMEI access is unsupported on this device", exception)
        return openDeviceInfoSettingsFallback(
            context,
            DeviceIdentifierFailure.NOT_AVAILABLE
        )
    }
    if (imei == null) return openDeviceInfoSettingsFallback(
        context,
        DeviceIdentifierFailure.NOT_AVAILABLE
    )

    return try {
        android.app.AlertDialog.Builder(context)
            .setTitle("IMEI")
            .setMessage(imei)
            .setPositiveButton(android.R.string.ok, null)
            .show()
        true
    } catch (exception: android.view.WindowManager.BadTokenException) {
        Log.e("DialerCode", "Unable to display IMEI dialog", exception)
        showCodeError(context, "Android could not display the IMEI information")
        true
    }
}

private fun openDeviceInfoSettingsFallback(
    context: Context,
    failure: DeviceIdentifierFailure
): Boolean {
    showCodeError(context, deviceIdentifierFallbackMessage(failure))
    return try {
        context.startActivity(
            Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (exception: ActivityNotFoundException) {
        Log.w("DialerCode", "Device information settings are unavailable", exception)
        showCodeError(
            context,
            deviceInfoSettingsLaunchFailureMessage(DeviceInfoSettingsLaunchFailure.ACTIVITY_NOT_FOUND)
        )
        true
    } catch (exception: SecurityException) {
        Log.e("DialerCode", "Not allowed to open device information settings", exception)
        showCodeError(
            context,
            deviceInfoSettingsLaunchFailureMessage(DeviceInfoSettingsLaunchFailure.NOT_ALLOWED)
        )
        true
    }
}

/**
 * Dispatches a carrier code without passing it through contact matching, number formatting,
 * call confirmation, or the ordinary-call ACTION_DIAL fallback.
 */
fun dispatchCarrierDialerCode(
    context: Context,
    input: String,
    accountHandle: PhoneAccountHandle? = null
): Boolean {
    val code = input.trim()
    return when (selectDialerCodeDispatchPath(code)) {
        DialerCodeDispatchPath.TELEPHONY_USSD -> sendUssdRequest(context, code, accountHandle)
        DialerCodeDispatchPath.TELECOM_MMI -> placeMmiCode(context, code, accountHandle)
        else -> false
    }
}

fun carrierCodeNeedsSimSelection(context: Context, prefs: PreferenceManager): Boolean {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        ?: return false
    val accounts = try {
        telecomManager.callCapablePhoneAccounts
    } catch (exception: SecurityException) {
        return false
    }
    val configuredSim = prefs.getInt(
        PreferenceManager.KEY_DEFAULT_SIM,
        prefs.getDefaultSimIndexDefault()
    )
    return accounts.size > 1 && configuredSim == 0
}

fun configuredCarrierCodeAccount(context: Context, prefs: PreferenceManager): PhoneAccountHandle? {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        ?: return null
    val accounts = try {
        telecomManager.callCapablePhoneAccounts
    } catch (exception: SecurityException) {
        return null
    }
    val configuredSim = prefs.getInt(
        PreferenceManager.KEY_DEFAULT_SIM,
        prefs.getDefaultSimIndexDefault()
    )
    val slot = when (configuredSim) {
        1 -> 0
        2 -> 1
        3 -> prefs.getInt(PreferenceManager.KEY_LAST_USED_SIM_GLOBAL, 1) - 1
        else -> -1
    }
    return if (slot >= 0) getPhoneAccountForSimSlot(context, accounts, slot)
    else accounts.singleOrNull()
}

private fun sendUssdRequest(
    context: Context,
    code: String,
    accountHandle: PhoneAccountHandle?
): Boolean {
    if (!hasCallPhonePermission(context)) {
        showCodeError(context, "Phone permission is required to send a USSD request")
        return true
    }
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
        showCodeError(context, "USSD requires a device with cellular telephony")
        return true
    }

    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    if (telephonyManager == null) {
        showCodeError(context, "USSD is unavailable on this device")
        return true
    }

    val subscriptionId = accountHandle?.let { subscriptionIdForAccount(context, it) }
    if (accountHandle != null && subscriptionId == null) {
        showCodeError(context, "Could not determine the selected SIM for this USSD request")
        return true
    }
    val manager = subscriptionId?.let(telephonyManager::createForSubscriptionId) ?: telephonyManager
    return try {
        manager.sendUssdRequest(
            code,
            object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager,
                    request: String,
                    response: CharSequence
                ) {
                    Toast.makeText(context, response, Toast.LENGTH_LONG).show()
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager,
                    request: String,
                    failureCode: Int
                ) {
                    showCodeError(context, "The carrier could not complete this USSD request (error $failureCode)")
                }
            },
            Handler(Looper.getMainLooper())
        )
        true
    } catch (exception: SecurityException) {
        Log.e("DialerCode", "USSD request is not permitted", exception)
        showCodeError(context, "Android does not allow this app to send the USSD request")
        true
    } catch (exception: UnsupportedOperationException) {
        Log.e("DialerCode", "USSD is unsupported by this device", exception)
        showCodeError(context, "USSD is not supported on this device")
        true
    } catch (exception: IllegalStateException) {
        Log.e("DialerCode", "Telephony is unavailable for the USSD request", exception)
        showCodeError(context, "Cellular service is unavailable for this USSD request")
        true
    } catch (exception: IllegalArgumentException) {
        Log.e("DialerCode", "USSD request was rejected", exception)
        showCodeError(context, "Android rejected this USSD request")
        true
    }
}

private fun placeMmiCode(
    context: Context,
    code: String,
    accountHandle: PhoneAccountHandle?
): Boolean {
    if (!hasCallPhonePermission(context)) {
        showCodeError(context, "Phone permission is required to run this carrier service code")
        return true
    }
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    if (telecomManager == null) {
        showCodeError(context, "Telephony is unavailable for this carrier service code")
        return true
    }

    val extras = android.os.Bundle().apply {
        accountHandle?.let {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
        }
    }
    return try {
        telecomManager.placeCall(android.net.Uri.parse(buildTelUriString(code)), extras)
        true
    } catch (exception: SecurityException) {
        Log.e("DialerCode", "Carrier MMI request is not permitted", exception)
        showCodeError(context, "Android does not allow this carrier service code")
        true
    } catch (exception: IllegalArgumentException) {
        Log.e("DialerCode", "Carrier MMI request was rejected", exception)
        showCodeError(context, "Android rejected this carrier service code")
        true
    } catch (exception: IllegalStateException) {
        Log.e("DialerCode", "Telephony is unavailable for the carrier MMI request", exception)
        showCodeError(context, "Cellular service is unavailable for this carrier service code")
        true
    } catch (exception: UnsupportedOperationException) {
        Log.e("DialerCode", "Carrier MMI is unsupported by this device", exception)
        showCodeError(context, "Carrier service codes are not supported on this device")
        true
    }
}

private fun hasCallPhonePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED

private fun subscriptionIdForAccount(context: Context, handle: PhoneAccountHandle): Int? {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        ?: return handle.id.toIntOrNull()
    return try {
        telecomManager.getPhoneAccount(handle)?.extras
            ?.getInt("android.telecom.extra.SUBSCRIPTION_ID", -1)
            ?.takeIf { it >= 0 }
            ?: handle.id.toIntOrNull()
    } catch (exception: SecurityException) {
        Log.e("DialerCode", "Unable to resolve selected SIM subscription", exception)
        null
    }
}

private fun showCodeError(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

private fun startSettings(context: Context, action: String): Boolean = try {
    context.startActivity(
        Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    true
} catch (exception: ActivityNotFoundException) {
    false
} catch (exception: SecurityException) {
    Log.w("DialerCode", "Not allowed to open settings action $action", exception)
    false
}
