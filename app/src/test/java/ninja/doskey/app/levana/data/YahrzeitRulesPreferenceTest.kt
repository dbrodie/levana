package ninja.doskey.app.levana.data

import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import org.junit.Assert.assertEquals
import org.junit.Test

class YahrzeitRulesPreferenceTest {

    @Test
    fun missingValuesGiveTheDefaults() {
        assertEquals(YahrzeitRules(), parseYahrzeitRules(null, null, null))
        assertEquals(YahrzeitLeapYearAdar.ADAR_II, YahrzeitRules().leapYearAdar)
        assertEquals(YahrzeitFirstYear.FOLLOW_ANNUAL, YahrzeitRules().firstYear)
        assertEquals(false, YahrzeitRules().adarI30AlsoNisan1)
    }

    @Test
    fun unrecognisedValuesFallBackToTheDefaults() {
        assertEquals(YahrzeitRules(), parseYahrzeitRules("ADAR_III", "SOMETIMES", null, "NEVER"))
        assertEquals(YahrzeitMissingDay.FIRST_ANNIVERSARY, YahrzeitRules().missingDay)
    }

    @Test
    fun everyStoredValueRoundTrips() {
        for (leap in YahrzeitLeapYearAdar.entries) for (first in YahrzeitFirstYear.entries) {
            for (nisan in listOf(false, true)) for (missing in YahrzeitMissingDay.entries) {
                assertEquals(
                    YahrzeitRules(leap, first, nisan, missing),
                    parseYahrzeitRules(leap.name, first.name, nisan, missing.name)
                )
            }
        }
    }
}
