package ninja.doskey.app.levana.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * The app's "today": the Developer Settings date override when one is set,
 * otherwise the real date.
 */
fun interface TodayProvider {
    suspend fun today(): LocalDate
}

class PreferencesTodayProvider(
    private val preferencesRepository: PreferencesRepository
) : TodayProvider {
    override suspend fun today(): LocalDate =
        preferencesRepository.preferences.first().devDateOverride ?: LocalDate.now()
}
