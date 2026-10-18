package ninja.doskey.app.levana.data

import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ZmanDisplayName
import ninja.doskey.app.levana.domain.model.displayName
import ninja.doskey.app.levana.domain.model.zmanDisplayName
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ZmanNamesTest {

    @Test
    fun `Hebrew UI shows the Hebrew name first and English under it`() {
        assertEquals(
            ZmanDisplayName("עלות השחר", "Alot HaShachar"),
            zmanDisplayName("Alot HaShachar", "עלות השחר", hebrewUi = true)
        )
        assertEquals(
            ZmanDisplayName("הדלקת נרות", "Candle Lighting"),
            ZmanimRepository.CANDLE_LIGHTING.displayName(hebrewUi = true)
        )
    }

    @Test
    fun `English UI is unchanged, English first and Hebrew under it`() {
        assertEquals(
            ZmanDisplayName("Alot HaShachar", "עלות השחר"),
            zmanDisplayName("Alot HaShachar", "עלות השחר", hebrewUi = false)
        )
        assertEquals(
            ZmanDisplayName("Havdalah", "הבדלה"),
            ZmanimRepository.HAVDALAH.displayName(hebrewUi = false)
        )
    }

    @Test
    fun `every zman comes from the single name list, in its order`() {
        // A Friday and a Shabbat, so candle lighting and havdalah are both included
        val repo = ZmanimRepository()
        val friday = repo.getZmanim(LocalDate.of(2026, 7, 24), Location.JERUSALEM)
        val shabbat = repo.getZmanim(LocalDate.of(2026, 7, 25), Location.JERUSALEM)
        val shown = (friday + shabbat).map { it.name to it.hebrewName }.toSet()
        val names = ZmanimRepository.ZMAN_NAMES.map { it.english to it.hebrew }

        assertEquals(names.toSet(), shown)
        assertEquals(names.map { it.first }.filter { it != "Havdalah" }, friday.map { it.name })

        val zman = shabbat.first { it.name == "Sunrise" }
        assertEquals(ZmanDisplayName("הנץ החמה", "Sunrise"), zman.displayName(hebrewUi = true))
    }
}
