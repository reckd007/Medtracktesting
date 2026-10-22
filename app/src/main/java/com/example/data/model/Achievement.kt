package com.example.data.model

enum class AchievementIconType {
    WEEK_WARRIOR,    // 1 Week Adherence
    THIRTY_DOSES,    // 30 Doses Taken
    FIRST_REFILL,    // First Refill
    PERFECT_DAY,     // Perfect Day
    EARLY_BIRD,      // Early Bird
    CONSISTENCY_14,  // 14-Day Streak
    HEALTH_DIARIST   // First health diary entry
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconType: AchievementIconType,
    val currentProgress: Int,
    val maxProgress: Int,
    val isUnlocked: Boolean,
    val unlockedAtTimestamp: Long? = null,
    val rewardPoints: Int = 100,
    val rewardBadge: String = "Silver", // "Bronze", "Silver", "Gold", "Diamond"
    val category: String = "Milestones" // "Streak", "Milestones", "Supply", "Health"
)

data class DayStreakStatus(
    val dayLabel: String,     // "Mon", "Tue", etc.
    val dateString: String,
    val isCompleted: Boolean,
    val isToday: Boolean
)

data class StreakRewardInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalOnTimeDoses: Int,
    val streakLevel: String, // "Bronze Flame", "Silver Shield", "Gold Champion", "Diamond Legend"
    val rewardPoints: Int,
    val nextMilestoneDays: Int,
    val progressToNextMilestone: Float,
    val isShieldActive: Boolean = true,
    val todayCompleted: Boolean = false,
    val weekStatus: List<DayStreakStatus> = emptyList()
)
