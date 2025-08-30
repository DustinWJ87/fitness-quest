package com.example.fitnessquest.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
// Default 9:00 if app state not loaded yet; user can change in-app
            scheduleDailyReminder(context, 9, 0)
        }
    }
}