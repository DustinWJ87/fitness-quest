package com.example.fitnessquest.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fitnessquest.R


class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val lines = listOf(
            "Rise and grind, bro — Karen still talks smack.",
            "Skip leg day? Not today, champ.",
            "Pool walks = easy XP. Go farm it.",
            "Couch Potato is regenerating… stop him!",
            "You up? — Ex. (Ignore. Do quests.)"
        )
        val text = lines.random()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("FitnessQuest Daily")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(context)) { notify(777, builder.build()) }
    }
}