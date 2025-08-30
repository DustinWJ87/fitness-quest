package com.example.fitnessquest.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessquest.data.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    ui: UIState,
    onTickQuest: (String) -> Unit,
    onResetDay: () -> Unit,
    onGoBoss: () -> Unit,
    onGoMeals: () -> Unit,
    onToggleNotifs: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("QuestFit", fontWeight = FontWeight.ExtraBold)
                    Text("Make IRL gains like XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            })
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            HeaderXP(level = ui.level, xp = ui.xp, need = ui.level * 200, streak = ui.streakDays)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                AssistChip(onClick = onGoBoss, label = { Text("Boss") }, leadingIcon = { Icon(Icons.Default.SportsEsports, null) })
                AssistChip(onClick = onGoMeals, label = { Text("Meals") }, leadingIcon = { Icon(Icons.Default.Restaurant, null) })
                FilterChip(selected = ui.settings.notificationsEnabled, onClick = onToggleNotifs, label = { Text("Notifs") }, leadingIcon = { Icon(Icons.Default.Notifications, null) })
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Today's Quests — ${ui.dailyDate}", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                TextButton(onClick = onResetDay) { Text("New Day") }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(ui.questsToday) { q -> QuestCard(q, ui.questProgress[q.id], onTickQuest) }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}


@Composable
fun HeaderXP(level: Int, xp: Int, need: Int, streak: Int) {
    val progress by animateFloatAsState(targetValue = (xp.toFloat() / need.toFloat()).coerceIn(0f,1f), animationSpec = tween(600))
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
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
                AnimatedVisibility(visible = done) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.secondary) }
            }
            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp).clip(RoundedCornerShape(50)), color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${progress}/${q.targetCount} ${q.unitLabel}")
                if (!done) Button(onClick = { onTick(q.id) }, shape = RoundedCornerShape(50)) { Text("+1 ${q.unitLabel}") } else Text("+${q.xp} XP", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun QuestIcon(type: QuestType, done: Boolean) {
    val tint = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val icon = when (type) {
        QuestType.POOL -> Icons.Default.Pool
        QuestType.VR -> Icons.Default.SportsEsports
        QuestType.DUMBBELL -> Icons.Default.FitnessCenter
        QuestType.JUGS -> Icons.Default.WaterDrop
        QuestType.STRETCH -> Icons.Default.SelfImprovement
        QuestType.WALK -> Icons.Default.DirectionsWalk
        QuestType.BREATH -> Icons.Default.SelfImprovement
    }
    Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
}