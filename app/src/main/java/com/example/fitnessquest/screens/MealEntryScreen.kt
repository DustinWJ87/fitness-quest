package com.example.fitnessquest.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fitnessquest.data.MealEntry
import com.example.fitnessquest.data.UIState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEntryScreen(
    ui: UIState,
    onBack: () -> Unit,
    onAdd: (String, Int) -> Unit,
    onRemove: (String) -> Unit
) {
    val today = LocalDate.now().toString()
    val todays = ui.meals.filter { it.dateIso == today }
    val total = todays.sumOf { it.calories }


    Scaffold(topBar = { TopAppBar(title = { Text("Meals ($total kcal)") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily target: ${ui.settings.dailyCalorieTarget} kcal")
                Spacer(Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = { (total.toFloat() / ui.settings.dailyCalorieTarget.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(10.dp).padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            MealAdder(onAdd)
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(todays, key = { it.id }) { m ->
                    ListItem(
                        headlineContent = { Text(m.name) },
                        supportingContent = { Text("${m.calories} kcal") },
                        trailingContent = { IconButton(onClick = { onRemove(m.id) }) { Icon(Icons.Default.Delete, null) } }
                    )
                    Divider()
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MealAdder(onAdd: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Meal / Snack") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = kcal,
            onValueChange = { kcal = it.filter { ch -> ch.isDigit() } },
            label = { Text("Calories") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val c = kcal.toIntOrNull() ?: 0
            if (name.isNotBlank() && c > 0) {
                onAdd(name.trim(), c); name = ""; kcal = ""
            }
        }) { Text("Add") }
    }
}