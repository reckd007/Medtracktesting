package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * Requirement 2: Welcoming first screen ("Get Started" screen).
 * Displays the selected caregiver & patient photo as a warm, translucent edge-to-edge background
 * with a soft white overlay (25-35% opacity) preserving natural skin tones while ensuring
 * crystal-clear legibility for the headline, supporting copy, and action buttons.
 *
 * Exclusively used on this initial Get Started / Welcome screen.
 */
@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTermsAndPrivacyDialog by remember { mutableStateOf(false) }
    var dialogInitialTab by remember { mutableStateOf("terms") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("welcome_screen_container")
    ) {
        // 1. Edge-to-edge full-bleed caregiver photo background
        Image(
            painter = painterResource(id = R.drawable.welcome_caregiver_bg),
            contentDescription = "Caregiver assisting smiling elderly patient",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Soft translucent white overlay (25-35% opacity)
        // Softens background and preserves warm skin tones while ensuring foreground contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f), // 30% top wash for brand badge
                            Color.White.copy(alpha = 0.25f), // 25% over faces, allowing them to remain clearly visible
                            Color.White.copy(alpha = 0.40f), // 40% near the lower text section
                            Color.White.copy(alpha = 0.55f)  // 55% at the button row for crisp contrast
                        )
                    )
                )
        )

        // 3. Foreground content respecting safe areas
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Brand Anchor Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.94f),
                        shadowElevation = 3.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(MedtrackMint, shape = RoundedCornerShape(5.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MEDTRACK HEALTH COMPANION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Generous vertical spacer to let the warm caregiver & patient photo shine through
                Spacer(modifier = Modifier.height(180.dp))

                // Bottom Content: Headline, Supporting Text, and Action Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Headline and Supporting Copy Card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.88f),
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.9f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            Text(
                                text = "Take control of your health.",
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MedLightTextPrimary,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-0.5).sp,
                                lineHeight = 33.sp,
                                modifier = Modifier.testTag("welcome_headline")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Stay on top of your medications with timely reminders and simple tracking.",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = MedLightTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .testTag("welcome_supporting_text")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary: Get Started Button
                    AuthPrimaryButton(
                        text = "Get Started",
                        onClick = onGetStartedClick,
                        leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                        testTag = "welcome_get_started_button"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary: Log In Button
                    AuthSecondaryButton(
                        text = "Log In",
                        onClick = onLoginClick,
                        leadingIcon = Icons.Default.Lock,
                        testTag = "welcome_login_button"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Clickable Terms of Service and Privacy Policy
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "By continuing, you agree to our ",
                            fontSize = 12.sp,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Terms of Service",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTealPrimary,
                            modifier = Modifier
                                .clickable {
                                    dialogInitialTab = "terms"
                                    showTermsAndPrivacyDialog = true
                                }
                                .testTag("welcome_terms_link")
                        )

                        Text(
                            text = " and ",
                            fontSize = 12.sp,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Privacy Policy",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTealPrimary,
                            modifier = Modifier
                                .clickable {
                                    dialogInitialTab = "privacy"
                                    showTermsAndPrivacyDialog = true
                                }
                                .testTag("welcome_privacy_link")
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showTermsAndPrivacyDialog) {
        TermsAndPrivacyDialog(
            initialTab = dialogInitialTab,
            onDismiss = { showTermsAndPrivacyDialog = false }
        )
    }
}
