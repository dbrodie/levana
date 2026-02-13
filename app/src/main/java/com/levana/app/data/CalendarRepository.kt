package com.levana.app.data

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.levana.app.domain.model.DayInfo
import com.levana.app.domain.model.HebrewDay
import com.levana.app.domain.model.HebrewMonth
import com.levana.app.domain.model.HebrewYearMonth
import com.levana.app.domain.model.HolidayCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.GregorianCalendar

class CalendarRepository {

    private val hebrewFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = true
    }

    private val translitFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = false
    }

    fun getHebrewDay(date: LocalDate, inIsrael: Boolean = false): HebrewDay {
        val jewishCalendar = createJewishCalendar(date, inIsrael)
        return toHebrewDay(jewishCalendar, date)
    }

    fun getMonthDays(yearMonth: YearMonth, inIsrael: Boolean = false): List<HebrewDay> {
        return (1..yearMonth.lengthOfMonth()).map { dayOfMonth ->
            val date = yearMonth.atDay(dayOfMonth)
            getHebrewDay(date, inIsrael)
        }
    }

    fun getHebrewMonthDays(
        hebrewYearMonth: HebrewYearMonth,
        inIsrael: Boolean = false
    ): List<HebrewDay> {
        val daysInMonth = hebrewYearMonth.daysInMonth()
        return (1..daysInMonth).map { day ->
            val jc = JewishCalendar(
                hebrewYearMonth.year,
                hebrewYearMonth.jewishDateMonth,
                day
            )
            jc.setInIsrael(inIsrael)
            val gc = jc.gregorianCalendar
            val date = LocalDate.of(
                gc.get(GregorianCalendar.YEAR),
                gc.get(GregorianCalendar.MONTH) + 1,
                gc.get(GregorianCalendar.DAY_OF_MONTH)
            )
            toHebrewDay(jc, date)
        }
    }

    fun getHebrewMonthName(hebrewYearMonth: HebrewYearMonth): String {
        val jd = JewishDate(
            hebrewYearMonth.year,
            hebrewYearMonth.jewishDateMonth,
            1
        )
        return hebrewFormatter.formatMonth(jd)
    }

    fun getDayInfo(
        date: LocalDate,
        inIsrael: Boolean = false,
        showModernIsraeli: Boolean = true
    ): DayInfo {
        val jewishCalendar = createJewishCalendar(date, inIsrael)
        val hebrewDay = toHebrewDay(jewishCalendar, date)

        val holidays = buildList {
            val yomTovIndex = jewishCalendar.yomTovIndex
            if (yomTovIndex >= 0) {
                HolidayMapper.mapHoliday(yomTovIndex)?.let { add(it) }
            }
            if (jewishCalendar.isRoshChodesh &&
                yomTovIndex != JewishCalendar.ROSH_CHODESH
            ) {
                HolidayMapper.mapHoliday(JewishCalendar.ROSH_CHODESH)?.let { add(it) }
            }
        }.let { list ->
            if (showModernIsraeli) {
                list
            } else {
                list.filter { it.category != HolidayCategory.MODERN_ISRAELI }
            }
        }

        val parshaEnum = jewishCalendar.parshah
        val parsha = if (parshaEnum != null &&
            parshaEnum != JewishCalendar.Parsha.NONE
        ) {
            translitFormatter.formatParsha(jewishCalendar)
        } else {
            null
        }

        val parshaHebrew = if (parshaEnum != null &&
            parshaEnum != JewishCalendar.Parsha.NONE
        ) {
            hebrewFormatter.formatParsha(jewishCalendar)
        } else {
            null
        }

        val specialShabbat = getSpecialShabbat(jewishCalendar)

        val molad = if (jewishCalendar.isShabbosMevorchim) {
            formatMolad(jewishCalendar)
        } else {
            null
        }

        val omerDay = jewishCalendar.dayOfOmer.let {
            if (it == -1) null else it
        }

        return DayInfo(
            hebrewDay = hebrewDay,
            dayOfWeek = date.dayOfWeek,
            gregorianDate = date,
            holidays = holidays,
            parsha = parsha,
            parshaHebrew = parshaHebrew,
            specialShabbat = specialShabbat,
            molad = molad,
            omerDay = omerDay
        )
    }

    private fun getSpecialShabbat(jc: JewishCalendar): String? {
        val special = jc.specialShabbos
        if (special == null || special == JewishCalendar.Parsha.NONE) {
            return null
        }
        return when (special) {
            JewishCalendar.Parsha.SHKALIM -> "Shabbat Shekalim"
            JewishCalendar.Parsha.ZACHOR -> "Shabbat Zachor"
            JewishCalendar.Parsha.PARA -> "Shabbat Parah"
            JewishCalendar.Parsha.HACHODESH -> "Shabbat HaChodesh"
            JewishCalendar.Parsha.HAGADOL -> "Shabbat HaGadol"
            JewishCalendar.Parsha.CHAZON -> "Shabbat Chazon"
            JewishCalendar.Parsha.NACHAMU -> "Shabbat Nachamu"
            JewishCalendar.Parsha.SHUVA -> "Shabbat Shuva"
            JewishCalendar.Parsha.SHIRA -> "Shabbat Shirah"
            else -> null
        }
    }

    private fun formatMolad(jc: JewishCalendar): String {
        val moladDate = jc.moladAsDate ?: return ""
        val moladLocal = moladDate.toInstant()
            .atZone(ZoneId.of("Asia/Jerusalem"))
        val dayOfWeek = moladLocal.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.FULL,
            java.util.Locale.getDefault()
        )
        val time = moladLocal.toLocalTime()
            .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
        val chalakim = jc.moladChalakim
        return "Molad: $dayOfWeek $time, $chalakim chalakim"
    }

    private fun createJewishCalendar(date: LocalDate, inIsrael: Boolean = false): JewishCalendar {
        val gregorianCalendar = GregorianCalendar(
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        return JewishCalendar(gregorianCalendar).apply {
            setInIsrael(inIsrael)
        }
    }

    private fun toHebrewDay(jewishCalendar: JewishCalendar, date: LocalDate): HebrewDay {
        val hasCandles = date.dayOfWeek == DayOfWeek.FRIDAY ||
            jewishCalendar.isErevYomTov ||
            jewishCalendar.isErevYomTovSheni
        val yomTovIndex = jewishCalendar.yomTovIndex
        val holidayCategory = if (yomTovIndex >= 0) {
            HolidayMapper.mapHoliday(yomTovIndex)?.category
        } else {
            null
        }
        return HebrewDay(
            day = jewishCalendar.jewishDayOfMonth,
            month = HebrewMonth.from(
                jewishCalendar.jewishMonth,
                jewishCalendar.isJewishLeapYear
            ),
            year = jewishCalendar.jewishYear,
            hebrewFormatted = hebrewFormatter.format(jewishCalendar),
            transliterated = translitFormatter.format(jewishCalendar),
            hebrewDayOfMonthFormatted = hebrewFormatter.formatHebrewNumber(
                jewishCalendar.jewishDayOfMonth
            ),
            gregorianDate = date,
            hasCandles = hasCandles,
            holidayCategory = holidayCategory
        )
    }
}
