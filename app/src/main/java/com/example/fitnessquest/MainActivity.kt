// FitnessQuest 2.0 — All‑in‑one (Compose + DataStore + Notifications + Navigation)
// File: app/src/main/java/com/example/fitnessquest/MainActivity.kt

package com.example.fitnessquest

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ==========================
// Data models
// ==========================

enum class QuestType { POOL, VR, DUMBBELL, JUGS, STRETCH, WALK, BREATH }

@Serializable
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val xp: Int = 10,
    val type: QuestType,
    val targetCount: Int = 1,
    val unitLabel: String = "x",
    val difficulty: String = "Light" // Light, Moderate, Challenge
)

@Serializable
data class QuestProgress(
    val questId: String,
    val completed: Boolean = false,
    val progress: Int = 0
)

@Serializable
data class Reward(
    val levelRequired: Int,
    val name: String,
    val description: String,
    val claimed: Boolean = false
)

@Serializable
data class MealEntry(
    val id: String,
    val date: String, // ISO date
    val name: String,
    val calories: Int
)

@Serializable
data class DayLog(
    val date: String,
    val xpEarned: Int = 0,
    val questsCompleted: Int = 0
)

@Serializable
data class SettingsState(
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 10,
    val reminderMinute: Int = 0,
    val dailyCalorieTarget: Int = 2500,
    val darkTheme: Boolean = true
)

@Serializable
data class PlayerState(
    val level: Int = 1,
    val xp: Int = 0,
    val dailyDate: String = "",
    val questsToday: List<Quest> = emptyList(),
    val questProgressToday: Map<String, QuestProgress> = emptyMap(),
    val weekStart: String = "",
    val weeklyBossGoal: Int = 20,
    val weeklyBossProgress: Int = 0,
    val rewards: List<Reward> = defaultRewards(),
    val streakDays: Int = 0,
    val lifetimeQuests: Int = 0,
    val history7: List<DayLog> = emptyList(),
    val meals: List<MealEntry> = emptyList(),
    val settings: SettingsState = SettingsState()
)

fun defaultRewards(): List<Reward> = listOf(
    Reward(2, "Snack Ticket", "Pick a favorite low-cal treat tonight"),
    Reward(3, "VR Loot", "Buy a new VR song pack or skin"),
    Reward(5, "Gear Upgrade", "New swim cap, water shoes, or dumbbell"),
    Reward(7, "Game Night", "Buy or start a new PC game"),
    Reward(10, "Epic Loot", "Bigger reward of your choice")
)

// ==========================
// DataStore
// ==========================

private val Application.dataStore by preferencesDataStore(name = "questfit2")

object Keys {
    val PLAYER_JSON = stringPreferencesKey("player_json")
}

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// ==========================
// ViewModel
// ==========================

class FitnessViewModel(app: Application) : AndroidViewModel(app) {
    private val ds = app.dataStore

    var state by mutableStateOf(PlayerState())
        private set

    init { viewModelScope.launch { loadOrInit() } }

