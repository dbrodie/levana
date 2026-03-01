package com.levana.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val CANDLE_LIGHTING = "candle_lighting"
    const val HOLIDAYS = "holidays"
    const val FASTS = "fasts"
    const val PERSONAL_EVENTS = "personal_events"
    const val OMER = "omer"

    fun createAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val channels = listOf(
            NotificationChannel(
                CANDLE_LIGHTING,
                "Candle Lighting",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Candle lighting time reminders"
            },
            NotificationChannel(
                HOLIDAYS,
                "Holidays",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Holiday reminders"
            },
            NotificationChannel(
                FASTS,
                "Fasts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Fast day reminders"
            },
            NotificationChannel(
                PERSONAL_EVENTS,
                "Personal Events",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Personal event and birthday reminders"
            },
            NotificationChannel(
                OMER,
                "Omer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Sefirat HaOmer reminders"
            }
        )

        manager.createNotificationChannels(channels)
    }
}
