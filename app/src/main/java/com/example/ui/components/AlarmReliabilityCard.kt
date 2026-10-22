package com.example.ui.components

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.AlertRingtoneManager
import com.example.notification.MedicationAlarmScheduler
import com.example.ui.theme.MedBorderTeal
import com.example.ui.theme.MedTealBg
import com.example.ui.theme.MedTealDark
import com.example.ui.theme.MedTealPrimary
import com.example.ui.theme.MedTealSoft
import com.example.ui.theme.MedTextPrimary
import com.example.ui.theme.MedTextSecondary

@Composable
fun AlarmReliabilityCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var testStatusMessage by remember { mutableStateOf<String?>(null) }

    val notificationsEnabled = remember {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val exactAlarmsAllowed = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    val batteryOptimizationsIgnored = remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    val selectedRingtone = remember {
        AlertRingtoneManager.getSelectedRingtone(context)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("alarm_reliability_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MedTealSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Alarm Reliability",
                        tint = MedTealPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Alarm Reliability & Permissions",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                    Text(
                        text = "Ensure timely reminders when app is closed",
                        fontSize = 12.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status Row 1: Notifications
            StatusItemRow(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                description = if (notificationsEnabled) "Permission granted to post reminders" else "Permission denied. Reminders will not appear!",
                isOk = notificationsEnabled,
                actionLabel = if (!notificationsEnabled) "Enable in Settings" else null,
                onAction = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )

            // Status Row 2: Exact Alarms
            StatusItemRow(
                icon = Icons.Default.Alarm,
                title = "Exact Alarm Timing",
                description = if (exactAlarmsAllowed) "Precision scheduling allowed by OS" else "OS restricted exact timing. May cause alarm delays.",
                isOk = exactAlarmsAllowed,
                actionLabel = if (!exactAlarmsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Allow Exact Alarms" else null,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                }
            )

            // Status Row 3: Active Alarm Sound
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MedTealBg,
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Alarm Sound",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Text(
                            text = "${selectedRingtone.title} (${if (selectedRingtone.isPreset) "Built-in Preset" else "Local Upload"})",
                            fontSize = 12.sp,
                            color = MedTealDark
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active sound",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Status Row 4: Battery Optimization
            StatusItemRow(
                icon = Icons.Default.BatteryAlert,
                title = "Background Battery Policy",
                description = if (batteryOptimizationsIgnored) "Unrestricted background execution" else "Battery saver may delay alarms if background restricted.",
                isOk = batteryOptimizationsIgnored,
                actionLabel = if (!batteryOptimizationsIgnored) "Adjust Battery Policy" else null,
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        context.startActivity(intent)
                    }
                }
            )

            // Test Alarm in 5 Seconds Button
            Button(
                onClick = {
                    MedicationAlarmScheduler.scheduleQuickTestAlarm(context, delaySeconds = 5)
                    testStatusMessage = "Test alarm scheduled! Lock your phone or exit app — alarm fires in 5s."
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("test_reliability_alarm_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule 5-Second Test Alarm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            testStatusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = Color(0xFF166534),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isOk: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isOk) MedTealPrimary else Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOk) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isOk) Color(0xFF166534) else Color(0xFF991B1B),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isOk) "Active" else "Attention",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOk) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                    }
                }
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = MedTextSecondary,
                lineHeight = 16.sp
            )

            if (actionLabel != null) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedTealPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
