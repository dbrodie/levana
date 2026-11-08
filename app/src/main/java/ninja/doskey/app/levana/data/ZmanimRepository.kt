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
import java.time.Instant
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

        val candleLightingTime = toLocalTime(
            candleLighting(date, location, candleLightingOffset, isHolyToday, isHolyTomorrow),
            location
        )

        val hasHavdalah = isHolyToday && !isHolyTomorrow
        val havdalahTime = if (hasHavdalah) {
            havdalah(date, location, inIsrael)?.let { toLocalTime(it, location) }
        } else {
            null
        }

        return ShabbatInfo(
            candleLightingTime = candleLightingTime,
            havdalahTime = havdalahTime,
            isErevShabbat = date.dayOfWeek == DayOfWeek.FRIDAY,
            isShabbat = isShabbat,
            isErevYomTov = tomorrow.isYomTovAssurBemelacha,
            isYomTov = isYomTov,
            hasCandleLighting = isHolyTomorrow,
            hasHavdalah = hasHavdalah
        )
    }

    fun hasCandles(date: LocalDate, inIsrael: Boolean = false): Boolean =
        createJewishCalendar(date.plusDays(1), inIsrael).isAssurBemelacha

    /**
     * Candle lighting on the evening of [date] as an absolute instant, by the same rules as
     * [getShabbatInfo]. Lighting at nightfall can fall after the following civil midnight,
     * which [ShabbatInfo.candleLightingTime] does not show. Null when no candles are lit
     * that evening or the time is unavailable (e.g. no nightfall at high latitudes).
     */
    fun getCandleLightingInstant(
        date: LocalDate,
        location: Location,
        candleLightingOffset: Double = 18.0,
        inIsrael: Boolean = false
    ): Instant? = candleLighting(
        date,
        location,
        candleLightingOffset,
        isHolyToday = createJewishCalendar(date, inIsrael).isAssurBemelacha,
        isHolyTomorrow = createJewishCalendar(date.plusDays(1), inIsrael).isAssurBemelacha
    )?.toInstant()

    private fun candleLighting(
        date: LocalDate,
        location: Location,
        candleLightingOffset: Double,
        isHolyToday: Boolean,
        isHolyTomorrow: Boolean
    ): Date? = when {
        !isHolyTomorrow -> null
        // Lighting on Shabbat, or from Yom Tov into Yom Tov, waits for nightfall
        isHolyToday && date.plusDays(1).dayOfWeek != DayOfWeek.SATURDAY ->
            createCalendar(date, location).nightfall
        else -> {
            val czc = createCalendar(date, location)
            czc.candleLightingOffset = candleLightingOffset
            czc.candleLighting
        }
    }

    /**
     * Havdalah on the evening of [lastDay], the last of consecutive holy days. Null when it
     * can't be computed: no nightfall after sunset that day (white nights, polar day/night),
     * or no sunset on the evening the holy days began (polar), when candle lighting is missing
     * too. In the polar night the sun still crosses 8.5° below the horizon around midday;
     * that crossing is never taken as havdalah.
     */
    private fun havdalah(lastDay: LocalDate, location: Location, inIsrael: Boolean): Date? {
        var lightingDay = lastDay.minusDays(1)
        while (createJewishCalendar(lightingDay, inIsrael).isAssurBemelacha) {
            lightingDay = lightingDay.minusDays(1)
        }
        if (createCalendar(lightingDay, location).seaLevelSunset == null) return null
        return createCalendar(lastDay, location).nightfall
    }

    /**
     * A polar day has no sunrise and/or no sunset. In the polar night the sun still crosses
     * 16.1°, 11° and 8.5° below the horizon around midday, but those crossings aren't dawn
     * or nightfall, so on a polar day Alot, Misheyakir and Nightfall are null for every
     * consumer (zmanim lists, widgets, fasts, reminders). White nights are already null
     * where the depression isn't reached.
     */
    private val ComplexZmanimCalendar.isPolarDay: Boolean
        get() = seaLevelSunrise == null || seaLevelSunset == null

    private val ComplexZmanimCalendar.alot: Date?
        get() = if (isPolarDay) null else alosHashachar

    private val ComplexZmanimCalendar.misheyakir: Date?
        get() = if (isPolarDay) null else misheyakir11Degrees

    private val ComplexZmanimCalendar.nightfall: Date?
        get() = if (isPolarDay) null else tzais

    /**
     * When the holy days that begin on the evening of [date] end: the havdalah of the last
     * of the consecutive Shabbat / Yom Tov days. Null if no holy day begins that evening.
     * The end keeps the civil date havdalah actually falls on, which is the day after the
     * last holy day when its nightfall is after midnight.
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
        val havdalah = havdalah(day, location, inIsrael)?.toInstant()?.atZone(ZoneId.of(location.timezoneId))
        return ObservanceEnd(
            date = havdalah?.toLocalDate() ?: day,
            time = havdalah?.toLocalTime(),
            includesYomTov = includesYomTov,
            lastDay = day
        )
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
        // Added in their fixed chronological order, matching ZMAN_NAMES, so no sort is needed.
        // One known exception: Tefillah (MGA) is sunrise + day/3 - 24 min and Shema (GRA) is
        // sunrise + day/4, so Tefillah (MGA) comes first when the day is shorter than 4.8
        // hours (polar winter, roughly north of 60°). That order is accepted, not sorted.
        return buildList {
            add(zman(ALOT_HASHACHAR, czc.alot, ZmanCategory.MORNING, location))
            add(zman(MISHEYAKIR, czc.misheyakir, ZmanCategory.MORNING, location))
            add(zman(SUNRISE, czc.seaLevelSunrise, ZmanCategory.MORNING, location))
            add(zman(SOF_ZMAN_SHEMA_MGA, czc.sofZmanShmaMGA, ZmanCategory.MORNING, location))
            add(zman(SOF_ZMAN_SHEMA_GRA, czc.sofZmanShmaGRA, ZmanCategory.MORNING, location))
            add(zman(SOF_ZMAN_TEFILLAH_MGA, czc.sofZmanTfilaMGA, ZmanCategory.MORNING, location))
            add(zman(SOF_ZMAN_TEFILLAH_GRA, czc.sofZmanTfilaGRA, ZmanCategory.MORNING, location))
            add(zman(CHATZOT, czc.chatzos, ZmanCategory.AFTERNOON, location))
            add(zman(MINCHA_GEDOLAH, czc.minchaGedola, ZmanCategory.AFTERNOON, location))
            add(zman(MINCHA_KETANAH, czc.minchaKetana, ZmanCategory.AFTERNOON, location))
            add(zman(PLAG_HAMINCHA, czc.plagHamincha, ZmanCategory.AFTERNOON, location))
            if (shabbatInfo.showCandleLighting) {
                add(zmanTime(CANDLE_LIGHTING, shabbatInfo.candleLightingTime, ZmanCategory.EVENING))
            }
            add(zman(SUNSET, czc.seaLevelSunset, ZmanCategory.EVENING, location))
            add(zman(NIGHTFALL, czc.nightfall, ZmanCategory.EVENING, location))
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
        return toLocalTime(czc.nightfall, location)
    }

    /**
     * Nightfall computed for [date] as an absolute instant. At high latitudes it can fall
     * after the following civil midnight, which [getTzaitTime] does not show.
     */
    fun getTzaitInstant(date: LocalDate, location: Location): Instant? =
        createCalendar(date, location).nightfall?.toInstant()

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
            toLocalTime(czc.alot, location)
        }

        val endTime = toLocalTime(czc.nightfall, location)

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
        val SOF_ZMAN_SHEMA_MGA = ZmanName("Sof Zman Shema (MGA)", "סוף זמן שמע (מג״א)")
        val SOF_ZMAN_SHEMA_GRA = ZmanName("Sof Zman Shema (GRA)", "סוף זמן שמע (גר״א)")
        val SOF_ZMAN_TEFILLAH_MGA = ZmanName("Sof Zman Tefillah (MGA)", "סוף זמן תפילה (מג״א)")
        val SOF_ZMAN_TEFILLAH_GRA = ZmanName("Sof Zman Tefillah (GRA)", "סוף זמן תפילה (גר״א)")
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
         * Latitude beyond which some day of the year has no sunrise, sunset, candle lighting or
         * nightfall: the sun's lowest point at the summer solstice, |latitude| + 23.44° − 90°,
         * stays above the 8.5° nightfall depression (≈ 58.06°), and from ≈ 65.7° there are
         * days without sunrise or sunset. Below it only the deeper Alot (16.1°) and
         * Misheyakir (11°) can be missing, which isn't flagged. Validated against a full-year
         * calculation in ZmanimHighLatitudeTest.
         */
        const val INCALCULABLE_TIMES_LATITUDE = 90.0 - 8.5 - 23.44

        /** Whether some times can't be calculated at [latitude] on some days of the year. */
        fun hasIncalculableTimes(latitude: Double): Boolean =
            kotlin.math.abs(latitude) > INCALCULABLE_TIMES_LATITUDE

        /**
         * Every zman name, in display order. The single source for zman names: the zmanim
         * screen, the day panel and Settings → Halachic Times all read from here.
         */
        val ZMAN_NAMES: List<ZmanName> = listOf(
            ALOT_HASHACHAR, MISHEYAKIR, SUNRISE,
            SOF_ZMAN_SHEMA_MGA, SOF_ZMAN_SHEMA_GRA, SOF_ZMAN_TEFILLAH_MGA, SOF_ZMAN_TEFILLAH_GRA,
            CHATZOT, MINCHA_GEDOLAH, MINCHA_KETANAH, PLAG_HAMINCHA,
            CANDLE_LIGHTING, SUNSET, NIGHTFALL, HAVDALAH, CHATZOT_HALAYLAH
        )

        /**
         * Persisted `selectedZmanim` keys of zmanim that were renamed, mapped to their
         * current English name, so a saved selection keeps working after a rename.
         */
        val RENAMED_ZMAN_KEYS: Map<String, String> = mapOf(
            "Sof Zman Tefillah" to SOF_ZMAN_TEFILLAH_GRA.english
        )
    }
}
