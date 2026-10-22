package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Achievement
import com.example.data.model.AchievementIconType
import com.example.data.model.StreakRewardInfo
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    streakInfo: StreakRewardInfo?,
    achievements: List<Achievement>,
    onTriggerCelebration: (Achievement) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("All", "Unlocked", "Streak", "Milestones", "Supply", "Health")

    val filteredAchievements = remember(achievements, selectedCategory, searchQuery) {
        achievements.filter { ach ->
            val matchesCategory = when (selectedCategory) {
                "Unlocked" -> ach.isUnlocked
                "All" -> true
                else -> ach.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isEmpty() ||
                    ach.title.contains(searchQuery, ignoreCase = true) ||
                    ach.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val unlockedCount = remember(achievements) { achievements.count { it.isUnlocked } }
    val totalCount = achievements.size

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("achievements_screen_root"),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (onBackClick != null) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MedTextPrimary
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7))
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Streaks & Achievements",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MedTextPrimary
                            )
                            Text(
                                text = "$unlockedCount of $totalCount Milestones Unlocked",
                                fontSize = 11.sp,
                                color = MedTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search achievements...", fontSize = 12.sp, color = MedTextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedTextSecondary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = MedTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MedTealPrimary,
                            unfocusedBorderColor = MedBorderTeal,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MedBgGray),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Adherence Streak & Points Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp))
                        .testTag("streaks_summary_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFF2563EB)))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title + Tier Tag
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
                                    shape = CircleShape,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "${streakInfo?.currentStreak ?: 0} Day Active Streak",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MedTextPrimary
                                    )
                                    Text(
                                        text = "Tier: ${streakInfo?.streakLevel ?: "Bronze Flame"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }

                            // Points Counter Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFEFF6FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${streakInfo?.rewardPoints ?: 0} Pts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }
                        }

                        // Stat Row: Longest Streak & Total Doses
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Longest Record", fontSize = 10.sp, color = MedTextSecondary)
                                    Text(
                                        text = "${streakInfo?.longestStreak ?: 0} Days",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MedTextPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("On-Time Doses", fontSize = 10.sp, color = MedTextSecondary)
                                    Text(
                                        text = "${streakInfo?.totalOnTimeDoses ?: 0} Doses",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MedTextPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Streak Shield", fontSize = 10.sp, color = MedTextSecondary)
                                    Text(
                                        text = if (streakInfo?.isShieldActive == true) "Active" else "Used",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (streakInfo?.isShieldActive == true) Color(0xFF166534) else MedTextSecondary
                                    )
                                }
                            }
                        }

                        // 7-Day Streak Roadmap Dots
                        if (streakInfo != null && streakInfo.weekStatus.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "7-Day Adherence Roadmap:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MedTextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    streakInfo.weekStatus.forEach { day ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = day.dayLabel,
                                                fontSize = 9.sp,
                                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (day.isToday) Color(0xFF2563EB) else MedTextSecondary
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            day.isCompleted -> Color(0xFFDCFCE7)
                                                            day.isToday -> Color(0xFFFEF3C7)
                                                            else -> Color(0xFFF1F5F9)
                                                        }
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = when {
                                                            day.isCompleted -> Color(0xFF22C55E)
                                                            day.isToday -> Color(0xFFF59E0B)
                                                            else -> Color(0xFFCBD5E1)
                                                        },
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (day.isCompleted) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF166534),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                } else if (day.isToday) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = Color(0xFFB45309),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Progress to Next Milestone
                        if (streakInfo != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Progress to ${streakInfo.nextMilestoneDays}-Day Goal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MedTextSecondary
                                    )
                                    Text(
                                        text = "${streakInfo.currentStreak} / ${streakInfo.nextMilestoneDays} Days",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTealPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { streakInfo.progressToNextMilestone },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MedTealPrimary,
                                    trackColor = MedTealSoft
                                )
                            }
                        }
                    }
                }
            }

            // Filter Category Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = (selectedCategory == category)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MedTealPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = MedTextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MedTealPrimary else MedBorderTeal
                            )
                        )
                    }
                }
            }

            // Achievement Items List
            if (filteredAchievements.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = MedTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No achievements match filter",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Text(
                            text = "Try clearing search or switching categories",
                            fontSize = 12.sp,
                            color = MedTextSecondary
                        )
                    }
                }
            } else {
                items(filteredAchievements, key = { it.id }) { ach ->
                    AchievementCardItem(
                        achievement = ach,
                        onClick = {
                            if (ach.isUnlocked) {
                                onTriggerCelebration(ach)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementCardItem(
    achievement: Achievement,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val unlockedDateStr = remember(achievement.unlockedAtTimestamp) {
        achievement.unlockedAtTimestamp?.let { dateFormat.format(Date(it)) } ?: "In Progress"
    }

    val isComplete = achievement.isUnlocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isComplete, onClick = onClick)
            .testTag("achievement_card_${achievement.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) Color.White else Color(0xFFFAFAFA)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isComplete) 1.5.dp else 1.dp,
            color = if (isComplete) Color(0xFF22C55E) else MedBorderTeal
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isComplete) {
                            when (achievement.rewardBadge) {
                                "Gold" -> Color(0xFFFEF3C7)
                                "Diamond" -> Color(0xFFE0F2FE)
                                else -> Color(0xFFDCFCE7)
                            }
                        } else Color(0xFFF1F5F9)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isComplete) {
                            when (achievement.rewardBadge) {
                                "Gold" -> Color(0xFFF59E0B)
                                "Diamond" -> Color(0xFF0284C7)
                                else -> Color(0xFF22C55E)
                            }
                        } else Color(0xFFCBD5E1),
                        shape = CircleShape
                    )
            ) {
                val vectorIcon = when (achievement.iconType) {
                    AchievementIconType.WEEK_WARRIOR -> Icons.Default.Bolt
                    AchievementIconType.THIRTY_DOSES -> Icons.Default.Medication
                    AchievementIconType.FIRST_REFILL -> Icons.Default.Inventory2
                    AchievementIconType.PERFECT_DAY -> Icons.Default.CheckCircle
                    AchievementIconType.EARLY_BIRD -> Icons.Default.WbSunny
                    AchievementIconType.CONSISTENCY_14 -> Icons.Default.Star
                    AchievementIconType.HEALTH_DIARIST -> Icons.Default.MilitaryTech
                }

                Icon(
                    imageVector = vectorIcon,
                    contentDescription = null,
                    tint = if (isComplete) {
                        when (achievement.rewardBadge) {
                            "Gold" -> Color(0xFFB45309)
                            "Diamond" -> Color(0xFF0369A1)
                            else -> Color(0xFF166534)
                        }
                    } else Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Info Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = achievement.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isComplete) MedTextPrimary else Color(0xFF64748B)
                    )

                    // Points Tag
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isComplete) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "+${achievement.rewardPoints} Pts",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) Color(0xFFB45309) else MedTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = achievement.description,
                    fontSize = 11.5.sp,
                    color = MedTextSecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar or Unlocked Tag
                if (isComplete) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Unlocked ($unlockedDateStr)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Tap to replay 🎉",
                            fontSize = 10.sp,
                            color = MedTealPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    val progressFraction = (achievement.currentProgress.toFloat() / achievement.maxProgress.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress",
                                fontSize = 10.sp,
                                color = MedTextSecondary
                            )
                            Text(
                                text = "${achievement.currentProgress} / ${achievement.maxProgress}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MedTealPrimary,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}
