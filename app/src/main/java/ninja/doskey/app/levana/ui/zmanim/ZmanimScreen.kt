package ninja.doskey.app.levana.ui.zmanim

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.ZmanCategory
import ninja.doskey.app.levana.domain.model.ZmanTime
import ninja.doskey.app.levana.domain.model.displayName
import ninja.doskey.app.levana.ui.daydetail.CityClockLabel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ZmanimScreen(initialDate: LocalDate? = null, modifier: Modifier = Modifier, viewModel: ZmanimViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(initialDate) { viewModel.onIntent(initialDate?.let { ZmanimIntent.LoadDate(it) } ?: ZmanimIntent.LoadDefault) }
    ZmanimContent(state, modifier)
}

@Composable
private fun ZmanimContent(state: ZmanimState, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val formatter = if (DateFormat.is24HourFormat(LocalContext.current)) DateTimeFormatter.ofPattern("HH:mm") else DateTimeFormatter.ofPattern("h:mm a")
    val grouped = state.zmanim.groupBy { it.category }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.otherClockCityName?.let { item(key = "clock") { CityClockLabel(it) } }
        ZmanCategory.entries.forEach { category ->
            val zmanim = grouped[category].orEmpty()
            if (zmanim.isNotEmpty()) item(key = category.name) { ZmanGroup(category, zmanim, formatter) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ZmanGroup(category: ZmanCategory, zmanim: List<ZmanTime>, formatter: DateTimeFormatter) {
    val label = stringResource(
        when (category) {
            ZmanCategory.MORNING -> R.string.zmanim_category_morning
            ZmanCategory.AFTERNOON -> R.string.zmanim_category_afternoon
            ZmanCategory.EVENING -> R.string.zmanim_category_evening
            ZmanCategory.NIGHT -> R.string.zmanim_category_night
        }
    )
    val hebrewUi = LocalConfiguration.current.locales[0].language in setOf("he", "iw")
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 10.dp))
            zmanim.forEachIndexed { index, zman ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    val name = zman.displayName(hebrewUi)
                    Column(Modifier.weight(1f)) {
                        Text(name.primary, style = MaterialTheme.typography.bodyLarge)
                        Text(name.secondary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(formatTime(zman.time, formatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatTime(time: LocalTime?, formatter: DateTimeFormatter): String = time?.format(formatter) ?: "—"
