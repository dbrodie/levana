package com.levana.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
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
            DayYearField(
                value = hebrewDay.coerceIn(1, daysInMonth),
                range = 1..daysInMonth,
                onValueChange = onDayChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(modifier = Modifier.weight(2f)) {
            Text("Month", style = MaterialTheme.typography.labelMedium)
            HebrewMonthDropdown(
                selectedMonth = hebrewMonth,
                months = months,
                onMonthChange = onMonthChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text("Year", style = MaterialTheme.typography.labelMedium)
            DayYearField(
                value = hebrewYear,
                range = 5700..5900,
                onValueChange = onYearChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            DayYearField(
                value = gregDay.coerceIn(1, daysInMonth),
                range = 1..daysInMonth,
                onValueChange = onDayChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(modifier = Modifier.weight(2f)) {
            Text("Month", style = MaterialTheme.typography.labelMedium)
            GregorianMonthDropdown(
                selectedMonth = gregMonth,
                onMonthChange = onMonthChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text("Year", style = MaterialTheme.typography.labelMedium)
            DayYearField(
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
private fun DayYearField(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            val parsed = input.toIntOrNull()
            if (parsed != null && parsed in range) {
                onValueChange(parsed)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GregorianMonthDropdown(
    selectedMonth: Int,
    onMonthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val monthName = Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.getDefault())

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = monthName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (1..12).forEach { month ->
                val name = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onMonthChange(month)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HebrewMonthDropdown(
    selectedMonth: Int,
    months: List<HebrewMonthOption>,
    onMonthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = months.firstOrNull { it.jewishDateMonth == selectedMonth }?.displayName
        ?: months.first().displayName

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onMonthChange(option.jewishDateMonth)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
