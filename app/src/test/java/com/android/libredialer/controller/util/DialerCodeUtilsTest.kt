package com.android.libredialer.controller.util

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialerCodeUtilsTest {
    @Test
    fun classifiesLocalAndAndroidSecretCodesSeparately() {
        assertEquals(DialerCodeType.LOCAL, classifyDialerCode("*#06#"))
        assertEquals(DialerCodeType.SECRET, classifyDialerCode("*#*#4636#*#*"))
    }

    @Test
    fun classifiesCarrierUssdAndSupplementaryServiceCodes() {
        assertEquals(DialerCodeType.USSD, classifyDialerCode("*123#"))
        assertEquals(DialerCodeType.USSD, classifyDialerCode("*123*1#"))
        assertEquals(DialerCodeType.MMI, classifyDialerCode("*#21#"))
        assertEquals(DialerCodeType.MMI, classifyDialerCode("##002#"))
        assertEquals(DialerCodeType.MMI, classifyDialerCode("*21*123456789#"))
    }

    @Test
    fun ordinaryNumbersAreNotClassifiedAsCarrierCodes() {
        assertEquals(DialerCodeType.NONE, classifyDialerCode("+1 555 123 4567"))
        assertEquals(DialerCodeType.NONE, classifyDialerCode("5551234567"))
    }

    @Test
    fun selectsThePlatformDispatchPathForEachDialerCodeCategory() {
        assertEquals(
            DialerCodeDispatchPath.DEVICE_IDENTIFIER_DISPLAY,
            selectDialerCodeDispatchPath("*#06#")
        )
        assertEquals(
            DialerCodeDispatchPath.TELEPHONY_USSD,
            selectDialerCodeDispatchPath("*123#")
        )
        assertEquals(
            DialerCodeDispatchPath.TELECOM_MMI,
            selectDialerCodeDispatchPath("*#21#")
        )
        assertEquals(
            DialerCodeDispatchPath.TELECOM_MMI,
            selectDialerCodeDispatchPath("##002#")
        )
        assertEquals(
            DialerCodeDispatchPath.ORDINARY_CALL,
            selectDialerCodeDispatchPath("+919876543210")
        )
    }

    @Test
    fun imeiCodeUsesSettingsFallbackNotCarrierOrOrdinaryCallDispatch() {
        assertEquals(DialerCodeType.LOCAL, classifyDialerCode("*#06#"))
        assertEquals(
            DialerCodeDispatchPath.DEVICE_IDENTIFIER_DISPLAY,
            selectDialerCodeDispatchPath("*#06#")
        )
        assertEquals(false, dispatchesAsCarrierCode("*#06#"))
        assertEquals(false, dispatchesAsOrdinaryCall("*#06#"))
    }

    @Test
    fun phonePermissionIsRequiredOnlyForTelephonyDispatch() {
        assertEquals(false, dialerCodeNeedsCallPhonePermission("*#06#"))
        assertEquals(true, dialerCodeNeedsCallPhonePermission("*123#"))
        assertEquals(true, dialerCodeNeedsCallPhonePermission("*#21#"))
        assertEquals(false, dialerCodeNeedsCallPhonePermission("*#*#4636#*#*"))
        assertEquals(false, dialerCodeNeedsCallPhonePermission("+919876543210"))
    }

    @Test
    fun requestsPhoneStateOnlyForLegacyImeiAccessWhenPermissionIsMissing() {
        assertEquals(true, shouldRequestPhoneStateForDeviceIdentifier("*#06#", 28, false))
        assertEquals(false, shouldRequestPhoneStateForDeviceIdentifier("*#06#", 28, true))
        assertEquals(false, shouldRequestPhoneStateForDeviceIdentifier("*#06#", 29, false))
        assertEquals(false, shouldRequestPhoneStateForDeviceIdentifier("*123#", 28, false))
    }

    @Test
    fun deviceIdentifierFailuresExplainTheRestrictionAndSettingsFallback() {
        assertEquals(
            true,
            deviceIdentifierFallbackMessage(DeviceIdentifierFailure.ACCESS_RESTRICTED)
                .contains("Android restricts IMEI access")
        )
        assertEquals(
            true,
            deviceIdentifierFallbackMessage(DeviceIdentifierFailure.PHONE_STATE_PERMISSION_REQUIRED)
                .contains("Phone permission is needed")
        )
        assertEquals(
            true,
            deviceIdentifierFallbackMessage(DeviceIdentifierFailure.NOT_AVAILABLE)
                .contains("IMEI may not be shown")
        )
    }

    @Test
    fun settingsLaunchFailuresProduceVisibleErrors() {
        assertEquals(
            "Device information Settings are unavailable on this device",
            deviceInfoSettingsLaunchFailureMessage(
                DeviceInfoSettingsLaunchFailure.ACTIVITY_NOT_FOUND
            )
        )
        assertEquals(
            "Android did not allow Device Information Settings to open",
            deviceInfoSettingsLaunchFailureMessage(
                DeviceInfoSettingsLaunchFailure.NOT_ALLOWED
            )
        )
    }

    @Test
    fun imeiCodeIsEncodedAsTelDataRatherThanAUriFragment() {
        val uriString = buildTelUriString("*#06#")
        val uri = URI(uriString)

        assertEquals("tel:*%2306%23", uriString)
        assertNull(uri.rawFragment)
        assertEquals("*#06#", uri.schemeSpecificPart)
    }

    @Test
    fun mmiCodeRetainsItsSpecialCharacters() {
        val uri = URI(buildTelUriString("*#21#"))

        assertNull(uri.rawFragment)
        assertEquals("*#21#", uri.schemeSpecificPart)
    }

    @Test
    fun supplementaryServiceCodeRetainsTheFullOriginalString() {
        listOf("##002#", "*21*+123456789#").forEach { code ->
            val uri = URI(buildTelUriString(code))
            assertNull(uri.rawFragment)
            assertEquals(code, uri.schemeSpecificPart)
        }
    }

    @Test
    fun internationalPhoneNumberRetainsItsLeadingPlus() {
        val uri = URI(buildTelUriString("+1 555 123 4567"))

        assertEquals("+1 555 123 4567", uri.schemeSpecificPart)
    }

    private fun dispatchesAsCarrierCode(input: String): Boolean =
        selectDialerCodeDispatchPath(input) in setOf(
            DialerCodeDispatchPath.TELEPHONY_USSD,
            DialerCodeDispatchPath.TELECOM_MMI
        )

    private fun dispatchesAsOrdinaryCall(input: String): Boolean =
        selectDialerCodeDispatchPath(input) == DialerCodeDispatchPath.ORDINARY_CALL
}
