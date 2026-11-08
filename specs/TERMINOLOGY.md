# Terminology and formatting

TASK-57.1 policy, 2026-11-08. Applies to app screens, accessibility labels, walkthroughs, widgets, Quick Settings, notifications and future store copy. Resource identifiers and stored values are implementation details, not editorial vocabulary.

## Language and naming

Owner decision: English UI uses transliteration for individual halachic times and English descriptions for features and controls. Candle lighting is the explicitly approved English exception. Hebrew UI uses Hebrew names only; omit redundant English secondary lines. English detailed zman rows may show Hebrew as a secondary line. Compact tiles use the documented short forms Netz, Shkia and Tzeit; full rows use Netz hachama, Shkia and Tzeit hakochavim. User-entered names and notes are never translated.

Use sentence case for headings, actions and labels, preserving proper names (Shabbat, Shema, Israel), acronyms (GRA/MGA, JSON/CSV) and the app brand. Do not uppercase section labels or quick-tile labels. Compact weekday abbreviations may remain uppercase. Transliteration uses Israeli/Sephardi tav (Shabbat, Bereshit, Toldot), ch for ח/כ, tz for צ, sh for ש; retain familiar spellings such as Chanukah, Pesach, Yahrzeit and Havdalah. Attached articles use lowercase ha (Alot hashachar, Plag hamincha). Proper holiday names retain their established capitalization.

| Concept | English | Hebrew |
|---|---|---|
| Times feature, card, settings and screen | Halachic times | זמני היום |
| Link to complete list | All halachic times | כל זמני היום |
| Dawn | Alot hashachar | עלות השחר |
| Earliest recognition | Misheyakir | משיכיר |
| Sunrise | Netz hachama (short: Netz) | הנץ החמה |
| Shema deadline | Sof zman Shema (GRA/MGA) | סוף זמן שמע (גר״א/מג״א) |
| Prayer deadline | Sof zman tefillah (GRA/MGA) | סוף זמן תפילה (גר״א/מג״א) |
| Midday | Chatzot hayom | חצות היום |
| Mincha | Mincha gedolah / Mincha ketanah | מנחה גדולה / מנחה קטנה |
| Plag | Plag hamincha | פלג המנחה |
| Candle lighting | Candle lighting | הדלקת נרות |
| Sunset | Shkia | שקיעה |
| Nightfall | Tzeit hakochavim (short: Tzeit) | צאת הכוכבים |
| Havdalah | Havdalah | הבדלה |
| Midnight | Chatzot halaylah | חצות הלילה |
| Torah portion category / named reading / plural | Torah reading / Parashat Vayera / Torah readings | פרשה / פרשת וירא / פרשות |
| Death anniversary | Yahrzeit | יארצייט |
| City management / saved list / action | Cities / Saved cities / Add city | ערים / ערים שמורות / הוספת עיר |
| Display choice | Selected city | העיר הנבחרת |
| Device position | Current location | המיקום הנוכחי |
| Modern Israeli observances | Modern Israeli holidays | מועדים לאומיים |

“Location” is reserved for physical position, permission and coordinates; “city” is a saved/display choice. Bundled city and country names are localized at presentation time, including saved cities, search results, zone labels and widgets. Searching either English or Hebrew names works. Persisted identity and coordinates do not change when the UI language changes. Geocoder/user names remain as supplied.

Retain יארצייט: it is an established, precise term for the app's Orthodox audience in Israel and the Diaspora; יום השנה alone also means other anniversaries, and יום זיכרון risks confusion with Yom HaZikaron. The review's X7 is not adopted. X8 is explicitly rejected: keep the existing Adar-rule toggle and its behavior. R4 (new calendar layout), time-format behavior and bidi fixes remain with their dedicated tasks; terminology changes do not force a 24-hour clock.

## Dates and numbers

English Hebrew months: Nisan, Iyar, Sivan, Tammuz, Av, Elul, Tishrei, Cheshvan, Kislev, Tevet, Shevat, Adar, Adar I, Adar II. Hebrew month spellings follow the shared HebrewDateText formatter (including חשון and ניסן). English full Hebrew dates are `20 Cheshvan 5787`, without a comma. Hebrew dates use letters with geresh/gershayim and the short year, `כ׳ חשון תשפ״ז`, as specified by TASK-57.25. Compact grid dates may omit month/year where their headers provide context. Gregorian dates use the UI locale; times follow the device's 12/24-hour setting. Numeric quantities use digits; a Hebrew calendar date uses Hebrew letters in Hebrew UI.

Parasha names use one explicit app-owned map rather than KosherJava's default Ashkenazi spellings. Compact calendar chips can show the name alone; agenda readings use `Parashat %s`; the day card has category `Torah reading` and the same name. Examples: Bereshit, Noach, Lech Lecha, Vayera, Chayei Sarah, Toldot.

## Store audit

Store copy is English only. The short description says “zmanim”; the full description uses “Zmanim” and “parsha”. At release, change these to “halachic times” and “Torah readings” to match the app. Shabbat and yahrzeit already follow the glossary. Terminology changes affect screenshots 1–8 and walkthrough text. No fastlane files are changed by this task; the coordinator refreshes them at release with owner approval.

## Naming research and owner decision

The owner approved transliterated individual zman names, English feature descriptions, Israeli/Sephardi transliteration and the Candle lighting exception. The initial English solar-name proposal is superseded.

- [Chabad](https://www.chabad.org/calendar/zmanim.htm) uses Halachic Times for navigation and pairs English and transliterated names in its list.
- [MyZmanim](https://www.myzmanim.com/infoline.aspx?country=US) distinguishes English names from transliterated Hebrew names (Sunrise / Neitz, Sunset / Shekiah).
- [ShulCloud example](https://sf.shulcloud.com/) uses Netz (Sunrise), Shkiah (Sunset) and Tzeit Hakochavim, among mixed labels.
- [Hebcal](https://www.hebcal.com/home/1663/zmanim-halachic-times-api) describes the feature as Zmanim (halachic times).
- [Zemaneh Yosef](https://royzmanim.com/) uses Worldwide Halachic Times as its English description.

There is no shared spelling standard across these products. Levana deliberately uses its own consistent glossary rather than mixing their pronunciation schemes. English explanations can describe sunrise, sunset or nightfall; primary time labels use the glossary.
