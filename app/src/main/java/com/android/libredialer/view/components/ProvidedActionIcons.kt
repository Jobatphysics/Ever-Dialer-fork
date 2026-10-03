package com.android.libredialer.view.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.android.libredialer.R

@Composable
internal fun providedCallIcon(): ImageVector = ImageVector.vectorResource(R.drawable.ic_provided_call)

@Composable
internal fun providedMessageIcon(): ImageVector = ImageVector.vectorResource(R.drawable.ic_provided_message)

@Composable
internal fun providedVideoCallIcon(): ImageVector = ImageVector.vectorResource(R.drawable.ic_provided_video_call)
