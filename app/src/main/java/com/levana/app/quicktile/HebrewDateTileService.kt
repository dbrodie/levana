package com.levana.app.quicktile

import android.app.LocaleManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.levana.app.MainActivity
import com.levana.app.R
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

        val hebrewDay = formatter.formatHebrewNumber(jc.jewishDayOfMonth)
            .replace("\u05F3", "")  // strip geresh — too small to read in icon
            .replace("\u05F4", "")  // strip gershayim

        tile.state = Tile.STATE_ACTIVE
        tile.label = hebrewDate
        tile.subtitle = subtitle
        tile.icon = buildTileIcon(hebrewDay)
        tile.updateTile()
    }

    private fun buildTileIcon(hebrewDay: String): Icon {
        val sizePx = (24 * resources.displayMetrics.density).toInt()

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw the calendar outline (white fill on transparent background)
        val drawable = getDrawable(R.drawable.ic_tile_calendar)!!
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)

        // Knock out the Hebrew day letters as transparent negative space
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.38f
            typeface = Typeface.DEFAULT_BOLD
        }
        // Center text in the date area (calendar grid occupies roughly y 40%–90% of icon)
        canvas.drawText(hebrewDay, sizePx / 2f, sizePx * 0.72f, paint)

        return Icon.createWithBitmap(bitmap)
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
