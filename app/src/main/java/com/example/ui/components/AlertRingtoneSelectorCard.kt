package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.AlertRingtone
import com.example.notification.AlertRingtoneManager
import com.example.ui.theme.MedBorderTeal
import com.example.ui.theme.MedTealBg
import com.example.ui.theme.MedTealDark
import com.example.ui.theme.MedTealPrimary
import com.example.ui.theme.MedTealSoft
import com.example.ui.theme.MedTextPrimary
import com.example.ui.theme.MedTextSecondary

@Composable
fun AlertRingtoneSelectorCard(
    onTestNotificationRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedRingtone by remember {
        mutableStateOf(AlertRingtoneManager.getSelectedRingtone(context))
    }
    var isExpanded by remember { mutableStateOf(false) }
    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    val presetList = remember { AlertRingtoneManager.getPresetRingtones(context) }

    // Launcher for uploading custom local ringtone files (.mp3, .wav, .m4a, .aac)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val originalFilename = queryFilename(context, uri)
            val imported = AlertRingtoneManager.saveUploadedRingtone(context, uri, originalFilename)
            if (imported != null) {
                selectedRingtone = imported
                uploadStatusMessage = "Imported: ${imported.title}"
                AlertRingtoneManager.stopPreview()
                currentlyPlayingUri = null
            } else {
                uploadStatusMessage = "Failed to import audio file. Please try standard MP3 or WAV."
            }
        }
    }

    // Stop audio preview when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            AlertRingtoneManager.stopPreview()
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("custom_ringtone_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title & Icon Header (Full width for title & subtitle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Ringtone Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Medication Alarm Sound",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "3 Presets & Local File Upload",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Change Tone",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Description summary
            Text(
                text = "Select from 3 high-clarity alarm presets or upload your own audio file (.mp3, .wav, .m4a, .aac). Audio previews auto-stop after 15 seconds.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // Current selection bar with preview player button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active: ${selectedRingtone.title}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentlyPlayingUri == selectedRingtone.uriString) "Playing preview (15s timer active)..." else "Tap to change sound • ${selectedRingtone.description}",
                            fontSize = 11.sp,
                            color = if (currentlyPlayingUri == selectedRingtone.uriString) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play/Stop Preview Button (15-second automatic timeout)
                    val isPlayingThis = currentlyPlayingUri == selectedRingtone.uriString
                    Button(
                        onClick = {
                            if (isPlayingThis) {
                                AlertRingtoneManager.stopPreview()
                                currentlyPlayingUri = null
                            } else {
                                val success = AlertRingtoneManager.playPreview(context, selectedRingtone.uriString) {
                                    currentlyPlayingUri = null
                                }
                                if (success) {
                                    currentlyPlayingUri = selectedRingtone.uriString
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayingThis) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 86.dp)
                            .testTag("preview_active_ringtone_button")
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Preview Tone",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlayingThis) "Stop" else "Preview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Expandable Ringtone List & Picker Options
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "3 Preset Alarm Sounds (15s):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )

                    // 3 Built-In Preset Ringtone Options
                    presetList.forEachIndexed { index, tone ->
                        val isSelected = selectedRingtone.id == tone.id
                        val isPlaying = currentlyPlayingUri == tone.uriString

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MedTealSoft.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) MedTealPrimary else Color(0xFFE2E8F0))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AlertRingtoneManager.setSelectedRingtone(context, tone)
                                    selectedRingtone = tone
                                }
                                .testTag("ringtone_preset_${index + 1}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            AlertRingtoneManager.setSelectedRingtone(context, tone)
                                            selectedRingtone = tone
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = MedTealPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Preset ${index + 1}: ${tone.title}",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MedTextPrimary
                                        )
                                        Text(
                                            text = tone.description,
                                            fontSize = 11.sp,
                                            color = MedTextSecondary
                                        )
                                    }
                                }

                                // Quick Preview Button for each preset
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            AlertRingtoneManager.stopPreview()
                                            currentlyPlayingUri = null
                                        } else {
                                            val success = AlertRingtoneManager.playPreview(context, tone.uriString) {
                                                currentlyPlayingUri = null
                                            }
                                            if (success) {
                                                currentlyPlayingUri = tone.uriString
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("preview_preset_${index + 1}")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview ${tone.title}",
                                        tint = if (isPlaying) Color(0xFFEF4444) else MedTealPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Display Uploaded Custom Ringtone (if user has imported one)
                    if (!selectedRingtone.isPreset) {
                        Text(
                            text = "Currently Uploaded Sound:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        val isPlayingCustom = currentlyPlayingUri == selectedRingtone.uriString

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MedTealSoft.copy(alpha = 0.5f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(MedTealPrimary)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = true,
                                        onClick = {},
                                        colors = RadioButtonDefaults.colors(selectedColor = MedTealPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = selectedRingtone.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MedTextPrimary
                                        )
                                        Text(
                                            text = "App-private durable storage (${selectedRingtone.description})",
                                            fontSize = 11.sp,
                                            color = MedTextSecondary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (isPlayingCustom) {
                                            AlertRingtoneManager.stopPreview()
                                            currentlyPlayingUri = null
                                        } else {
                                            val success = AlertRingtoneManager.playPreview(context, selectedRingtone.uriString) {
                                                currentlyPlayingUri = null
                                            }
                                            if (success) {
                                                currentlyPlayingUri = selectedRingtone.uriString
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingCustom) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview Custom",
                                        tint = if (isPlayingCustom) Color(0xFFEF4444) else MedTealPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Button: Upload Local Ringtone from Device
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch("audio/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("upload_audio_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = MedTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload Local Audio File (MP3, WAV, M4A)...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTealPrimary
                        )
                    }

                    uploadStatusMessage?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = MedTealDark,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Button 2: Test 3-Second Alarm (Trigger real AlarmManager + Notification + Sound)
                    Button(
                        onClick = {
                            onTestNotificationRequested()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("test_custom_ringtone_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Alarm (Fires in 3s with active sound)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun queryFilename(context: Context, uri: Uri): String? {
    var name: String? = null
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
    } catch (e: Exception) {
        // Fallback to last path segment
        name = uri.lastPathSegment
    }
    return name
}
