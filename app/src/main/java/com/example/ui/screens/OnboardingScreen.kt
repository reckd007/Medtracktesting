package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val badge: String,
    val isFirstStep: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit,
    onAddFirstMedication: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        OnboardingStep(
            title = "Welcome to MedTrack",
            subtitle = "Your Smart Healthcare & Medication Companion",
            description = "MedTrack helps you and your family manage medications effortlessly, never miss a dose, and keep all vital health records secure in one place. Start by adding your first medication to get personalized schedules.",
            icon = Icons.Default.MedicalServices,
            badge = "Step 1 of 4",
            isFirstStep = true
        ),
        OnboardingStep(
            title = "Never Miss a Dose",
            subtitle = "Smart Reminders & Alarm Manager",
            description = "Set reliable reminders that alert you even when the app is closed. Customize alert sounds, vibration patterns, track refill inventory thresholds, and rotate injection sites easily.",
            icon = Icons.Default.Alarm,
            badge = "Step 2 of 4"
        ),
        OnboardingStep(
            title = "Track Health & Vitals",
            subtitle = "Comprehensive Daily Health Diary",
            description = "Log your daily vitals including blood pressure, blood sugar, weight, mood, pain levels, and symptoms. Visualize your health trends over time with clean charts and logs.",
            icon = Icons.Default.Favorite,
            badge = "Step 3 of 4"
        ),
        OnboardingStep(
            title = "MedTrack AI & Family Care",
            subtitle = "Intelligent Guidance & Shared Profiles",
            description = "Ask MedTrack AI questions about dosage timing, food interactions, and storage guidelines. Create family care profiles to manage loved ones safely and securely.",
            icon = Icons.Default.AutoAwesome,
            badge = "Step 4 of 4"
        )
    )

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen_root"),
        containerColor = MedTealBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MedTealPrimary)
                    )
                    Text(
                        text = "MedTrack",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                }

                if (pagerState.currentPage < steps.size - 1) {
                    TextButton(
                        onClick = onCompleteOnboarding,
                        modifier = Modifier.testTag("btn_skip_onboarding")
                    ) {
                        Text(
                            text = "Skip",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MedTextSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Step Badge Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MedTealSoft,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = step.badge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTealDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Hero Icon Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(MedTealSoft)
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = MedTealPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = step.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MedTealPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = step.description,
                        fontSize = 13.sp,
                        color = MedTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    if (step.isFirstStep) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                onCompleteOnboarding()
                                onAddFirstMedication()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("btn_onboarding_add_first_med")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add First Medication Now",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Indicators and Navigation Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Page Indicator Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(steps.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateFloatAsState(targetValue = if (isSelected) 24f else 8f, label = "dotWidth")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) MedTealPrimary else MedBorderTeal)
                        )
                    }
                }

                // Next / Get Started Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MedBorderTeal),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_onboarding_back")
                        ) {
                            Text(
                                text = "Back",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < steps.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onCompleteOnboarding()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                        modifier = Modifier
                            .weight(if (pagerState.currentPage > 0) 1f else 2f)
                            .height(52.dp)
                            .testTag("btn_onboarding_next")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (pagerState.currentPage < steps.size - 1) "Next" else "Get Started",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (pagerState.currentPage < steps.size - 1) {
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
        }
    }
}
