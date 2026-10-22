package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MedBorderTeal
import com.example.ui.theme.MedTealPrimary
import com.example.ui.theme.MedTealSoft
import com.example.ui.theme.MedTextPrimary
import com.example.ui.theme.MedTextSecondary

enum class LegalTab { TERMS, PRIVACY }

@Composable
fun TermsAndPrivacyDialog(
    initialTab: LegalTab = LegalTab.TERMS,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .testTag("terms_privacy_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MedTealSoft,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedTab == LegalTab.TERMS) Icons.Outlined.HealthAndSafety else Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MedTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = if (selectedTab == LegalTab.TERMS) "Terms of Service" else "Privacy Policy",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_legal_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MedTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    TabPill(
                        title = "Terms of Service",
                        selected = selectedTab == LegalTab.TERMS,
                        onClick = { selectedTab = LegalTab.TERMS },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        title = "Privacy Policy",
                        selected = selectedTab == LegalTab.PRIVACY,
                        onClick = { selectedTab = LegalTab.PRIVACY },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == LegalTab.TERMS) {
                        Text(
                            text = "Last updated: October 2026",
                            fontSize = 12.sp,
                            color = MedTextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        LegalSection(
                            title = "1. Medical Disclaimer",
                            content = "MedTrack is an informational medication scheduling and adherence tracking tool designed to assist users in organizing their treatment plans. MedTrack does NOT provide medical advice, diagnosis, or emergency clinical intervention. Always consult your qualified healthcare provider, physician, or pharmacist regarding your health, prescription dosages, drug interactions, or symptoms. In case of a medical emergency, call your local emergency services (e.g. 911 or 112) immediately."
                        )

                        LegalSection(
                            title = "2. User Responsibilities",
                            content = "You are responsible for ensuring that medication dosages, schedules, and instructions entered into the app accurately reflect your licensed physician's prescriptions. While MedTrack schedules reliable reminders and dosage logs, technology can fail due to device battery, OS battery optimization, or network conditions; you should not rely exclusively on MedTrack for life-critical medications."
                        )

                        LegalSection(
                            title = "3. Age Requirements",
                            content = "You must be at least 13 years of age to register an independent account with MedTrack. If you are under 18, you represent that you have received parental or guardian permission to track medications with MedTrack."
                        )

                        LegalSection(
                            title = "4. Account Security",
                            content = "You agree to safeguard your account credentials and not disclose your password to unauthorized third parties. MedTrack encrypts session credentials and supports local offline privacy."
                        )
                    } else {
                        Text(
                            text = "HIPAA-Aligned Privacy Notice • October 2026",
                            fontSize = 12.sp,
                            color = MedTextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        LegalSection(
                            title = "1. Protection of Health Data",
                            content = "Your privacy is paramount. Personal health data, medication schedules, dosage logs, and vitals entered into MedTrack belong strictly to you. MedTrack does NOT sell your personal health information to data brokers, pharmaceutical advertisers, or commercial third parties."
                        )

                        LegalSection(
                            title = "2. Storage & Encryption",
                            content = "All sensitive data stored locally on your Android device utilizes private sandbox storage and Android Keystore-protected mechanisms. When synchronized with cloud infrastructure (e.g., Supabase), communication is secured end-to-end via Transport Layer Security (TLS 1.3) with encrypted database tables."
                        )

                        LegalSection(
                            title = "3. Notifications & Permissions",
                            content = "MedTrack requests Notification and Alarm permissions strictly to trigger scheduled dose reminders and low-supply refill alerts. These permissions are never used to track your location or deliver unrequested commercial promotions."
                        )

                        LegalSection(
                            title = "4. Data Deletion & Export",
                            content = "You maintain full autonomy to export your medication logs or permanently delete your account and associated records directly from the application settings at any time."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Acknowledgment Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("accept_legal_btn")
                ) {
                    Text("I Understand & Agree", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color.White else Color.Transparent,
        border = if (selected) CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)) else null,
        modifier = modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MedTealPrimary else MedTextSecondary
            )
        }
    }
}

@Composable
private fun LegalSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MedTextPrimary)
        Text(text = content, fontSize = 12.sp, color = MedTextSecondary, lineHeight = 17.sp)
    }
}
