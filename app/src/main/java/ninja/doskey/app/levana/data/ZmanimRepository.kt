package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.domain.model.ShabbatInfo
import ninja.doskey.app.levana.domain.model.ZmanCategory
import ninja.doskey.app.levana.domain.model.ZmanName
import ninja.doskey.app.levana.domain.model.ZmanTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

class ZmanimRepository {

    /**
     * Candle lighting and havdalah for [date]. Follows the holy-day transitions: candles are
     * lit before sunset on a weekday or on Yom Tov before Shabbat, but only after nightfall
     * when Shabbat or Yom Tov continues into another Yom Tov; havdalah is only shown when
     * the last of consecutive holy days ends. [inIsrael] selects one- or two-day Yom Tov.
     */
    fun getShabbatInfo(
        date: LocalDate,
        location: Location,
        candleLightingOffset: Double = 18.0,
        inIsrael: Boolean = false
    ): ShabbatInfo {
        val today = createJewishCalendar(date, inIsrael)
        val tomorrow = createJewishCalendar(date.plusDays(1), inIsrael)
        val isShabbat = date.dayOfWeek == DayOfWeek.SATURDAY
        val isYomTov = today.isYomTovAssurBemelacha
        val isHolyToday = today.isAssurBemelacha
        val isHolyTomorrow = tomorrow.isAssurBemelacha

        val candleLightingTime = when {
            !isHolyTomorrow -> null
            // Lighting on Shabbat, or from Yom Tov into Yom Tov, waits for nightfall
            isHolyToday && date.plusDays(1).dayOfWeek != DayOfWeek.SATURDAY ->
                getTzaitTime(date, location)
            else -> {
                val czc = createCalendar(date, location)
                czc.candleLightingOffset = candleLightingOffset
                toLocalTime(czc.candleLighting, location)
            }
        }

        val havdalahTime = if (isHolyToday && !isHolyTomorrow) {
            getTzaitTime(date, location)
        } else {
            null
        }

        return ShabbatInfo(
            candleLightingTime = candleLightingTime,
            havdalahTime = havdalahTime,
            isErevShabbat = date.dayOfWeek == DayOfWeek.FRIDAY,
            isShabbat = isShabbat,
            isErevYomTov = tomorrow.isYomTovAssurBemelacha,
            isYomTov = isYomTov
        )
    }

    fun hasCandles(date: LocalDate, inIsrael: Boolean = false): Boolean =
        createJewishCalendar(date.plusDays(1), inIsrael).isAssurBemelacha

    /**
     * When the holy days that begin on the evening of [date] end: the havdalah of the last
     * of the consecutive Shabbat / Yom Tov days. Null if no holy day begins that evening.
     */
    fun getObservanceEnd(
        date: LocalDate,
        location: Location,
        inIsrael: Boolean = false
    ): ObservanceEnd? {
        var day = date.plusDays(1)
        var jc = createJewishCalendar(day, inIsrael)
        if (!jc.isAssurBemelacha) return null
        var includesYomTov = false
        while (true) {
            includesYomTov = includesYomTov || jc.isYomTovAssurBemelacha
            val next = createJewishCalendar(day.plusDays(1), inIsrael)
            if (!next.isAssurBemelacha) break
            day = day.plusDays(1)
            jc = next
        }
        return ObservanceEnd(day, getTzaitTime(day, location), includesYomTov)
    }

    fun getZmanim(
        date: LocalDate,
        location: Location,
        candleLightingOffset: Double = 18.0,
        inIsrael: Boolean = false
    ): List<ZmanTime> {
        val czc = createCalendar(date, location)
        val shabbatInfo = getShabbatInfo(
            date,
            location,
            candleLightingOffset,
            inIsrael
        )
        return buildList {
            add(zman(ALOT_HASHACHAR, czc.alosHashachar, ZmanCategory.MORNING, location))
            add(zman(MISHEYAKIR, czc.misheyakir11Degrees, ZmanCategory.MORNING, location))
            add(zman(SUNRISE, czc.seaLevelSunrise, ZmanCategory.MORNING, location))
            add(zman(SOF_ZMAN_SHEMA_GRA, czc.sofZmanShmaGRA, ZmanCategory.MORNING, location))
            add(zman(SOF_ZMAN_SHEMA_MGA, czc.sofZmanShmaMGA, ZmanCategory.MORNING, location))
            add(zman(SOF_ZMAN_TEFILLAH, czc.sofZmanTfilaGRA, ZmanCategory.MORNING, location))
            add(zman(CHATZOT, czc.chatzos, ZmanCategory.AFTERNOON, location))
            add(zman(MINCHA_GEDOLAH, czc.minchaGedola, ZmanCategory.AFTERNOON, location))
            add(zman(MINCHA_KETANAH, czc.minchaKetana, ZmanCategory.AFTERNOON, location))
            add(zman(PLAG_HAMINCHA, czc.plagHamincha, ZmanCategory.AFTERNOON, location))
            if (shabbatInfo.showCandleLighting) {
                add(zmanTime(CANDLE_LIGHTING, shabbatInfo.candleLightingTime, ZmanCategory.EVENING))
            }
            add(zman(SUNSET, czc.seaLevelSunset, ZmanCategory.EVENING, location))
            add(zman(NIGHTFALL, czc.tzais, ZmanCategory.EVENING, location))
            if (shabbatInfo.showHavdalah) {
                add(zmanTime(HAVDALAH, shabbatInfo.havdalahTime, ZmanCategory.EVENING))
            }
            add(zman(CHATZOT_HALAYLAH, czc.solarMidnight, ZmanCategory.NIGHT, location))
        }
    }

