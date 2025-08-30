package com.example.fitnessquest.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.fitnessquest.data.UIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BossBattleScreen(
    ui: UIState,
    onBack: () -> Unit,
    onResetWeek: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Boss") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(ui.boss.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            val stage = ui.boss.stage
            val icon = when (stage) {
                0 -> Icons.Default.SentimentVerySatisfied
                1 -> Icons.Default.SentimentSatisfied
                2 -> Icons.Default.SentimentDissatisfied
                3 -> Icons.Default.MoodBad
                else -> Icons.Default.DeleteForever
            }
            val tint = when (stage) {
                0 -> MaterialTheme.colorScheme.secondary
                1 -> MaterialTheme.colorScheme.primary
                2 -> Color(0xFFFFC107)
                3 -> Color(0xFFFF7043)
                else -> Color(0xFFE57373)
            }

            Box(
                Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(96.dp))
            }

            Spacer(Modifier.height(16.dp))
            val p = ui.weeklyProgress
            val g = ui.weeklyGoal.coerceAtLeast(1) // prevent divide by zero
            val progress by animateFloatAsState(
                targetValue = (p.toFloat() / g.toFloat()).coerceIn(0f, 1f),
                animationSpec = tween(600)
            )
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFFE57373),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text("Boss HP: $p / $g")
            Spacer(Modifier.height(16.dp))
            if (p >= g) {
                Text(
                    "Boss obliterated. Send flowers to the Brunch Squad.",
                    color = MaterialTheme.colorScheme.secondary
                )
                TextButton(onClick = onResetWeek) { Text("New Week") }
            } else {
                Text("Tip: Pool walks and stretches are easy wins.")
            }
        }
    }
}
