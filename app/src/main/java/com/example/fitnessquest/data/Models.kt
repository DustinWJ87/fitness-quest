package com.example.fitnessquest.data

import kotlinx.serialization.Serializable
import java.time.LocalDate


@Serializable
enum class QuestType { POOL, VR, DUMBBELL, JUGS, STRETCH, WALK, BREATH }


@Serializable
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val xp: Int = 10,
    val type: QuestType,
    val targetCount: Int = 1,
    val unitLabel: String = "x"
)


@Serializable
data class QuestProgress(
    val questId: String,
    val completed: Boolean = false,
    val progress: Int = 0
)


@Serializable
data class MealEntry(
    val id: String,
    val dateIso: String, // ISO_LOCAL_DATE
    val name: String,
    val calories: Int
)


@Serializable
data class Reward(
    val levelRequired: Int,
    val name: String,
    val description: String,
    val claimed: Boolean = false
)


@Serializable
data class BossState(
    val name: String = "Karen the Calorie Queen",
    val stage: Int = 0 // 0..4 (4 = defeated)
)


@Serializable
data class Settings(
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val dailyCalorieTarget: Int = 2500
)


@Serializable
data class UIState(
    val level: Int = 1,
    val xp: Int = 0,
    val dailyDate: String = LocalDate.now().toString(),
    val questsToday: List<Quest> = emptyList(),
    val questProgress: Map<String, QuestProgress> = emptyMap(),
    val weekStart: String = weekStartFor(LocalDate.now()),
    val weeklyGoal: Int = 20,
    val weeklyProgress: Int = 0,
    val rewards: List<Reward> = defaultRewards(),
    val streakDays: Int = 0,
    val meals: List<MealEntry> = emptyList(),
    val boss: BossState = BossState(),
    val settings: Settings = Settings()
)


fun defaultRewards(): List<Reward> = listOf(
    Reward(2, "Snack Ticket", "Pick a favorite low-cal treat tonight"),
    Reward(3, "VR Loot", "Buy a new VR song pack or skin"),
    Reward(5, "Gear Upgrade", "New swim cap or dumbbell"),
    Reward(7, "Game Night", "Start a new PC game"),
    Reward(10, "Epic Loot", "Bigger reward of your choice")
)


fun weekStartFor(date: LocalDate) = date.with(java.time.DayOfWeek.MONDAY).toString()