    fun getSunsetTime(date: LocalDate, location: Location): LocalTime? {
        val czc = createCalendar(date, location)
        return toLocalTime(czc.seaLevelSunset, location)
    }

    fun getTzaitTime(date: LocalDate, location: Location): LocalTime? {
        val czc = createCalendar(date, location)
        return toLocalTime(czc.tzais, location)
    }

    fun getFastTimes(date: LocalDate, location: Location): Pair<LocalTime?, LocalTime?>? {
        val jc = createJewishCalendar(date)
        val yomTovIndex = jc.yomTovIndex
        if (yomTovIndex < 0) return null

        val isFast = yomTovIndex == JewishCalendar.SEVENTEEN_OF_TAMMUZ ||
            yomTovIndex == JewishCalendar.TISHA_BEAV ||
            yomTovIndex == JewishCalendar.FAST_OF_GEDALYAH ||
            yomTovIndex == JewishCalendar.TENTH_OF_TEVES ||
            yomTovIndex == JewishCalendar.FAST_OF_ESTHER

        if (!isFast) return null

        val czc = createCalendar(date, location)
        val isTishaBav = yomTovIndex == JewishCalendar.TISHA_BEAV

        val startTime = if (isTishaBav) {
            // Tisha B'Av starts at sunset of the previous day
            val prevCzc = createCalendar(date.minusDays(1), location)
            toLocalTime(prevCzc.sunset, location)
        } else {
            // Minor fasts start at dawn (alot hashachar)
            toLocalTime(czc.alosHashachar, location)
        }

        val endTime = toLocalTime(czc.tzais, location)

        return Pair(startTime, endTime)
    }

    private fun createJewishCalendar(date: LocalDate, inIsrael: Boolean = false): JewishCalendar {
        val gc = GregorianCalendar(
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        return JewishCalendar(gc).apply { setInIsrael(inIsrael) }
    }

    private fun toLocalTime(date: Date?, location: Location): LocalTime? = date?.toInstant()
        ?.atZone(ZoneId.of(location.timezoneId))
        ?.toLocalTime()

    private fun createCalendar(date: LocalDate, location: Location): ComplexZmanimCalendar {
        val tz = TimeZone.getTimeZone(location.timezoneId)
        val geoLocation = GeoLocation(
            location.name,
            location.latitude,
            location.longitude,
            location.elevation,
            tz
        )
        val cal = ComplexZmanimCalendar(geoLocation)
        cal.calendar = GregorianCalendar(
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        ).also { it.timeZone = tz }
        return cal
    }

    private fun zman(
        name: ZmanName,
        date: Date?,
        category: ZmanCategory,
        location: Location
    ): ZmanTime = zmanTime(name, toLocalTime(date, location), category)

    private fun zmanTime(name: ZmanName, time: LocalTime?, category: ZmanCategory): ZmanTime =
        ZmanTime(name.english, name.hebrew, time, category)

    companion object {
        val ALOT_HASHACHAR = ZmanName("Alot HaShachar", "עלות השחר")
        val MISHEYAKIR = ZmanName("Misheyakir", "משיכיר")
        val SUNRISE = ZmanName("Sunrise", "הנץ החמה")
        val SOF_ZMAN_SHEMA_GRA = ZmanName("Sof Zman Shema (GRA)", "סוף זמן שמע (גר״א)")
        val SOF_ZMAN_SHEMA_MGA = ZmanName("Sof Zman Shema (MGA)", "סוף זמן שמע (מג״א)")
        val SOF_ZMAN_TEFILLAH = ZmanName("Sof Zman Tefillah", "סוף זמן תפילה")
        val CHATZOT = ZmanName("Chatzot", "חצות היום")
        val MINCHA_GEDOLAH = ZmanName("Mincha Gedolah", "מנחה גדולה")
        val MINCHA_KETANAH = ZmanName("Mincha Ketanah", "מנחה קטנה")
        val PLAG_HAMINCHA = ZmanName("Plag HaMincha", "פלג המנחה")
        val CANDLE_LIGHTING = ZmanName("Candle Lighting", "הדלקת נרות")
        val SUNSET = ZmanName("Sunset", "שקיעה")
        val NIGHTFALL = ZmanName("Nightfall", "צאת הכוכבים")
        val HAVDALAH = ZmanName("Havdalah", "הבדלה")
        val CHATZOT_HALAYLAH = ZmanName("Chatzot HaLaylah", "חצות הלילה")

        /**
         * Every zman name, in display order. The single source for zman names: the zmanim
         * screen, the day panel and Settings → Halachic Times all read from here.
         */
        val ZMAN_NAMES: List<ZmanName> = listOf(
            ALOT_HASHACHAR, MISHEYAKIR, SUNRISE,
            SOF_ZMAN_SHEMA_GRA, SOF_ZMAN_SHEMA_MGA, SOF_ZMAN_TEFILLAH,
            CHATZOT, MINCHA_GEDOLAH, MINCHA_KETANAH, PLAG_HAMINCHA,
            CANDLE_LIGHTING, SUNSET, NIGHTFALL, HAVDALAH, CHATZOT_HALAYLAH
        )
    }
}
