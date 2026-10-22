package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Achievement
import com.example.data.model.AchievementIconType
import com.example.data.model.DayStreakStatus
import com.example.data.model.StreakRewardInfo
import com.example.ui.theme.MedicoMint

/**
 * Interactive Streak Rewards & Achievements Dialog displaying consecutive day streak breakdown,
 * rewards unlocked, milestones progress, and achievement cards with animation replay.
 */
@Composable
fun StreakAchievementsSheet(
    streakInfo: StreakRewardInfo,
    achievements: List<Achievement>,
    onDismiss: () -> Unit,
    onTriggerCelebration: (Achievement) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Unlocked", "In Progress"

    val filteredAchievements = remember(achievements, selectedFilter) {
        when (selectedFilter) {
            "Unlocked" -> achievements.filter { it.isUnlocked }
            "In Progress" -> achievements.filter { !it.isUnlocked }
            else -> achievements
        }
    }

    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount = achievements.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White)
                .testTag("streak_achievements_dialog"),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Streak & Achievements",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${streakInfo.currentStreak} Days Consecutive • $unlockedCount/$totalCount Milestones",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_streak_sheet_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Streak Hero Card with Flame and Rewards
                    item {
                        StreakHeroCard(
                            streakInfo = streakInfo
                        )
                    }

                    // 2. 7-Day Consecutive Streak Tracker (Mini week roadmap)
                    item {
                        WeeklyStreakRoadmapCard(
                            weekStatus = streakInfo.weekStatus,
                            streakDays = streakInfo.currentStreak
                        )
                    }

                    // 3. Filter Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "All" to "All ($totalCount)",
                                "Unlocked" to "Unlocked ($unlockedCount)",
                                "In Progress" to "In Progress (${totalCount - unlockedCount})"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedFilter == key,
                                    onClick = { selectedFilter = key },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF1D5FD6),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // 4. Milestone List
                    items(filteredAchievements, key = { it.id }) { ach ->
                        AchievementItemCard(
                            achievement = ach,
                            onPlayAnimation = { onTriggerCelebration(ach) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Streak Hero card showing current streak flame, points, and shield.
 */
@Composable
fun StreakHeroCard(
    streakInfo: StreakRewardInfo,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame_pulse")
    val flamePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.horizontalGradient(listOf(Color(0xFFFDE68A), Color(0xFF93C5FD)))
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .scale(flamePulse)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A))
                                )
                            )
                            .border(2.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "${streakInfo.currentStreak} Day Streak!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Level: ${streakInfo.streakLevel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                // Streak Shield Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.horizontalGradient(listOf(Color(0xFFBFDBFE), Color(0xFF93C5FD))))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Shield Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            }

            // Stats grid: Longest streak, on-time doses, reward points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Longest Streak", "${streakInfo.longestStreak} Days", Color(0xFFFEF3C7)),
                    Triple("On-Time Doses", "${streakInfo.totalOnTimeDoses}", Color(0xFFF0FDF4)),
                    Triple("Reward Points", "${streakInfo.rewardPoints} Pts", Color(0xFFF5F3FF))
                ).forEach { (label, value, bg) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = bg
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = label, fontSize = 10.sp, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }
            }

            // Next Milestone progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next Tier: ${streakInfo.nextMilestoneDays}-Day Streak Reward",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "${streakInfo.currentStreak}/${streakInfo.nextMilestoneDays} Days",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D5FD6)
                    )
                }

                LinearProgressIndicator(
                    progress = { streakInfo.progressToNextMilestone },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF1D5FD6),
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

/**
 * Visual 7-day consecutive adherence calendar tracker.
 */
@Composable
fun WeeklyStreakRoadmapCard(
    weekStatus: List<DayStreakStatus>,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Consecutive Days Record",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = if (streakDays >= 7) "7-Day Week Goal Met!" else "Complete all 7 days for milestone!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (streakDays >= 7) Color(0xFF16A34A) else Color(0xFFD97706)
                )
            }

            // 7 day status bubbles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekStatus.forEach { status ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = status.dayLabel,
                            fontSize = 10.sp,
                            fontWeight = if (status.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (status.isToday) Color(0xFF1D5FD6) else Color(0xFF64748B)
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        status.isCompleted -> Color(0xFFDCFCE7)
                                        status.isToday -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                )
                                .border(
                                    width = if (status.isToday) 2.dp else 1.dp,
                                    color = when {
                                        status.isCompleted -> Color(0xFF22C55E)
                                        status.isToday -> Color(0xFFF59E0B)
                                        else -> Color(0xFFCBD5E1)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (status.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (status.isToday) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "In progress",
                                    tint = Color(0xFFD97706),
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

/**
 * Individual milestone item card with progress bar, reward details, and animation replay button.
 */
@Composable
fun AchievementItemCard(
    achievement: Achievement,
    onPlayAnimation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("achievement_card_${achievement.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) Color(0xFFF8FAFC) else Color.White
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = if (achievement.isUnlocked) {
                Brush.horizontalGradient(listOf(Color(0xFF86EFAC), Color(0xFF93C5FD)))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Badge Icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (achievement.isUnlocked) {
                                    Color(0xFFFEF3C7)
                                } else {
                                    Color(0xFFF1F5F9)
                                }
                            )
                            .border(
                                1.5.dp,
                                if (achievement.isUnlocked) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                CircleShape
                            ),
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
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (achievement.isUnlocked) Color(0xFFD97706) else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = achievement.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (achievement.isUnlocked) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                            if (achievement.isUnlocked) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "UNLOCKED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = achievement.description,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )
                    }
                }

                // Reward points badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (achievement.isUnlocked) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                ) {
                    Text(
                        text = "+${achievement.rewardPoints} pts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) Color(0xFF1D5FD6) else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bar & Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val progressFraction = (achievement.currentProgress.toFloat() / achievement.maxProgress.coerceAtLeast(1)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (achievement.isUnlocked) Color(0xFF22C55E) else Color(0xFF1D5FD6),
                        trackColor = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "${achievement.currentProgress} / ${achievement.maxProgress}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                // Interactive Celebration replay button
                Button(
                    onClick = onPlayAnimation,
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("celebrate_button_${achievement.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (achievement.isUnlocked) Color(0xFF1D5FD6) else Color(0xFF64748B)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (achievement.isUnlocked) "Replay Animation" else "Preview",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
