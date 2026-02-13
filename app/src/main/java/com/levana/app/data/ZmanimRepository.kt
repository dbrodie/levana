package com.levana.app.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import com.levana.app.domain.model.Location
import com.levana.app.domain.model.ZmanCategory
import com.levana.app.domain.model.ZmanTime
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

class ZmanimRepository {

    fun getZmanim(date: LocalDate, location: Location): List<ZmanTime> {
        val czc = createCalendar(date, location)
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
            add(zman("Sunset", "שקיעה", czc.sunset, ZmanCategory.EVENING, location))
            add(zman("Nightfall", "צאת הכוכבים", czc.tzais, ZmanCategory.EVENING, location))
            add(midnight(czc, location))
        }
    }

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
        val sunset = czc.sunset
        val nextSunrise = czc.sunrise
        val time = if (sunset != null && nextSunrise != null) {
            val midMs = (sunset.time + nextSunrise.time) / 2
            Date(midMs).toInstant()
                .atZone(ZoneId.of(location.timezoneId))
                .toLocalTime()
        } else {
            null
        }
        return ZmanTime(
            "Chatzot HaLaylah",
            "חצות הלילה",
            time,
            ZmanCategory.NIGHT
        )
    }
}
