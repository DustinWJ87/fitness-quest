package com.example.fitnessquest.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar


const val CHANNEL_ID = "questfit_daily"


fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    createChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
    val pending = PendingIntent.getBroadcast(
        context, 1001, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )


    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
    }


    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        cal.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pending
    )
}


fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Daily Gains", NotificationManager.IMPORTANCE_DEFAULT)
        mgr.createNotificationChannel(channel)
    }
}