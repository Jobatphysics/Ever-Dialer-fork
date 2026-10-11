package com.coolappstore.evercallrecorder.by.svhp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_SETTINGS = "com.coolappstore.evercallrecorder.by.svhp.EXTRA_OPEN_SETTINGS"
        const val EXTRA_OPEN_RECORDING_URI = "com.coolappstore.evercallrecorder.by.svhp.EXTRA_OPEN_RECORDING_URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val openSettingsDirectly = intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true
        val openedRecordingUri = intent?.getStringExtra(EXTRA_OPEN_RECORDING_URI)
        setContent {
            AppNavigationScreen(
                openSettingsDirectly = openSettingsDirectly,
                openedRecordingUri = openedRecordingUri
            )
        }
    }
}
