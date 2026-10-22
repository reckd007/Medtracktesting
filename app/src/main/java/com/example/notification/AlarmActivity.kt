package com.example.notification

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MedBorderTeal
import com.example.ui.theme.MedTealDark
import com.example.ui.theme.MedTealPrimary
import com.example.ui.theme.MedTealSoft
import com.example.ui.theme.MedTextPrimary
import com.example.ui.theme.MedTextSecondary

class AlarmActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AlarmActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and display over keyguard / lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME) ?: "Medication"
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
        val form = intent.getStringExtra(NotificationHelper.EXTRA_FORM) ?: "Pill"
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: "Now"
        val instructions = intent.getStringExtra(NotificationHelper.EXTRA_INSTRUCTIONS) ?: ""
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 1001)

        Log.d(TAG, "AlarmActivity launched for $medicationName ($dosage), notifId=$notificationId")

        setContent {
            MaterialTheme {
                AlarmScreen(
                    medicineName = medicationName,
                    dosage = dosage,
                    form = form,
                    scheduledTime = scheduledTime,
                    instructions = instructions,
                    onTaken = {
                        val takeIntent = Intent(this, MedicationActionReceiver::class.java).apply {
                            action = NotificationHelper.ACTION_TAKE_DOSE
                            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationId)
                            putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, medicationName)
                            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
                            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
                        }
                        sendBroadcast(takeIntent)
                        finish()
                    },
                    onStop = {
                        val stopIntent = Intent(this, MedicationActionReceiver::class.java).apply {
                            action = NotificationHelper.ACTION_STOP_ALARM
                            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationId)
                            putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, medicationName)
                            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
                        }
                        sendBroadcast(stopIntent)
                        finish()
                    },
                    onSnooze = {
                        val snoozeIntent = Intent(this, MedicationActionReceiver::class.java).apply {
                            action = NotificationHelper.ACTION_SNOOZE_DOSE
                            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationId)
                            putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, medicationName)
                            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
                            putExtra(NotificationHelper.EXTRA_FORM, form)
                            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
                            putExtra(NotificationHelper.EXTRA_INSTRUCTIONS, instructions)
                            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
                        }
                        sendBroadcast(snoozeIntent)
                        finish()
                    }
                )
            }
        }
    }

    /**
     * Requirement 5: Physical volume buttons stop the active ringing alarm.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    Log.d(TAG, "Physical volume button pressed during alarm: silencing audio")
                    AlarmRingtoneService.stopAlarm(this)
                    finish()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun AlarmScreen(
    medicineName: String,
    dosage: String,
    form: String,
    scheduledTime: String,
    instructions: String,
    onTaken: () -> Unit,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("alarm_screen_surface"),
        color = Color(0xFFF0FDF4) // Soft medical mint background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Alarm Header & Pulsing Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MedTealSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Ringing Alarm",
                        tint = MedTealPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                ) {
                    Text(
                        text = "MEDICATION REMINDER • RINGING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTealDark,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Medicine Details Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Time to take your medicine",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MedTextSecondary
                    )

                    Text(
                        text = medicineName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    if (dosage.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MedTealSoft
                        ) {
                            Text(
                                text = "$dosage • $form",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTealPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MedTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Scheduled: $scheduledTime",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MedTextSecondary
                        )
                    }

                    if (instructions.isNotBlank()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                        Text(
                            text = instructions,
                            fontSize = 13.sp,
                            color = MedTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Stoppable via Physical Volume Buttons Hint
            Text(
                text = "Tip: Press phone Volume-Up or Volume-Down to silence ringing",
                fontSize = 12.sp,
                color = MedTextSecondary,
                textAlign = TextAlign.Center
            )

            // Action Buttons: TAKEN, STOP, SNOOZE
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TAKEN BUTTON (Primary Green)
                Button(
                    onClick = onTaken,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("alarm_screen_taken_button")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TAKEN — Log Dose", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // STOP BUTTON (Silences alarm, keeps dose pending)
                    OutlinedButton(
                        onClick = onStop,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444))
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("alarm_screen_stop_button")
                    ) {
                        Icon(imageVector = Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    // SNOOZE 15m BUTTON
                    OutlinedButton(
                        onClick = onSnooze,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MedTealPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MedTealPrimary)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("alarm_screen_snooze_button")
                    ) {
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Snooze 15m", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
