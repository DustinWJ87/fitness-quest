package com.example.fitnessquest


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitnessquest.data.AppViewModel
import com.example.fitnessquest.navigation.Routes
import com.example.fitnessquest.notifications.scheduleDailyReminder
import com.example.fitnessquest.screens.BossBattleScreen
import com.example.fitnessquest.screens.DashboardScreen
import com.example.fitnessquest.screens.MealEntryScreen
import com.example.fitnessquest.ui.theme.FQTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FQTheme(darkTheme = true) {
                val vm: AppViewModel = viewModel()
                val nav = rememberNavController()


// Schedule notification after first composition if enabled
                LaunchedEffect(vm.ui.settings.notificationsEnabled, vm.ui.settings.notificationHour, vm.ui.settings.notificationMinute) {
                    if (vm.ui.settings.notificationsEnabled) {
                        scheduleDailyReminder(
                            context = this@MainActivity,
                            hour = vm.ui.settings.notificationHour,
                            minute = vm.ui.settings.notificationMinute
                        )
                    }
                }


                NavHost(navController = nav, startDestination = Routes.Dashboard) {
                    composable(Routes.Dashboard) {
                        DashboardScreen(
                            ui = vm.ui,
                            onTickQuest = { vm.completeQuest(it) },
                            onResetDay = { vm.resetDaily() },
                            onGoBoss = { nav.navigate(Routes.Boss) },
                            onGoMeals = { nav.navigate(Routes.Meals) },
                            onToggleNotifs = { vm.toggleNotifications() }
                        )
                    }
                    composable(Routes.Boss) {
                        BossBattleScreen(
                            ui = vm.ui,
                            onBack = { nav.popBackStack() },
                            onResetWeek = { vm.resetWeek() }
                        )
                    }
                    composable(Routes.Meals) {
                        MealEntryScreen(
                            ui = vm.ui,
                            onBack = { nav.popBackStack() },
                            onAdd = { name, kcal -> vm.addMeal(name, kcal) },
                            onRemove = { id -> vm.removeMeal(id) }
                        )
                    }
                }
            }
        }
    }
}