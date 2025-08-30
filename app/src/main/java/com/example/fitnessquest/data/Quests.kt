package com.example.fitnessquest.data

fun generateDailyQuests(): List<Quest> {
    val pool = listOf(
        Quest("pool_walk_5", "Pool Walk", "Walk laps in pool — 5 min", 10, QuestType.POOL, targetCount = 5, unitLabel = "min"),
        Quest("pool_kicks_3", "Wall Kicks", "Hold wall & flutter — 3 min", 10, QuestType.POOL, targetCount = 3, unitLabel = "min")
    )
    val vr = listOf(
        Quest("vr_box_3", "VR Boxing", "Light shadow boxing — 3 min", 10, QuestType.VR, targetCount = 3, unitLabel = "min"),
        Quest("vr_rhythm_4", "VR Rhythm", "Rhythm game warmup — 4 min", 10, QuestType.VR, targetCount = 4, unitLabel = "min")
    )
    val dumbbells = listOf(
        Quest("db_curl_10", "Dumbbell Curls", "8 lb curls — 10/side", 10, QuestType.DUMBBELL, targetCount = 10, unitLabel = "reps"),
        Quest("db_press_10", "Dumbbell Press", "Seated press — 10 reps", 10, QuestType.DUMBBELL, targetCount = 10, unitLabel = "reps")
    )
    val stretch = listOf(
        Quest("st_torso_2", "Torso Twists", "2 x 10/side", 10, QuestType.STRETCH, targetCount = 2, unitLabel = "sets"),
        Quest("st_calf_2", "Calf Stretch", "2 x 30s", 10, QuestType.STRETCH, targetCount = 2, unitLabel = "sets")
    )
    val breath = listOf(
        Quest("breath_box_2", "Box Breathing", "4-4-4-4 — 2 min", 10, QuestType.BREATH, targetCount = 2, unitLabel = "min")
    )
    return listOf(pool.random(), vr.random(), dumbbells.random(), stretch.random(), breath.random())
}