package com.android.libredialer.controller.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.android.libredialer.MainActivity
import org.json.JSONArray
import org.json.JSONObject

object CallReminderManager {
    const val ACTION_TRIGGER = "com.android.libredialer.CALL_REMINDER_TRIGGER"
    const val ACTION_BOOT = "com.android.libredialer.CALL_REMINDER_RESCHEDULE"
    const val EXTRA_ID = "call_reminder_id"
    const val EXTRA_NUMBER = "call_reminder_number"
    private const val PREFS = "call_reminders"
    private const val KEY_ENTRIES = "entries"
    private const val CHANNEL_ID = "call_reminders"

    private data class Entry(val id: String, val number: String, val triggerAt: Long)

    fun schedule(context: Context, number: String, triggerAt: Long): String {
        val id = "${number.trim()}-$triggerAt-${System.nanoTime()}"
        val entries = load(context).toMutableList().apply { add(Entry(id, number.trim(), triggerAt)) }
        save(context, entries)
        scheduleAlarm(context, entries.last())
        return id
    }

    fun rescheduleAll(context: Context) {
        load(context).forEach { entry ->
            if (entry.triggerAt > System.currentTimeMillis()) scheduleAlarm(context, entry)
            else remove(context, entry.id)
        }
    }

    fun trigger(context: Context, id: String, number: String?) {
        val entry = load(context).firstOrNull { it.id == id } ?: return
        remove(context, id)
        showNotification(context, number?.takeIf { it.isNotBlank() } ?: entry.number)
    }

    private fun scheduleAlarm(context: Context, entry: Entry) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(context, entry)
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(entry.triggerAt, pendingIntent),
                pendingIntent
            )
        } catch (_: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, entry.triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, entry.triggerAt, pendingIntent)
            }
        }
    }

    private fun remove(context: Context, id: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, CallReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER
            putExtra(EXTRA_ID, id)
        }
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        save(context, load(context).filterNot { it.id == id })
    }

    private fun pendingIntent(context: Context, entry: Entry): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            entry.id.hashCode(),
            Intent(context, CallReminderReceiver::class.java).apply {
                action = ACTION_TRIGGER
                putExtra(EXTRA_ID, entry.id)
                putExtra(EXTRA_NUMBER, entry.number)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun load(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList(json.length()) {
                repeat(json.length()) {
                    val item = json.getJSONObject(it)
                    add(Entry(item.getString("id"), item.getString("number"), item.getLong("triggerAt")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, entries: List<Entry>) {
        val json = JSONArray()
        entries.forEach {
            json.put(JSONObject().apply {
                put("id", it.id)
                put("number", it.number)
                put("triggerAt", it.triggerAt)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ENTRIES, json.toString()).apply()
    }

    private fun showNotification(context: Context, number: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Call reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            manager.areNotificationsEnabled().not()
        ) return
        val openIntent = PendingIntent.getActivity(
            context,
            number.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_DIAL
                data = android.net.Uri.fromParts("tel", number, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            number.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.android.libredialer.R.drawable.ic_launcher_phone)
                .setContentTitle("Call reminder")
                .setContentText("Remember to call $number")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()
        )
    }
}

class CallReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CallReminderManager.ACTION_TRIGGER -> CallReminderManager.trigger(
                context,
                intent.getStringExtra(CallReminderManager.EXTRA_ID) ?: return,
                intent.getStringExtra(CallReminderManager.EXTRA_NUMBER)
            )
            Intent.ACTION_BOOT_COMPLETED,
            CallReminderManager.ACTION_BOOT -> CallReminderManager.rescheduleAll(context)
        }
    }
}
