package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

/**
 * Common Top Bar / Header for Auth and Onboarding Screens.
 */
@Composable
fun AuthHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    currentStep: Int? = null,
    totalSteps: Int = 6,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .testTag("auth_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MedLightTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(42.dp))
            }

            // Medtrack Brand Pill Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MedtrackMint.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedtrackMint.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = null,
                        tint = MedtrackMint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Medtrack",
                        color = Color(0xFF0F766E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Step Indicator badge if applicable
            if (currentStep != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "$currentStep of $totalSteps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTealPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(42.dp))
            }
        }

        if (currentStep != null) {
            Spacer(modifier = Modifier.height(16.dp))
            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_step_progress_bar"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (step in 1..totalSteps) {
                    val isActive = step <= currentStep
                    val isCurrent = step == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    isCurrent -> MedTealPrimary
                                    isActive -> MedtrackMint
                                    else -> Color(0xFFE2E8F0)
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MedLightTextPrimary,
            letterSpacing = (-0.3).sp,
            lineHeight = 30.sp,
            modifier = Modifier.testTag("auth_header_title")
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MedLightTextSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.testTag("auth_header_subtitle")
            )
        }
    }
}

/**
 * Standard text field with modern styling, icons, and error handling.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    testTag: String = "auth_text_field"
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (errorMessage != null) Color(0xFFDC2626) else MedLightTextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(text = placeholder, color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            },
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (errorMessage != null) Color(0xFFDC2626) else MedTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            trailingIcon = if (isPassword) {
                {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.testTag("${testTag}_toggle_password")
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            isError = errorMessage != null,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color(0xFFFEF2F2),
                focusedBorderColor = MedTealPrimary,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                errorBorderColor = Color(0xFFDC2626),
                focusedTextColor = MedLightTextPrimary,
                unfocusedTextColor = MedLightTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        if (errorMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("${testTag}_error")
                )
            }
        }
    }
}

/**
 * Standard Prominent Primary Button with Loading State.
 */
@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    testTag: String = "auth_primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MedTealPrimary,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFCBD5E1),
            disabledContentColor = Color(0xFF94A3B8)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
        contentPadding = PaddingValues(vertical = 15.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Processing...",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

/**
 * Secondary Outlined Button.
 */
@Composable
fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    testTag: String = "auth_secondary_button"
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MedLightTextPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MedTealPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Checklist item showing password requirements validation.
 */
@Composable
fun PasswordRequirementRow(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) MedtrackMint else Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isMet) Color(0xFF0F766E) else Color(0xFF64748B),
            fontWeight = if (isMet) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Custom vector hero graphic matching Medtrack's branding and color scheme.
 */
@Composable
fun HealthcareHeroIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background circular gradient
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MedtrackMint.copy(alpha = 0.16f),
                            MedTealPrimary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Pulse wave canvas background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height * 0.52f

            // Pulse wave line
            val path = Path().apply {
                moveTo(width * 0.08f, centerY)
                lineTo(width * 0.32f, centerY)
                lineTo(width * 0.38f, centerY - 28f)
                lineTo(width * 0.44f, centerY + 32f)
                lineTo(width * 0.50f, centerY - 45f)
                lineTo(width * 0.56f, centerY + 18f)
                lineTo(width * 0.62f, centerY)
                lineTo(width * 0.92f, centerY)
            }

            drawPath(
                path = path,
                color = MedtrackMint.copy(alpha = 0.35f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
        }

        // Central Card: Medtrack 3D Capsule Pill Graphic
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 20.dp)
            ) {
                // The Signature Capsule
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFF0F172A))
                        .border(2.5.dp, MedtrackMint, RoundedCornerShape(percent = 50))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left half
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Medtrack",
                                color = MedtrackMint,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // Right half
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                                .background(MedtrackMint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature pills row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureMiniChip(
                        icon = Icons.Default.Alarm,
                        label = "Reminders",
                        tint = MedTealPrimary,
                        bg = MedTealSoft
                    )
                    FeatureMiniChip(
                        icon = Icons.Default.Favorite,
                        label = "Health Log",
                        tint = Color(0xFFDC2626),
                        bg = Color(0xFFFEF2F2)
                    )
                    FeatureMiniChip(
                        icon = Icons.Default.Shield,
                        label = "Private",
                        tint = Color(0xFF0F766E),
                        bg = MedtrackMint.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureMiniChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}

/**
 * Modal dialog presenting Terms of Service and Privacy Policy.
 */
@Composable
fun TermsAndPrivacyDialog(
    initialTab: String = "terms", // "terms" or "privacy"
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (selectedTab == "terms") "Terms of Service" else "Privacy Policy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedLightTextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == "terms") Color.White else Color.Transparent)
                            .clickable { selectedTab = "terms" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Terms of Service",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == "terms") FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == "terms") MedTealPrimary else Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == "privacy") Color.White else Color.Transparent)
                            .clickable { selectedTab = "privacy" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Privacy Policy",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == "privacy") FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == "privacy") MedTealPrimary else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == "terms") {
                        TermsContent()
                    } else {
                        PrivacyContent()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I Understand & Agree", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TermsContent() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "1. Medical Information Disclaimer",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MedLightTextPrimary
        )
        Text(
            text = "Medtrack is a personal medication tracking and wellness reminder tool. Medtrack is not a doctor, licensed hospital, or pharmacy. The reminders, interaction checks, and insights provided are for informational convenience only and must not replace professional clinical advice or emergency medical care.",
            fontSize = 13.sp,
            color = MedLightTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Text(
            text = "2. User Responsibility & Dosages",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MedLightTextPrimary
        )
        Text(
            text = "You are solely responsible for verifying that your medication names, dosages, timings, and prescription schedules correspond accurately to your physician's prescriptions and pharmacy labels before taking any medication.",
            fontSize = 13.sp,
            color = MedLightTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Text(
            text = "3. Age Requirement",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MedLightTextPrimary
        )
        Text(
            text = "Primary caregiver accounts on Medtrack require users to be at least 18 years of age. Accounts may be created by parents or authorized caregivers to manage dependents.",
            fontSize = 13.sp,
            color = MedLightTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Text(
            text = "4. Account Security",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MedLightTextPrimary
        )
        Text(
            text = "You agree to maintain a secure password and safeguard your login credentials. You may terminate your account or export your health data at any time from the Settings screen.",
            fontSize = 13.sp,
            color = MedLightTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun PrivacyContent() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "1. Zero Health Data Selling",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MedLightTextPrimary
        )
        Text(
            text = "We never sell, rent, or monetize your personal health information, prescription history, or diary logs to advertisers, data brokers, or insurance companies.",
            fontSize = 13.sp,
            color = MedLightTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Text(
            text = "2. Encryption & HIPAA Standard Architecture",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MedLightTextPrimary
        )
        Text(
            text = "Your account credentials and sensitive health data are protected using cryptographic hashing and TLS 1.3 encryption in transit. Passwords are never stored or logged in plain text.",
            fontSize = 13.sp,
            color = MedLightTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Text(
            text = "3. Local & Cloud Storage Choice",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MedLightTextPrimary
        )
        Text(
            text = "Medtrack supports resilient on-device caching via Room database alongside authenticated cloud sync. When you log out, active tokens and session headers are securely purged.",
            fontSize = 13.sp,
            color = MedLightTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Text(
            text = "4. Right to Deletion",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MedLightTextPrimary
        )
        Text(
            text = "You have full control over your data. You may request complete account deletion directly from the Settings screen at any time.",
            fontSize = 13.sp,
            color = MedLightTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
    }
}
