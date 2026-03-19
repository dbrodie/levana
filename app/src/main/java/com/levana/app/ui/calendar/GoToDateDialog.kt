package com.levana.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.GregorianCalendar
import java.util.Locale

@Composable
fun GoToDateDialog(
    initialDate: LocalDate,
    calendarHebrewMode: Boolean,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initJd = remember(initialDate) {
        JewishDate(
            GregorianCalendar(
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth
            )
        )
    }

    var hebrewYear by remember { mutableIntStateOf(initJd.jewishYear) }
    var hebrewMonth by remember { mutableIntStateOf(initJd.jewishMonth) }
    var hebrewDay by remember { mutableIntStateOf(initJd.jewishDayOfMonth) }

    var gregYear by remember { mutableIntStateOf(initialDate.year) }
    var gregMonth by remember { mutableIntStateOf(initialDate.monthValue) }
    var gregDay by remember { mutableIntStateOf(initialDate.dayOfMonth) }

    fun syncHebrewToGregorian() {
        try {
            val daysInHebMonth = JewishDate(hebrewYear, hebrewMonth, 1).daysInJewishMonth
            val clampedDay = hebrewDay.coerceIn(1, daysInHebMonth)
            hebrewDay = clampedDay
            val gc = JewishDate(hebrewYear, hebrewMonth, clampedDay).gregorianCalendar
            gregYear = gc.get(java.util.Calendar.YEAR)
            gregMonth = gc.get(java.util.Calendar.MONTH) + 1
            gregDay = gc.get(java.util.Calendar.DAY_OF_MONTH)
        } catch (_: Exception) { }
    }

    fun syncGregorianToHebrew() {
        try {
            val daysInGregMonth = java.time.YearMonth.of(gregYear, gregMonth).lengthOfMonth()
            val clampedDay = gregDay.coerceIn(1, daysInGregMonth)
            gregDay = clampedDay
            val jd = JewishDate(
                GregorianCalendar(gregYear, gregMonth - 1, clampedDay)
            )
            hebrewYear = jd.jewishYear
            hebrewMonth = jd.jewishMonth
            hebrewDay = jd.jewishDayOfMonth
        } catch (_: Exception) { }
    }

    val hebrewSection: @Composable () -> Unit = {
        Text(
            text = "Hebrew Date",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        HebrewPickerRow(
            hebrewDay = hebrewDay,
            hebrewMonth = hebrewMonth,
            hebrewYear = hebrewYear,
            onDayChange = { hebrewDay = it; syncHebrewToGregorian() },
            onMonthChange = { hebrewMonth = it; syncHebrewToGregorian() },
            onYearChange = { hebrewYear = it; syncHebrewToGregorian() }
        )
    }

    val gregorianSection: @Composable () -> Unit = {
        Text(
            text = "Gregorian Date",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        GregorianPickerRow(
            gregDay = gregDay,
            gregMonth = gregMonth,
            gregYear = gregYear,
            onDayChange = { gregDay = it; syncGregorianToHebrew() },
            onMonthChange = { gregMonth = it; syncGregorianToHebrew() },
            onYearChange = { gregYear = it; syncGregorianToHebrew() }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Date") },
        text = {
            Column {
                if (calendarHebrewMode) {
                    hebrewSection()
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    gregorianSection()
                } else {
                    gregorianSection()
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    hebrewSection()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val daysInGregMonth =
                            java.time.YearMonth.of(gregYear, gregMonth).lengthOfMonth()
                        val safeDay = gregDay.coerceIn(1, daysInGregMonth)
                        onConfirm(LocalDate.of(gregYear, gregMonth, safeDay))
                    } catch (_: Exception) { }
                }
            ) {
                Text("Go to Date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HebrewPickerRow(
    hebrewDay: Int,
    hebrewMonth: Int,
    hebrewYear: Int,
    onDayChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit
) {
    val isLeapYear = try {
        JewishDate(hebrewYear, JewishDate.TISHREI, 1).isJewishLeapYear
    } catch (_: Exception) {
        false
    }
    val months = buildHebrewMonthList(isLeapYear)
    val daysInMonth = try {
        JewishDate(hebrewYear, hebrewMonth, 1).daysInJewishMonth
    } catch (_: Exception) {
        30
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Day", style = MaterialTheme.typography.labelMedium)
            SpinnerPicker(
                value = hebrewDay.coerceIn(1, daysInMonth),
                range = 1..daysInMonth,
                onValueChange = onDayChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(modifier = Modifier.weight(2f)) {
            Text("Month", style = MaterialTheme.typography.labelMedium)
            HebrewMonthSpinner(
                selectedMonth = hebrewMonth,
                months = months,
                onMonthChange = onMonthChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text("Year", style = MaterialTheme.typography.labelMedium)
            SpinnerPicker(
                value = hebrewYear,
                range = 5700..5900,
                onValueChange = onYearChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GregorianPickerRow(
    gregDay: Int,
    gregMonth: Int,
    gregYear: Int,
    onDayChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit
) {
    val daysInMonth = try {
        java.time.YearMonth.of(gregYear, gregMonth).lengthOfMonth()
    } catch (_: Exception) {
        31
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Day", style = MaterialTheme.typography.labelMedium)
            SpinnerPicker(
                value = gregDay.coerceIn(1, daysInMonth),
                range = 1..daysInMonth,
                onValueChange = onDayChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(modifier = Modifier.weight(2f)) {
            Text("Month", style = MaterialTheme.typography.labelMedium)
            GregorianMonthSpinner(
                selectedMonth = gregMonth,
                onMonthChange = onMonthChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text("Year", style = MaterialTheme.typography.labelMedium)
            SpinnerPicker(
                value = gregYear,
                range = 1940..2140,
                onValueChange = onYearChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class HebrewMonthOption(val jewishDateMonth: Int, val displayName: String)

private fun buildHebrewMonthList(isLeapYear: Boolean): List<HebrewMonthOption> = buildList {
    add(HebrewMonthOption(JewishDate.TISHREI, "Tishrei"))
    add(HebrewMonthOption(JewishDate.CHESHVAN, "Cheshvan"))
    add(HebrewMonthOption(JewishDate.KISLEV, "Kislev"))
    add(HebrewMonthOption(JewishDate.TEVES, "Teves"))
    add(HebrewMonthOption(JewishDate.SHEVAT, "Shevat"))
    if (isLeapYear) {
        add(HebrewMonthOption(JewishDate.ADAR, "Adar I"))
        add(HebrewMonthOption(JewishDate.ADAR_II, "Adar II"))
    } else {
        add(HebrewMonthOption(JewishDate.ADAR, "Adar"))
    }
    add(HebrewMonthOption(JewishDate.NISSAN, "Nissan"))
    add(HebrewMonthOption(JewishDate.IYAR, "Iyar"))
    add(HebrewMonthOption(JewishDate.SIVAN, "Sivan"))
    add(HebrewMonthOption(JewishDate.TAMMUZ, "Tammuz"))
    add(HebrewMonthOption(JewishDate.AV, "Av"))
    add(HebrewMonthOption(JewishDate.ELUL, "Elul"))
}

@Composable
private fun SpinnerPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            enabled = value > range.first
        ) {
            Text("\u2212", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(2.dp))
        androidx.compose.material3.IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            enabled = value < range.last
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HebrewMonthSpinner(
    selectedMonth: Int,
    months: List<HebrewMonthOption>,
    onMonthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = months.indexOfFirst { it.jewishDateMonth == selectedMonth }
        .coerceAtLeast(0)
    val currentMonth = months[currentIndex]

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.IconButton(
            onClick = {
                if (currentIndex > 0) onMonthChange(months[currentIndex - 1].jewishDateMonth)
            },
            enabled = currentIndex > 0
        ) {
            Text("\u2212", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = currentMonth.displayName,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.IconButton(
            onClick = {
                if (currentIndex < months.size - 1) {
                    onMonthChange(months[currentIndex + 1].jewishDateMonth)
                }
            },
            enabled = currentIndex < months.size - 1
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun GregorianMonthSpinner(
    selectedMonth: Int,
    onMonthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = Month.of(selectedMonth)
        .getDisplayName(TextStyle.FULL, Locale.getDefault())

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.IconButton(
            onClick = { if (selectedMonth > 1) onMonthChange(selectedMonth - 1) },
            enabled = selectedMonth > 1
        ) {
            Text("\u2212", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = monthName,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp)
        )
        androidx.compose.material3.IconButton(
            onClick = { if (selectedMonth < 12) onMonthChange(selectedMonth + 1) },
            enabled = selectedMonth < 12
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}
