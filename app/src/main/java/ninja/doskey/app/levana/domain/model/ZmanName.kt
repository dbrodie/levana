package ninja.doskey.app.levana.domain.model

/**
 * A zman's name in English and Hebrew. [english] is also the key persisted in
 * `selectedZmanim`, so it must not change. The full list is
 * [ninja.doskey.app.levana.data.ZmanimRepository.ZMAN_NAMES].
 */
data class ZmanName(val english: String, val hebrew: String)

/** A zman name as shown: [primary] is the main line, [secondary] the smaller line under it. */
data class ZmanDisplayName(val primary: String, val secondary: String)

/** Hebrew first in Hebrew, English first otherwise. */
fun zmanDisplayName(english: String, hebrew: String, hebrewUi: Boolean): ZmanDisplayName =
    if (hebrewUi) ZmanDisplayName(hebrew, english) else ZmanDisplayName(english, hebrew)

fun ZmanName.displayName(hebrewUi: Boolean): ZmanDisplayName =
    zmanDisplayName(english, hebrew, hebrewUi)

fun ZmanTime.displayName(hebrewUi: Boolean): ZmanDisplayName =
    zmanDisplayName(name, hebrewName, hebrewUi)
