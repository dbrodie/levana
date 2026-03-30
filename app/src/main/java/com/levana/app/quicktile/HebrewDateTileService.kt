package com.levana.app.quicktile

import android.app.LocaleManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.levana.app.MainActivity
import com.levana.app.R
import com.levana.app.data.HolidayMapper
import com.levana.app.data.PreferencesRepository
import com.levana.app.notifications.NotificationPoster
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.GregorianCalendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class HebrewDateTileService : TileService() {

    private val preferencesRepository: PreferencesRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        val tile = qsTile ?: return

        serviceScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val today = prefs.devDateOverride ?: LocalDate.now()
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
    }

    private fun buildTileIcon(hebrewDay: String): Icon {
        val sizePx = (24 * resources.displayMetrics.density).toInt()

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw the calendar outline (white fill on transparent background)
        val drawable = getDrawable(R.drawable.ic_tile_calendar)!!
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)

        // Draw the Hebrew day letter in white to match the calendar frame
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
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

        serviceScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val today = prefs.devDateOverride ?: LocalDate.now()
            val intent = Intent(this@HebrewDateTileService, MainActivity::class.java).apply {
                putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, today.toEpochDay())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                this@HebrewDateTileService,
                today.toEpochDay().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
