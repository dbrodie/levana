package ninja.doskey.app.levana.ui.datepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The event and birthday editors' date: the Hebrew date with its Gregorian day, opening
 * the date picker when tapped.
 */
@Composable
fun HebrewDateField(
    label: String,
    hebrewDate: String,
    gregorianDate: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .clickable(onClickLabel = stringResource(R.string.date_picker_change_date), role = Role.Button, onClick = onClick)
                .padding(16.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(hebrewDate, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        gregorianDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