    private suspend fun loadOrInit() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val stored = ds.data.first()[Keys.PLAYER_JSON]
        if (stored == null) {
            // First run
            val quests = generateDailyQuests()
            val prog = quests.associate { it.id to QuestProgress(it.id) }
            state = PlayerState(
                level = 1,
                xp = 0,
                dailyDate = today,
                questsToday = quests,
                questProgressToday = prog,
                weekStart = weekStartFor(LocalDate.now()),
                weeklyBossGoal = 20,
                weeklyBossProgress = 0,
                rewards = defaultRewards(),
                streakDays = 1,
                history7 = emptyList(),
                meals = emptyList(),
                settings = SettingsState()
            )
            persist()
        } else {
            var loaded = runCatching { json.decodeFromString<PlayerState>(stored) }.getOrElse { PlayerState() }
            // New day logic
            val needNewDaily = loaded.dailyDate != today || loaded.questsToday.isEmpty()
            if (needNewDaily) {
                val qs = generateDailyQuests()
                val prog = qs.associate { it.id to QuestProgress(it.id) }
                // update streak
                val prevDate = loaded.dailyDate
                val newStreak = when {
                    prevDate.isBlank() -> 1
                    prevDate == today -> loaded.streakDays
                    LocalDate.parse(prevDate) == LocalDate.now().minusDays(1) -> loaded.streakDays + 1
                    else -> 1
                }
                loaded = loaded.copy(
                    dailyDate = today,
                    questsToday = qs,
                    questProgressToday = prog,
                    streakDays = newStreak,
                    weekStart = loaded.weekStart.ifBlank { weekStartFor(LocalDate.now()) }
                )
            }
            state = loaded
            persist()
        }
    }

    private fun weekStartFor(date: LocalDate): String =
        date.with(java.time.DayOfWeek.MONDAY).format(DateTimeFormatter.ISO_DATE)

    fun completeQuest(questId: String, incrementBy: Int = 1) {
        val qp = state.questProgressToday[questId] ?: return
        if (qp.completed) return
        val quest = state.questsToday.firstOrNull { it.id == questId } ?: return

        val newProg = (qp.progress + incrementBy).coerceAtMost(quest.targetCount)
        val done = newProg >= quest.targetCount

        val updatedMap = state.questProgressToday.toMutableMap()
        updatedMap[questId] = qp.copy(progress = newProg, completed = done)

        var gained = 0
        var weekProg = state.weeklyBossProgress
        var questsDone = 0
        if (done) { gained += quest.xp; weekProg += 1; questsDone = 1 }

        val (newLevel, newXp) = addXpInternal(state.level, state.xp, gained)

        // update today history entry
        val today = state.dailyDate
        val hist = state.history7.toMutableList()
        val idx = hist.indexOfFirst { it.date == today }
        if (idx >= 0) {
            val d = hist[idx]
            hist[idx] = d.copy(xpEarned = d.xpEarned + gained, questsCompleted = d.questsCompleted + questsDone)
        } else {
            hist.add(DayLog(today, xpEarned = gained, questsCompleted = questsDone))
            if (hist.size > 7) hist.removeAt(0)
        }

        state = state.copy(
            questProgressToday = updatedMap,
            weeklyBossProgress = weekProg,
            level = newLevel,
            xp = newXp,
            lifetimeQuests = state.lifetimeQuests + questsDone,
            history7 = hist
        )
        viewModelScope.launch { persist() }
    }

    private fun addXpInternal(level: Int, xp: Int, amount: Int): Pair<Int, Int> {
        var lvl = level
        var x = xp + amount
        var leveledUp = false
        while (x >= lvl * 200) { x -= lvl * 200; lvl++; leveledUp = true }
        if (leveledUp) autoUnlockRewards(lvl)
        return lvl to x
    }

    private fun autoUnlockRewards(newLevel: Int) {
        val updated = state.rewards.map { r -> if (!r.claimed && newLevel >= r.levelRequired) r.copy(claimed = true) else r }
        state = state.copy(rewards = updated)
    }

    fun addMeal(name: String, calories: Int) {
        val m = MealEntry(
            id = System.currentTimeMillis().toString(),
            date = state.dailyDate,
            name = name,
            calories = calories
        )
        state = state.copy(meals = state.meals + m)
        viewModelScope.launch { persist() }
    }

    fun removeMeal(id: String) {
        state = state.copy(meals = state.meals.filterNot { it.id == id })
        viewModelScope.launch { persist() }
    }

    fun updateSettings(newSettings: SettingsState) {
        state = state.copy(settings = newSettings)
        viewModelScope.launch { persist() }
    }

    fun resetDaily() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val qs = generateDailyQuests()
        val prog = qs.associate { it.id to QuestProgress(it.id) }
        state = state.copy(dailyDate = today, questsToday = qs, questProgressToday = prog)
        viewModelScope.launch { persist() }
    }

    fun resetWeek() {
        val anchor = weekStartFor(LocalDate.now())
        state = state.copy(weekStart = anchor, weeklyBossProgress = 0)
        viewModelScope.launch { persist() }
    }

    private suspend fun persist() {
        ds.edit { e -> e[Keys.PLAYER_JSON] = json.encodeToString(state) }
    }
}

// ==========================
// Quest generation presets
// ==========================

