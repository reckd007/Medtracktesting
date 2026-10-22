package com.example.ui.screens

import androidx.compose.animation.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPreferences
import com.example.data.model.FamilyMember
import com.example.data.model.UserAccountInfo
import com.example.ui.components.MedTrackCapsuleLogo
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    userAccount: UserAccountInfo,
    familyMembers: List<FamilyMember>,
    appPreferences: AppPreferences,
    onUpdateAccount: (UserAccountInfo) -> Unit,
    onAddFamilyMember: (name: String, relationship: String, age: String, bloodType: String, allergies: String, emergencyContact: String) -> Unit,
    onRemoveFamilyMember: (String) -> Unit,
    onUpdatePreferences: (AppPreferences) -> Unit,
    onOpenRemindersAndAlarms: () -> Unit,
    onVerifyFamilyMember: ((FamilyMember) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onDeleteAccount: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var showAddFamilyDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<FamilyMember?>(null) }
    var feedbackSnackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            if (feedbackSnackbarMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { feedbackSnackbarMessage = null }) {
                            Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Text(feedbackSnackbarMessage ?: "")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    MedTrackCapsuleLogo(height = 34)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Account info, family care circle, themes, alarms & preferences",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Hero Section: Reminder & Alarm Settings Button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(3.dp, RoundedCornerShape(22.dp))
                        .testTag("settings_reminders_and_alarms_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Alarms",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Reminder & Alarm Settings",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Custom ringtones, AlarmManager push schedules, refill alerts & injection site rotation",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Button(
                            onClick = onOpenRemindersAndAlarms,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_open_reminder_and_alarm_settings")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Configure",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section: Alarm Reliability & Permissions
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    com.example.ui.components.AlarmReliabilityCard()
                }
            }

            // Section: Theme, Appearance & Palette
            item {
                ThemeAndAppearanceCard(
                    appPreferences = appPreferences,
                    onUpdatePreferences = onUpdatePreferences
                )
            }

            // Section 2: Account Information
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(2.dp, RoundedCornerShape(22.dp))
                        .testTag("settings_account_info_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MedTealSoft,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = userAccount.fullName.split(" ")
                                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                                .take(2)
                                                .joinToString(""),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MedTealPrimary
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Account & Health ID",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTextPrimary
                                    )
                                    Text(
                                        text = "Primary patient profile details",
                                        fontSize = 11.sp,
                                        color = MedTextSecondary
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showEditAccountDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("settings_edit_account_btn")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = MedTealPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTealPrimary, softWrap = false, maxLines = 1)
                            }
                        }

                        HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountDetailRow(icon = Icons.Outlined.Person, label = "Full Name", value = userAccount.fullName)
                            AccountDetailRow(icon = Icons.Outlined.Email, label = "Email Address", value = userAccount.email)
                            AccountDetailRow(icon = Icons.Outlined.Phone, label = "Emergency Contact", value = userAccount.phone)
                            AccountDetailRow(icon = Icons.Outlined.Bloodtype, label = "Blood Group", value = userAccount.bloodType)
                            AccountDetailRow(icon = Icons.Outlined.MedicalServices, label = "Primary Doctor", value = userAccount.primaryDoctor)
                            AccountDetailRow(icon = Icons.Outlined.HealthAndSafety, label = "Insurance Policy", value = userAccount.insuranceProvider)
                            AccountDetailRow(icon = Icons.Outlined.LocalPharmacy, label = "Preferred Pharmacy", value = userAccount.preferredPharmacy)
                        }

                        HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.5f))

                        // Account Session & Logout Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MedSuccessBg
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MedSuccess,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Active Session",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedSuccess
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (onLogout != null) {
                                    OutlinedButton(
                                        onClick = { showLogoutConfirmation = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFDC2626)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("settings_logout_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Log Out", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (onDeleteAccount != null) {
                                    IconButton(
                                        onClick = { showDeleteAccountConfirmation = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("settings_delete_account_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Account",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Family Care Circle (Family Info & Adding / Removing Members)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(2.dp, RoundedCornerShape(22.dp))
                        .testTag("settings_family_care_circle_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Family & Dependents",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTextPrimary
                                    )
                                    Text(
                                        text = "${familyMembers.size} member${if (familyMembers.size != 1) "s" else ""} in care circle",
                                        fontSize = 11.sp,
                                        color = MedTextSecondary
                                    )
                                }
                            }

                            Button(
                                onClick = { showAddFamilyDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("settings_add_family_member_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Add family members or dependents to monitor their medication regimens, log health entries, and switch profiles instantly from the header.",
                            fontSize = 12.sp,
                            color = MedTextSecondary,
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.5f))

                        // List of family members
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            familyMembers.forEach { member ->
                                FamilyMemberItemCard(
                                    member = member,
                                    onRemove = { memberToRemove = member },
                                    onVerify = { onVerifyFamilyMember?.invoke(member) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: App & Health Preferences
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(2.dp, RoundedCornerShape(22.dp))
                        .testTag("settings_app_preferences_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Alerts & Measurements",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                                Text(
                                    text = "System alert toggles and clinical measurement units",
                                    fontSize = 11.sp,
                                    color = MedTextSecondary
                                )
                            }
                        }

                        HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.5f))

                        PreferenceToggleRow(
                            title = "Push Reminders",
                            subtitle = "Deliver dose alerts via AlarmManager even when app is closed",
                            checked = appPreferences.pushNotificationsEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(appPreferences.copy(pushNotificationsEnabled = it))
                            }
                        )

                        PreferenceToggleRow(
                            title = "Play Sound with Alarms",
                            subtitle = "Emit custom ringtone or audio chime for scheduled alerts",
                            checked = appPreferences.alertSoundEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(appPreferences.copy(alertSoundEnabled = it))
                            }
                        )

                        PreferenceToggleRow(
                            title = "Vibrate on Dose Alert",
                            subtitle = "Distinct vibration pattern (0, 300, 200, 300 ms)",
                            checked = appPreferences.alertVibrationEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(appPreferences.copy(alertVibrationEnabled = it))
                            }
                        )

                        HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.5f))

                        // Default Refill Threshold Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Default Low Stock Alert Threshold",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                                Text(
                                    text = "Notify when remaining units drop below this number",
                                    fontSize = 11.sp,
                                    color = MedTextSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5, 10, 15, 20).forEach { count ->
                                    val isSelected = appPreferences.defaultLowStockThreshold == count
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MedTealPrimary else MedTealBg,
                                        modifier = Modifier.clickable {
                                            onUpdatePreferences(appPreferences.copy(defaultLowStockThreshold = count))
                                        }
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MedTextPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Snooze Minutes Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Default Snooze Duration",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                                Text(
                                    text = "Delay duration when pressing Snooze on dose cards",
                                    fontSize = 11.sp,
                                    color = MedTextSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(10, 15, 20, 30).forEach { mins ->
                                    val isSelected = appPreferences.snoozeMinutes == mins
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MedTealPrimary else MedTealBg,
                                        modifier = Modifier.clickable {
                                            onUpdatePreferences(appPreferences.copy(snoozeMinutes = mins))
                                        }
                                    ) {
                                        Text(
                                            text = "${mins}m",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MedTextPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: MedTrack AI Settings (at the bottom of the page)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(2.dp, RoundedCornerShape(22.dp))
                        .testTag("settings_ai_section_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MedTealSoft,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MedTealPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "MedTrack AI Settings",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTextPrimary
                                    )
                                    Text(
                                        text = "Smart assistant & medication privacy",
                                        fontSize = 11.sp,
                                        color = MedTextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (appPreferences.aiSeeMedicinesEnabled) MedTealSoft else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (appPreferences.aiSeeMedicinesEnabled) "SHARING ACTIVE" else "PRIVATE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (appPreferences.aiSeeMedicinesEnabled) MedTealDark else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.5f))

                        // Switch: Let AI assistant see my medicines
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Let AI assistant see my medicines",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Sends your active profile's medication names, dosages, and allergies to MedTrack AI so it gives accurate guidance on schedules, interactions, and missed doses.",
                                    fontSize = 11.sp,
                                    color = MedTextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = appPreferences.aiSeeMedicinesEnabled,
                                onCheckedChange = { checked ->
                                    onUpdatePreferences(
                                        appPreferences.copy(
                                            aiSeeMedicinesEnabled = checked,
                                            aiSeenMedPrompt = true
                                        )
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MedTealPrimary
                                ),
                                modifier = Modifier.testTag("settings_ai_see_medicines_switch")
                            )
                        }

                        // Reset In-Chat AI Tips & Prompts
                        if (appPreferences.aiHideDisclaimer || appPreferences.aiSeenMedPrompt) {
                            HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Restore In-Chat AI Tips & Prompts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MedTextPrimary
                                    )
                                    Text(
                                        text = "Unhide safety disclaimer banner in Ask AI chat",
                                        fontSize = 10.sp,
                                        color = MedTextSecondary
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        onUpdatePreferences(
                                            appPreferences.copy(
                                                aiHideDisclaimer = false
                                            )
                                        )
                                    }
                                ) {
                                    Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTealPrimary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Medtrack Healthcare • v2.4.0",
                        fontSize = 12.sp,
                        color = MedTextSecondary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Modal Dialog: Logout Confirmation
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Log Out of Medtrack?",
                    fontWeight = FontWeight.Bold,
                    color = MedLightTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out? Your medication schedules and health diary records are safely stored with your account. You can log back in at any time.",
                    fontSize = 13.5.sp,
                    color = MedLightTextSecondary,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onLogout?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Modal Dialog: Delete Account Confirmation
    if (showDeleteAccountConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Account?",
                    fontWeight = FontWeight.Bold,
                    color = MedLightTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will terminate your account session and clear your active profile from this device. Are you sure you wish to proceed?",
                    fontSize = 13.5.sp,
                    color = MedLightTextSecondary,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountConfirmation = false
                        onDeleteAccount?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirmation = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Modal Dialog: Edit Account Info
    if (showEditAccountDialog) {
        var editName by remember { mutableStateOf(userAccount.fullName) }
        var editEmail by remember { mutableStateOf(userAccount.email) }
        var editPhone by remember { mutableStateOf(userAccount.phone) }
        var editBloodType by remember { mutableStateOf(userAccount.bloodType) }
        var editDoctor by remember { mutableStateOf(userAccount.primaryDoctor) }
        var editInsurance by remember { mutableStateOf(userAccount.insuranceProvider) }
        var editPharmacy by remember { mutableStateOf(userAccount.preferredPharmacy) }

        AlertDialog(
            onDismissRequest = { showEditAccountDialog = false },
            title = {
                Text(
                    text = "Edit Account Information",
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Patient Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Emergency Contact Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBloodType,
                        onValueChange = { editBloodType = it },
                        label = { Text("Blood Group (e.g. O+, A+)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDoctor,
                        onValueChange = { editDoctor = it },
                        label = { Text("Primary Care Physician") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editInsurance,
                        onValueChange = { editInsurance = it },
                        label = { Text("Insurance Provider / Policy #") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPharmacy,
                        onValueChange = { editPharmacy = it },
                        label = { Text("Preferred Pharmacy") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = userAccount.copy(
                            fullName = editName.trim().ifBlank { userAccount.fullName },
                            email = editEmail.trim().ifBlank { userAccount.email },
                            phone = editPhone.trim().ifBlank { userAccount.phone },
                            bloodType = editBloodType.trim().ifBlank { userAccount.bloodType },
                            primaryDoctor = editDoctor.trim().ifBlank { userAccount.primaryDoctor },
                            insuranceProvider = editInsurance.trim().ifBlank { userAccount.insuranceProvider },
                            preferredPharmacy = editPharmacy.trim().ifBlank { userAccount.preferredPharmacy }
                        )
                        onUpdateAccount(updated)
                        showEditAccountDialog = false
                        feedbackSnackbarMessage = "Account information successfully updated!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAccountDialog = false }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
        )
    }

    // Modal Dialog: Add Family Member
    if (showAddFamilyDialog) {
        var memberName by remember { mutableStateOf("") }
        var memberRelation by remember { mutableStateOf("Uncle") }
        var memberAge by remember { mutableStateOf("") }
        var memberBloodType by remember { mutableStateOf("") }
        var memberAllergies by remember { mutableStateOf("") }
        var memberPatientPhone by remember { mutableStateOf("") }
        var hasError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val relationOptions = listOf("Uncle", "Father", "Mother", "Child", "Spouse", "Grandparent", "Other")

        AlertDialog(
            onDismissRequest = { showAddFamilyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MedTealPrimary)
                    Text("Add Dependent / Family Relative", fontWeight = FontWeight.Bold, color = MedTextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Security Explanation Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Text(
                                text = "A random 6-digit verification code will be sent to the patient's phone. Medicine schedule access remains locked until confirmed.",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {
                            memberName = it
                            if (it.isNotBlank()) errorMessage = null
                        },
                        label = { Text("Patient Full Name *") },
                        placeholder = { Text("e.g. Arthur Miller or Robert") },
                        isError = hasError && memberName.isBlank(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_family_patient_name")
                    )

                    Text("Relationship to Caregiver:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relationOptions.take(4).forEach { rel ->
                            val isSel = memberRelation == rel
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MedTealPrimary else MedTealBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { memberRelation = rel }
                            ) {
                                Text(
                                    text = rel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MedTextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relationOptions.drop(4).forEach { rel ->
                            val isSel = memberRelation == rel
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MedTealPrimary else MedTealBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { memberRelation = rel }
                            ) {
                                Text(
                                    text = rel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MedTextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = memberPatientPhone,
                        onValueChange = {
                            memberPatientPhone = it
                            if (it.isNotBlank()) errorMessage = null
                        },
                        label = { Text("Patient Phone Number (For SMS Verification) *") },
                        placeholder = { Text("e.g. +1 (555) 432-9876") },
                        singleLine = true,
                        isError = hasError && memberPatientPhone.isBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("add_family_patient_phone")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = memberAge,
                            onValueChange = { memberAge = it },
                            label = { Text("Age") },
                            placeholder = { Text("e.g. 62") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = memberBloodType,
                            onValueChange = { memberBloodType = it },
                            label = { Text("Blood Group") },
                            placeholder = { Text("e.g. B+") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = memberAllergies,
                        onValueChange = { memberAllergies = it },
                        label = { Text("Allergies / Special Notes") },
                        placeholder = { Text("e.g. Sulfa, Penicillin, or None") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MedDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memberName.isBlank()) {
                            hasError = true
                            errorMessage = "Please enter the patient's full name."
                        } else if (memberPatientPhone.isBlank()) {
                            hasError = true
                            errorMessage = "Patient phone number is required for security verification."
                        } else {
                            val blood = if (memberBloodType.isNotBlank()) memberBloodType.trim() else "Not Specified"
                            val allergies = if (memberAllergies.isNotBlank()) memberAllergies.trim() else "None reported"
                            onAddFamilyMember(
                                memberName.trim(),
                                memberRelation,
                                memberAge.trim(),
                                blood,
                                allergies,
                                memberPatientPhone.trim()
                            )
                            showAddFamilyDialog = false
                            feedbackSnackbarMessage = "Created profile for ${memberName.trim()}. Waiting for patient verification."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add & Request Verification", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFamilyDialog = false }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
        )
    }

    // Modal Dialog: Remove Family Member Confirmation
    val targetMemberToRemove = memberToRemove
    if (targetMemberToRemove != null) {
        val target = targetMemberToRemove
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MedDanger)
                    Text("Remove Family Member?", fontWeight = FontWeight.Bold, color = MedTextPrimary)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${target.name} (${target.relationship}) from your family care circle? This member will no longer appear in the top profile switcher.",
                    fontSize = 14.sp,
                    color = MedTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveFamilyMember(target.id)
                        feedbackSnackbarMessage = "Removed ${target.name} from family care circle."
                        memberToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedDanger),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Remove Member", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AccountDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MedTealBg.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MedTealPrimary, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 12.sp, color = MedTextSecondary, fontWeight = FontWeight.Medium)
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MedTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FamilyMemberItemCard(
    member: FamilyMember,
    onRemove: () -> Unit,
    onVerify: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Avatar + Name & Relation Tag on left, Delete Icon Button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (member.isPrimary) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = member.initial,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = member.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (member.isPrimary) MaterialTheme.colorScheme.primaryContainer else Color(0xFFDBEAFE)
                        ) {
                            Text(
                                text = if (member.isPrimary) "Primary / Self" else member.relationship,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (member.isPrimary) MaterialTheme.colorScheme.primary else Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Delete button at end of first row (triggers removal confirmation)
                if (!member.isPrimary) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2))
                            .testTag("remove_family_member_${member.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Member",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text("Active Primary", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), softWrap = false)
                        }
                    }
                }
            }

            // Row 2: Phone number & age (can wrap cleanly, fully visible phone)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (member.patientPhone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = member.patientPhone,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val extraDetails = buildList {
                    if (member.ageOrBirth.isNotBlank()) add("Age ${member.ageOrBirth}")
                    if (member.bloodType.isNotBlank()) add("Blood ${member.bloodType}")
                    if (member.allergies.isNotBlank() && member.allergies != "None") add("Allergies: ${member.allergies}")
                }.joinToString(" • ")

                if (extraDetails.isNotBlank()) {
                    Text(
                        text = extraDetails,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Row 3: Status line
            if (!member.isPrimary) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (member.isVerified) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (member.isVerified) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (member.isVerified) Color(0xFF166534) else Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (member.isVerified) "Verified Device • Full Schedule Access" else "Waiting for Verification",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (member.isVerified) Color(0xFF166534) else Color(0xFF92400E)
                        )
                    }
                }
            }

            // Row 4: Action row at bottom
            if (!member.isPrimary && member.isPending && onVerify != null) {
                Button(
                    onClick = onVerify,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("verify_code_btn_${member.id}")
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verify Code / Resend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MedTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MedTextSecondary,
                lineHeight = 15.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun ThemeAndAppearanceCard(
    appPreferences: AppPreferences,
    onUpdatePreferences: (AppPreferences) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(2.dp, RoundedCornerShape(22.dp))
            .testTag("theme_and_appearance_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Appearance & Text Scaling",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize Medtrack font scaling and color accents",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Text Size Scaling Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Text Size Scaling",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Scales text size dynamically without altering color contrast",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "SMALL" to "Small",
                        "NORMAL" to "Normal",
                        "LARGE" to "Large",
                        "EXTRA_LARGE" to "Extra Large"
                    ).forEach { (sizeKey, label) ->
                        val isSelected = appPreferences.textSize.equals(sizeKey, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onUpdatePreferences(appPreferences.copy(textSize = sizeKey))
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Interactive Theme & Color Palette Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val activePalette = MedPalette.fromId(appPreferences.colorPalette)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Color Palette & Theme",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to switch app accent & styling",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "Active: ${activePalette.displayName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                softWrap = false
                            )
                        }
                    }
                }

                // Grid of Theme Palette Options with correct preview for active theme mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val paletteChunks = MedPalette.entries.chunked(2)
                    paletteChunks.forEach { rowPalettes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPalettes.forEach { palette ->
                                val isSelected = activePalette == palette
                                val previewPrimary = palette.primary
                                val previewContainer = palette.primaryContainer

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) previewContainer else MaterialTheme.colorScheme.surface,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (isSelected) previewPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onUpdatePreferences(appPreferences.copy(colorPalette = palette.id))
                                        }
                                        .testTag("palette_select_${palette.id.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(previewPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = palette.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isSelected) previewPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = when (palette) {
                                                    MedPalette.ROYAL -> "Sapphire Blue"
                                                    MedPalette.TEAL -> "Clinical Teal"
                                                    MedPalette.NAVY -> "Midnight Navy"
                                                    MedPalette.MINT -> "Botanical Mint"
                                                    MedPalette.SKY -> "Sky Blue"
                                                    MedPalette.CORAL -> "Sunset Rose"
                                                },
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
