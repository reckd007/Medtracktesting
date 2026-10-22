package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Achievement
import com.example.data.model.AchievementIconType
import com.example.ui.theme.MedicoMint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val speedY: Float,
    val speedX: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

/**
 * Short celebratory animation overlay displayed when a user completes an achievement
 * such as '1 Week Adherence', '30 Doses Taken', or 'First Refill'.
 * Features lively spring-loaded badge scale, pulsating aura, confetti particle explosion,
 * and clear reward milestone information.
 */
@Composable
fun AchievementCelebrationOverlay(
    achievement: Achievement?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (achievement == null) return

    // Animation drivers
    val scaleAnim = remember { Animatable(0.2f) }
    val alphaAnim = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(1f) }

    // Continuous aura rotation
    val infiniteTransition = rememberInfiniteTransition(label = "aura_rotation")
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Confetti particles
    val particles = remember {
        val colors = listOf(
            Color(0xFF7BE0A0), // Mint
            Color(0xFFF59E0B), // Gold
            Color(0xFF1D5FD6), // Royal Blue
            Color(0xFFEF4444), // Crimson
            Color(0xFF8B5CF6), // Purple
            Color(0xFF10B981)  // Emerald
        )
        List(50) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = -Random.nextFloat() * 0.3f,
                speedY = 0.3f + Random.nextFloat() * 0.7f,
                speedX = (Random.nextFloat() - 0.5f) * 0.3f,
                size = 8f + Random.nextFloat() * 12f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = Random.nextFloat() * 360f,
                isCircle = Random.nextBoolean()
            )
        }
    }

    val confettiProgress = remember { Animatable(0f) }

    LaunchedEffect(achievement.id) {
        launch {
            alphaAnim.animateTo(1f, tween(300))
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            confettiProgress.animateTo(1f, tween(2500, easing = LinearEasing))
        }
        launch {
            progressAnim.animateTo(0f, tween(4000, easing = LinearEasing))
            delay(200)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .testTag("achievement_celebration_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // Falling confetti canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val progress = confettiProgress.value

                particles.forEach { particle ->
                    val curY = (particle.initialY + particle.speedY * progress) * canvasHeight
                    val curX = (particle.initialX + particle.speedX * progress) * canvasWidth
                    val curRot = particle.rotationSpeed * progress

                    if (curY in 0f..canvasHeight && curX in 0f..canvasWidth) {
                        rotate(curRot, pivot = Offset(curX, curY)) {
                            if (particle.isCircle) {
                                drawCircle(
                                    color = particle.color,
                                    radius = particle.size / 2,
                                    center = Offset(curX, curY)
                                )
                            } else {
                                drawRect(
                                    color = particle.color,
                                    topLeft = Offset(curX - particle.size / 2, curY - particle.size / 4),
                                    size = Size(particle.size, particle.size / 2)
                                )
                            }
                        }
                    }
                }
            }

            // Main Celebration Card with spring scale
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value)
                    .shadow(24.dp, RoundedCornerShape(28.dp))
                    .testTag("achievement_celebration_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFF59E0B), Color(0xFF7BE0A0), Color(0xFF1D5FD6))
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Milestone Tag
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ACHIEVEMENT UNLOCKED!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Rotating Radiant Aura + Trophy Badge Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(120.dp)
                    ) {
                        // Outer Pulsing Aura Ring
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(auraPulse)
                                .rotate(auraRotation)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFFD700).copy(alpha = 0.5f),
                                            Color(0xFF7BE0A0).copy(alpha = 0.5f),
                                            Color(0xFF38BDF8).copy(alpha = 0.5f),
                                            Color(0xFFFFD700).copy(alpha = 0.5f)
                                        )
                                    )
                                )
                        )

                        // Central Trophy Shield
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
                                    )
                                )
                                .border(3.dp, Color(0xFFF59E0B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (achievement.iconType) {
                                AchievementIconType.WEEK_WARRIOR -> Icons.Default.Bolt
                                AchievementIconType.THIRTY_DOSES -> Icons.Default.Medication
                                AchievementIconType.FIRST_REFILL -> Icons.Default.Inventory2
                                AchievementIconType.PERFECT_DAY -> Icons.Default.CheckCircle
                                AchievementIconType.EARLY_BIRD -> Icons.Default.WbSunny
                                AchievementIconType.CONSISTENCY_14 -> Icons.Default.Star
                                AchievementIconType.HEALTH_DIARIST -> Icons.Default.MilitaryTech
                            }
                            val iconTint = when (achievement.iconType) {
                                AchievementIconType.WEEK_WARRIOR -> Color(0xFFD97706)
                                AchievementIconType.THIRTY_DOSES -> Color(0xFF0D9488)
                                AchievementIconType.FIRST_REFILL -> Color(0xFF2563EB)
                                AchievementIconType.PERFECT_DAY -> Color(0xFF16A34A)
                                AchievementIconType.EARLY_BIRD -> Color(0xFFEA580C)
                                AchievementIconType.CONSISTENCY_14 -> Color(0xFF7C3AED)
                                AchievementIconType.HEALTH_DIARIST -> Color(0xFF0284C7)
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = achievement.title,
                                tint = iconTint,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    // Achievement Title
                    Text(
                        text = achievement.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    // Achievement Description
                    Text(
                        text = achievement.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Reward details banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(Color(0xFF86EFAC), Color(0xFF34D399)))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Reward Earned",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                            Text(
                                text = "+${achievement.rewardPoints} Streak Pts",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    // Auto-dismiss countdown visual bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnim.value)
                                .height(3.dp)
                                .background(Color(0xFF7BE0A0))
                        )
                    }

                    // Interactive Claim / Dismiss Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dismiss_achievement_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D5FD6)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Awesome! Claim Reward",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