private fun generateDailyQuests(): List<Quest> {
    val pool = listOf(
        Quest("pool_walk_5", "Pool Walk", "Walk laps — 5 min", 10, QuestType.POOL, 5, "min"),
        Quest("pool_kicks_3", "Wall Kicks", "Hold wall & flutter — 3 min", 10, QuestType.POOL, 3, "min")
    )
    val vr = listOf(
        Quest("vr_box_3", "VR Boxing", "Shadow boxing — 3 min", 10, QuestType.VR, 3, "min"),
        Quest("vr_rhythm_4", "VR Rhythm", "Warmup — 4 min", 10, QuestType.VR, 4, "min")
    )
    val dumbbells = listOf(
        Quest("db_curl_10", "Dumbbell Curls", "8 lb curls — 10/side", 10, QuestType.DUMBBELL, 10, "reps"),
        Quest("db_press_10", "Seated Press", "Press — 10 reps", 10, QuestType.DUMBBELL, 10, "reps")
    )
    val stretch = listOf(
        Quest("st_torso_2", "Torso Twists", "2×10/side", 10, QuestType.STRETCH, 2, "sets"),
        Quest("st_calf_2", "Calf Stretch", "2×30s", 10, QuestType.STRETCH, 2, "sets")
    )
    val breath = listOf(
        Quest("breath_box_2", "Box Breathing", "4-4-4-4 — 2 min", 10, QuestType.BREATH, 2, "min")
    )
    return listOf(pool.random(), vr.random(), dumbbells.random(), stretch.random(), breath.random())
}

// ==========================
// Theme
// ==========================

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD4AF37), // Gold
    onPrimary = Color.Black,
    secondary = Color(0xFF4CAF50),
    onSecondary = Color.Black,
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2A2A2D),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC)
)

// ==========================
// Navigation
// ==========================

enum class Route { Home, Boss, Rewards, Dashboard, Settings, ManageQuests, Meals }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestFitApp(vm: FitnessViewModel = viewModel()) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    val s = vm.state

    // Schedule notification when settings change
    LaunchedEffect(s.settings) {
        if (s.settings.notificationsEnabled) scheduleDailyReminder(
            ctx,
            s.settings.reminderHour,
            s.settings.reminderMinute
        ) else cancelDailyReminder(ctx)
    }

    MaterialTheme(colorScheme = if (s.settings.darkTheme) DarkColors else darkColorScheme()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("QuestFit", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text("Make IRL gains like XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                })
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Quests") }, icon = { Icon(Icons.Filled.FitnessCenter, null) })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Boss") }, icon = { Icon(Icons.Filled.WaterDrop, null) })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, label = { Text("Rewards") }, icon = { Icon(Icons.Filled.EmojiEvents, null) })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, label = { Text("Dash") }, icon = { Icon(Icons.Filled.QueryStats, null) })
                    NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, label = { Text("Settings") }, icon = { Icon(Icons.Filled.Settings, null) })
                }
            }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                HeaderXP(level = s.level, xp = s.xp, need = s.level * 200, streak = s.streakDays)
                when (tab) {
                    0 -> HomeScreen(vm)
                    1 -> BossScreen(state = s, onResetWeek = { vm.resetWeek() })
                    2 -> RewardsScreen(s.rewards)
                    3 -> DashboardScreen(s)
                    4 -> SettingsScreen(s.settings, onUpdate = { vm.updateSettings(it) })
                }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: FitnessViewModel) {
    val s = vm.state
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today's Quests — ${s.dailyDate}", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Row {
                TextButton(onClick = { vm.resetDaily() }) { Text("New Day") }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(s.questsToday) { q ->
                val prog = s.questProgressToday[q.id]
                QuestCard(q, prog) { id -> vm.completeQuest(id) }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        MealBar(vm)
    }
}

