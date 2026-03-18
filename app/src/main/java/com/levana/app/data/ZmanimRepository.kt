package com.levana.app.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import com.levana.app.domain.model.Location
import com.levana.app.domain.model.ShabbatInfo
import com.levana.app.domain.model.ZmanCategory
import com.levana.app.domain.model.ZmanTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

class ZmanimRepository {

    fun getShabbatInfo(
        date: LocalDate,
        location: Location,
        candleLightingOffset: Double = 18.0
    ): ShabbatInfo {
        val jc = createJewishCalendar(date)
        val isErevShabbat = date.dayOfWeek == DayOfWeek.FRIDAY
        val isShabbat = date.dayOfWeek == DayOfWeek.SATURDAY
        val isErevYomTov = jc.isErevYomTov ||
            jc.isErevYomTovSheni
        val isYomTov = jc.isYomTov && !isErevYomTov

        val candleLightingTime = if (isErevShabbat || isErevYomTov) {
            val czc = createCalendar(date, location)
            czc.candleLightingOffset = candleLightingOffset
            toLocalTime(czc.candleLighting, location)
        } else {
            null
        }

        val havdalahTime = if (isShabbat || isYomTov) {
            val czc = createCalendar(date, location)
            toLocalTime(czc.tzais, location)
        } else {
            null
        }

        return ShabbatInfo(
            candleLightingTime = candleLightingTime,
            havdalahTime = havdalahTime,
            isErevShabbat = isErevShabbat,
            isShabbat = isShabbat,
            isErevYomTov = isErevYomTov,
            isYomTov = isYomTov
        )
    }

    fun hasCandles(date: LocalDate): Boolean {
        val jc = createJewishCalendar(date)
        return date.dayOfWeek == DayOfWeek.FRIDAY ||
            jc.isErevYomTov ||
            jc.isErevYomTovSheni
    }

    fun getZmanim(
        date: LocalDate,
        location: Location,
        candleLightingOffset: Double = 18.0
    ): List<ZmanTime> {
        val czc = createCalendar(date, location)
        val shabbatInfo = getShabbatInfo(
            date,
            location,
            candleLightingOffset
        )
        return buildList {
            add(
                zman(
                    "Alot HaShachar",
                    "עלות השחר",
                    czc.alosHashachar,
                    ZmanCategory.MORNING,
                    location
                )
            )
            add(
                zman(
                    "Misheyakir",
                    "משיכיר",
                    czc.misheyakir11Degrees,
                    ZmanCategory.MORNING,
                    location
                )
            )
            add(zman("Sunrise", "הנץ החמה", czc.sunrise, ZmanCategory.MORNING, location))
            add(
                zman(
                    "Sof Zman Shema (GRA)",
                    "סוף זמן שמע (גר״א)",
                    czc.sofZmanShmaGRA,
                    ZmanCategory.MORNING,
                    location
                )
            )
            add(
                zman(
                    "Sof Zman Shema (MGA)",
                    "סוף זמן שמע (מג״א)",
                    czc.sofZmanShmaMGA,
                    ZmanCategory.MORNING,
                    location
                )
            )
            add(
                zman(
                    "Sof Zman Tefillah",
                    "סוף זמן תפילה",
                    czc.sofZmanTfilaGRA,
                    ZmanCategory.MORNING,
                    location
                )
            )
            add(zman("Chatzot", "חצות היום", czc.chatzos, ZmanCategory.AFTERNOON, location))
            add(
                zman(
                    "Mincha Gedolah",
                    "מנחה גדולה",
                    czc.minchaGedola,
                    ZmanCategory.AFTERNOON,
                    location
                )
            )
            add(
                zman(
                    "Mincha Ketanah",
                    "מנחה קטנה",
                    czc.minchaKetana,
                    ZmanCategory.AFTERNOON,
                    location
                )
            )
            add(
                zman(
                    "Plag HaMincha",
                    "פלג המנחה",
                    czc.plagHamincha,
                    ZmanCategory.AFTERNOON,
                    location
                )
            )
            if (shabbatInfo.showCandleLighting) {
                add(
                    ZmanTime(
                        "Candle Lighting",
                        "הדלקת נרות",
                        shabbatInfo.candleLightingTime,
                        ZmanCategory.EVENING
                    )
                )
            }
            add(zman("Sunset", "שקיעה", czc.sunset, ZmanCategory.EVENING, location))
            add(zman("Nightfall", "צאת הכוכבים", czc.tzais, ZmanCategory.EVENING, location))
            if (shabbatInfo.showHavdalah) {
                add(
                    ZmanTime(
                        "Havdalah",
                        "הבדלה",
                        shabbatInfo.havdalahTime,
                        ZmanCategory.EVENING
                    )
                )
            }
            add(midnight(czc, location))
        }
    }

    fun getSunsetTime(date: LocalDate, location: Location): LocalTime? {
        val czc = createCalendar(date, location)
        return toLocalTime(czc.sunset, location)
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

    private fun createJewishCalendar(date: LocalDate): JewishCalendar {
        val gc = GregorianCalendar(
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        return JewishCalendar(gc)
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
        name: String,
        hebrew: String,
        date: Date?,
        category: ZmanCategory,
        location: Location
    ): ZmanTime {
        val time = date?.toInstant()
            ?.atZone(ZoneId.of(location.timezoneId))
            ?.toLocalTime()
        return ZmanTime(name, hebrew, time, category)
    }

    private fun midnight(czc: ComplexZmanimCalendar, location: Location): ZmanTime {
        return ZmanTime("Chatzot HaLaylah", "חצות הלילה", toLocalTime(czc.solarMidnight, location), ZmanCategory.NIGHT)
    }
}
