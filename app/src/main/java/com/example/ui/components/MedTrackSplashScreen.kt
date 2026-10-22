package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MedicoMint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 2.0s opening logo animation screen.
 * Displays an animated Medtrack capsule logo centered on a sleek black/charcoal background
 * with "Medtrack" in mint green and a subtle tagline.
 */
@Composable
fun MedTrackSplashScreen(
    onAnimationFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation drivers: scale, alpha, pulse, and text offset
    val logoScale = remember { Animatable(0.25f) }
    val logoAlpha = remember { Animatable(0f) }
    val glowScale = remember { Animatable(0.8f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }

    // Total duration: 2.0 seconds (2000ms)
    LaunchedEffect(Unit) {
        // Logo entrance: smooth spring bounce and fade in
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1.05f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 250)
            )
        }
        launch {
            glowScale.animateTo(
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // Medtrack title text animation: fades in and slides up
        launch {
            delay(250L)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(250L)
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // Wait for 2000ms (2.0 seconds), then transition to main app
        delay(2000L)
        onAnimationFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Immediate skip if tapped
                onAnimationFinish()
            }
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Centered Logo
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .testTag("splash_logo"),
                contentAlignment = Alignment.Center
            ) {
                // Glowing background aura
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(glowScale.value)
                        .clip(CircleShape)
                        .background(MedicoMint.copy(alpha = 0.12f))
                )

                // Capsule Pill Logo
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(60.dp)
                        .shadow(16.dp, RoundedCornerShape(percent = 50))
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFF121212))
                        .border(2.5.dp, MedicoMint, RoundedCornerShape(percent = 50))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left half: Dark Surface with Medtrack text in mint green
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .background(Color(0xFF121212)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Medtrack",
                                color = MedicoMint,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // Right half: Mint Green with Medical Plus Symbol
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                                .background(MedicoMint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Medtrack Plus",
                                tint = Color(0xFF000000),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "Medtrack" written below logo
            Text(
                text = "Medtrack",
                color = MedicoMint,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp,
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = textOffsetY.value.toInt()) }
                    .alpha(textAlpha.value)
                    .testTag("splash_medtrack_text")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Smart Health & Medication Companion",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = textOffsetY.value.toInt()) }
                    .alpha(textAlpha.value * 0.85f)
            )
        }
    }
}
