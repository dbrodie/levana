package com.levana.app.quicktile

import android.app.LocaleManager
import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.levana.app.MainActivity
import com.levana.app.data.HolidayMapper
import com.levana.app.notifications.NotificationPoster
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.GregorianCalendar
import java.util.Locale

class HebrewDateTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return

        val today = LocalDate.now()
        val jc = JewishCalendar(GregorianCalendar(today.year, today.monthValue - 1, today.dayOfMonth))

        val localeManager = getSystemService(LocaleManager::class.java)
        val locales = localeManager.applicationLocales
        val isHebrew = !locales.isEmpty && locales[0]?.language.let { it == "iw" || it == "he" }

        val formatter = HebrewDateFormatter().apply { isHebrewFormat = isHebrew }
        val hebrewDate = formatter.format(jc)

        val subtitle = when {
            jc.yomTovIndex >= 0 -> {
                val holiday = HolidayMapper.mapHoliday(jc.yomTovIndex)
                if (holiday != null) if (isHebrew) holiday.hebrewName else holiday.name else null
            }
            jc.isRoshChodesh -> {
                val holiday = HolidayMapper.mapHoliday(JewishCalendar.ROSH_CHODESH)
                if (holiday != null) if (isHebrew) holiday.hebrewName else holiday.name else null
            }
            else -> today.dayOfWeek.getDisplayName(
                TextStyle.FULL,
                if (isHebrew) Locale("iw") else Locale.getDefault()
            )
        }

        tile.state = Tile.STATE_ACTIVE
        tile.label = hebrewDate
        tile.subtitle = subtitle
        tile.updateTile()
    }

    override fun onClick() {
        val tile = qsTile ?: return

        val today = LocalDate.now()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, today.toEpochDay())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            today.toEpochDay().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startActivityAndCollapse(pending)
    }
}
