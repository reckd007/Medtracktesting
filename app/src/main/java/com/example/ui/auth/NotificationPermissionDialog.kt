package com.example.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

/**
 * Requirement 5.5: Notification permission prompt with clear healthcare rationale.
 */
@Composable
fun NotificationPermissionPromptDialog(
    onEnableClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Dialog(onDismissRequest = onSkipClick) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MedTealSoft,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notification Reminders",
                            tint = MedTealPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Never Miss a Dose",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedLightTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Medtrack relies on precise notifications and alarms to remind you of your scheduled prescriptions, low refill stockpiles, and doctor appointments.",
                    fontSize = 13.5.sp,
                    color = MedLightTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        RationaleRow(
                            icon = Icons.Default.Alarm,
                            text = "Timely medication dosage alerts right on your lock screen"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RationaleRow(
                            icon = Icons.Default.Shield,
                            text = "Low refill warnings before you run out of essential pills"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onEnableClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("notif_dialog_enable_button")
                ) {
                    Text(
                        text = "Enable Reminders",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onSkipClick,
                    modifier = Modifier.testTag("notif_dialog_skip_button")
                ) {
                    Text(
                        text = "Not Now",
                        color = Color(0xFF64748B),
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RationaleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MedtrackMint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            color = MedLightTextPrimary,
            lineHeight = 16.sp
        )
    }
}
