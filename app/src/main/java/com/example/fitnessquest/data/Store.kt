package com.example.fitnessquest.data

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey // For APP_JSON_KEY
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// Define DataStore for Context
val Context.dataStore by preferencesDataStore(name = "fitness_prefs")

// Define Keys for XP and Quests using intPreferencesKey
private val XP_KEY = intPreferencesKey("xp") // Changed to intPreferencesKey
private val QUESTS_KEY = intPreferencesKey("quests") // Changed to intPreferencesKey

object Store {
    // Functions remain the same as they already handle Int
    fun getXP(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[XP_KEY] ?: 0 }

    suspend fun saveXP(context: Context, xp: Int) {
        context.dataStore.edit { prefs -> prefs[XP_KEY] = xp }
    }

    fun getQuests(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[QUESTS_KEY] ?: 0 }

    suspend fun saveQuests(context: Context, quests: Int) {
        context.dataStore.edit { prefs -> prefs[QUESTS_KEY] = quests }
    }
}

// Additional App-wide storage using DataStore
private val Application.dataStore by preferencesDataStore(name = "questfit2") // Note: This is a different DataStore instance name
object Keys {
    // This key is for the serialized UIState, so it should be stringPreferencesKey
    val APP = stringPreferencesKey("app_json")
}

private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    // This AppViewModel uses the "questfit2" DataStore instance
    private val ds = app.dataStore // Uses Application.dataStore ("questfit2")

    var ui: UIState = UIState()
        private set

    init { viewModelScope.launch { loadOrInit() } }

    private suspend fun loadOrInit() {
        val prefs = ds.data.first()
        val saved: String? = prefs[Keys.APP] // Reads from "app_json"
        ui = if (saved.isNullOrBlank()) UIState().ensureDaily()
        else runCatching { json.decodeFromString(UIState.serializer(), saved) }.getOrElse { UIState() }.ensureDaily()
        persist()
    }

    private fun UIState.ensureDaily(): UIState {
        val today = java.time.LocalDate.now().toString()
        val needNewDaily = dailyDate != today || questsToday.isEmpty()
        val needNewWeek = weekStart != weekStartFor(java.time.LocalDate.now())
        val initQuests = if (needNewDaily) generateDailyQuests() else questsToday
        val initProg = if (needNewDaily) initQuests.associate { it.id to QuestProgress(it.id) } else questProgress
        val newWeeklyProgress = if (needNewWeek) 0 else weeklyProgress
        val newBoss = boss.copy(stage = computeBossStage(newWeeklyProgress, weeklyGoal))
        return copy(
            dailyDate = today,
            questsToday = initQuests,
            questProgress = initProg,
            weekStart = if (needNewWeek) weekStartFor(java.time.LocalDate.now()) else weekStart,
            weeklyProgress = newWeeklyProgress,
            boss = newBoss
        )
    }

    private suspend fun persist() {
        val str = json.encodeToString(UIState.serializer(), ui)
        ds.edit { it[Keys.APP] = str } // Writes to "app_json"
    }

    // ── Mutations ────────────────────────────────────────────
    fun completeQuest(questId: String) {
        val q = ui.questsToday.firstOrNull { it.id == questId } ?: return
        val qp = ui.questProgress[questId] ?: QuestProgress(questId)
        if (qp.completed) return
        val newProg = (qp.progress + 1).coerceAtMost(q.targetCount)
        val done = newProg >= q.targetCount
        val updated = ui.questProgress.toMutableMap().apply { put(questId, qp.copy(progress = newProg, completed = done)) }

        var xp = ui.xp
        var lvl = ui.level
        var week = ui.weeklyProgress
        if (done) { xp += q.xp; week += 1 }
        while (xp >= lvl * 200) { xp -= lvl * 200; lvl++ }
        val stage = computeBossStage(week, ui.weeklyGoal)
        ui = ui.copy(xp = xp, level = lvl, questProgress = updated, weeklyProgress = week, boss = ui.boss.copy(stage = stage))
        viewModelScope.launch { persist() }
    }

    fun resetDaily() {
        val qs = generateDailyQuests()
        val prog = qs.associate { it.id to QuestProgress(it.id) }
        ui = ui.copy(dailyDate = java.time.LocalDate.now().toString(), questsToday = qs, questProgress = prog)
        viewModelScope.launch { persist() }
    }

    fun resetWeek() {
        val anchor = weekStartFor(java.time.LocalDate.now())
        ui = ui.copy(weekStart = anchor, weeklyProgress = 0, boss = ui.boss.copy(stage = 0))
        viewModelScope.launch { persist() }
    }

    fun addMeal(name: String, calories: Int) {
        val entry = MealEntry(
            id = java.util.UUID.randomUUID().toString(),
            dateIso = java.time.LocalDate.now().toString(),
            name = name, calories = calories
        )
        ui = ui.copy(meals = ui.meals + entry)
        viewModelScope.launch { persist() }
    }

    fun removeMeal(id: String) {
        ui = ui.copy(meals = ui.meals.filterNot { it.id == id })
        viewModelScope.launch { persist() }
    }

    fun toggleNotifications() {
        ui = ui.copy(settings = ui.settings.copy(notificationsEnabled = !ui.settings.notificationsEnabled))
        viewModelScope.launch { persist() }
    }
}

fun computeBossStage(progress: Int, goal: Int): Int {
    if (goal <= 0) return 0
    val ratio = progress.toFloat() / goal.toFloat()
    return when {
        ratio >= 1f -> 4
        ratio >= 0.75f -> 3
        ratio >= 0.5f -> 2
        ratio >= 0.25f -> 1
        else -> 0
    }
}