@Composable
fun MealBar(vm: FitnessViewModel) {
    val s = vm.state
    val todayMeals = s.meals.filter { it.date == s.dailyDate }
    val total = todayMeals.sumOf { it.calories }
    val target = s.settings.dailyCalorieTarget
    val ratio by animateFloatAsState((total.toFloat() / target).coerceIn(0f, 1f))

    Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Meals Today", fontWeight = FontWeight.SemiBold)
                Text("${total} / ${target} kcal")
            }
            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(RoundedCornerShape(50)))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                var mealName by remember { mutableStateOf("") }
                var mealCals by remember { mutableStateOf("") }
                OutlinedTextField(mealName, onValueChange = { mealName = it }, label = { Text("Item") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(mealCals, onValueChange = { mealCals = it.filter { ch -> ch.isDigit() } }, label = { Text("kcal") }, modifier = Modifier.width(100.dp))
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val c = mealCals.toIntOrNull() ?: 0
                    if (mealName.isNotBlank() && c > 0) {
                        vm.addMeal(mealName.trim(), c)
                        mealName = ""; mealCals = ""
                    }
                }) { Text("Add") }
            }
            if (todayMeals.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                todayMeals.takeLast(5).reversed().forEach { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${m.calories} kcal")
                            IconButton(onClick = { vm.removeMeal(m.id) }) { Icon(Icons.Filled.Delete, null) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestCard(q: Quest, prog: QuestProgress?, onTick: (String) -> Unit) {
    val progress = prog?.progress ?: 0
    val done = prog?.completed ?: false
    val ratio by animateFloatAsState(targetValue = (progress.toFloat() / q.targetCount.toFloat()).coerceIn(0f,1f), animationSpec = tween(450))

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (done) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuestIcon(q.type, done)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(q.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(q.description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                }
                AnimatedVisibility(visible = done) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp).clip(RoundedCornerShape(50)), color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${progress}/${q.targetCount} ${q.unitLabel}")
                if (!done) {
                    Button(onClick = { onTick(q.id) }, shape = RoundedCornerShape(50)) { Text("+1 ${q.unitLabel}") }
                } else {
                    Text("+${q.xp} XP", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun QuestIcon(type: QuestType, done: Boolean) {
    val tint = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val icon = when (type) {
        QuestType.POOL -> Icons.Filled.Pool
        QuestType.VR -> Icons.Filled.SportsEsports
        QuestType.DUMBBELL -> Icons.Filled.FitnessCenter
        QuestType.JUGS -> Icons.Filled.WaterDrop
        QuestType.STRETCH, QuestType.BREATH -> Icons.Filled.SelfImprovement
        QuestType.WALK -> Icons.Filled.DirectionsWalk
    }
    Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
fun BossScreen(state: PlayerState, onResetWeek: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Weekly Boss", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("Week start: ${state.weekStart}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        val p = state.weeklyBossProgress
        val g = state.weeklyBossGoal
        val progress by animateFloatAsState(targetValue = (p.toFloat() / g.toFloat()).coerceIn(0f,1f), animationSpec = tween(600))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(12.dp).clip(RoundedCornerShape(50)), color = Color(0xFFE57373), trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Text("Boss HP: $p / $g (complete quests to deal damage)")
        Spacer(Modifier.height(12.dp))
        if (p >= g) {
            Text("Boss defeated! Claim your IRL loot.", color = MaterialTheme.colorScheme.secondary)
            TextButton(onClick = onResetWeek) { Text("New Week") }
        } else {
            Text("Tip: Pool walks and stretches are easy wins.")
        }
    }
}

@Composable
fun RewardsScreen(rewards: List<Reward>) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(rewards) { r ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Level ${r.levelRequired}: ${r.name}", fontWeight = FontWeight.SemiBold)
                    Text(r.description, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    val status = if (r.claimed) "Unlocked" else "Locked"
                    AssistChip(onClick = {}, label = { Text(status) })
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun DashboardScreen(s: PlayerState) {
    val weekLogs = s.history7
    val totalXp = weekLogs.sumOf { it.xpEarned }
    val totalQuests = weekLogs.sumOf { it.questsCompleted }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dashboard", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        // Weekly summary card
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("This Week", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("XP: $totalXp • Quests: $totalQuests • Streak: ${s.streakDays}")
            }
        }
        Spacer(Modifier.height(12.dp))
        // Lifetime
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Lifetime", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Level ${s.level} • XP ${s.xp} • Total Quests ${s.lifetimeQuests}")
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: SettingsState, onUpdate: (SettingsState) -> Unit) {
    var enabled by remember { mutableStateOf(settings.notificationsEnabled) }
    var hour by remember { mutableStateOf(settings.reminderHour) }
    var minute by remember { mutableStateOf(settings.reminderMinute) }
    var kcal by remember { mutableStateOf(settings.dailyCalorieTarget.toString()) }
    var dark by remember { mutableStateOf(settings.darkTheme) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        SettingRow(title = "Notifications", subtitle = if (enabled) "On" else "Off") {
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        AnimatedVisibility(visible = enabled) {
            Column {
                TimePickerRow(hour, minute, onChange = { h, m -> hour = h; minute = m })
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingRow(title = "Daily Calorie Target", subtitle = "How many kcal per day?") {
            var text by remember { mutableStateOf(kcal) }
            OutlinedTextField(text, onValueChange = {
                text = it.filter { ch -> ch.isDigit() }
                kcal = text
            }, modifier = Modifier.width(120.dp))
        }
        Spacer(Modifier.height(8.dp))
        SettingRow(title = "Dark Theme", subtitle = "Use dark colors by default") {
            Switch(checked = dark, onCheckedChange = { dark = it })
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val target = kcal.toIntOrNull() ?: settings.dailyCalorieTarget
            onUpdate(settings.copy(
                notificationsEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute,
                dailyCalorieTarget = target,
                darkTheme = dark
            ))
        }) { Text("Save Settings") }
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, trailing: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
        Row(content = trailing)
    }
}

@Composable
fun TimePickerRow(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    var h by remember { mutableStateOf(hour) }
    var m by remember { mutableStateOf(minute) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(h.toString().padStart(2, '0'), onValueChange = {
            h = it.filter(Char::isDigit).take(2).toIntOrNull()?.coerceIn(0,23) ?: h
            onChange(h,m)
        }, label = { Text("Hour") }, modifier = Modifier.width(90.dp))
        OutlinedTextField(m.toString().padStart(2, '0'), onValueChange = {
            m = it.filter(Char::isDigit).take(2).toIntOrNull()?.coerceIn(0,59) ?: m
            onChange(h,m)
        }, label = { Text("Minute") }, modifier = Modifier.width(110.dp))
    }
}

// ==========================
// Header
// ==========================

@Composable
fun HeaderXP(level: Int, xp: Int, need: Int, streak: Int) {
    val progress by animateFloatAsState(targetValue = (xp.toFloat() / need.toFloat()).coerceIn(0f,1f), animationSpec = tween(600))
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MilitaryTech, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Level $level", fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(10.dp).clip(RoundedCornerShape(50)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("XP: $xp / $need", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            AssistChip(onClick = {}, label = { Text("Streak: $streak") })
        }
    }
}

// ==========================
// Notifications — Alarm + Receiver
// ==========================

private const val CHANNEL_ID = "questfit_channel"
private const val ALARM_REQUEST_CODE = 9172

fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    createNotificationChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
    val pending = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
    )

    // Next trigger time for today or tomorrow
    val now = LocalTime.now()
    var trigger = LocalTime.of(hour, minute)
    val firstMillis = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        set(java.util.Calendar.HOUR_OF_DAY, trigger.hour)
        set(java.util.Calendar.MINUTE, trigger.minute)
        if (trigger.isBefore(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        firstMillis,
        AlarmManager.INTERVAL_DAY,
        pending
    )
}

fun cancelDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
    val pending = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
    )
    alarmManager.cancel(pending)
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        createNotificationChannel(context)
        val activityIntent = Intent(context, MainActivity::class.java)
        val contentPending = PendingIntent.getActivity(
            context, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val notif = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("QuestFit")
            .setContentText("Your quests await, hero! Tap to begin.")
            .setContentIntent(contentPending)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1001, notif)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule default 10:00 if we can’t read DataStore here.
            scheduleDailyReminder(context, 10, 0)
        }
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "QuestFit Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Daily quest reminders" }
            )
        }
    }
}

// ==========================
// Activity
// ==========================

class MainActivity : ComponentActivity() {
    private val vm by viewModels<FitnessViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuestFitApp(vm) }
        // On first launch, schedule with current settings
        val s = vm.state.settings
        if (s.notificationsEnabled) scheduleDailyReminder(this, s.reminderHour, s.reminderMinute)
    }
}
