# Levana - Hebrew Calendar App

## Product Specification Document

**Version:** 1.0
**Status:** Draft

---

## 1. Overview

### 1.1 Product Vision

Levana is an open-source Android Hebrew calendar application designed for Orthodox Jewish users. It provides comprehensive Jewish calendar functionality including zmanim (halachic times), holiday information, Torah readings, and personal event tracking. The app operates fully offline and supports multiple minhagim (customs) for both Israel and Diaspora users.

### 1.2 Target Audience

- **Primary:** Orthodox Jewish users
- **Geographic:** Both Israel and Diaspora communities
- **Minhagim:** All major traditions (Ashkenazi, Sephardi/Edot HaMizrach, Yemenite, Chabad/Lubavitch, and others)

### 1.3 Key Differentiators

- Complete offline functionality
- Material Design 3 with modern Android technologies
- Comprehensive minhag support with configurable zmanim calculations
- Dynamic theming based on Jewish holidays

---

## 2. Technical Requirements

### 2.1 Platform

- **Target OS:** Android
- **Minimum API Level:** 34 (Android 14)
  - Note: Evaluate lowering API level in future versions to increase reach, while maintaining feature parity
- **Architecture:** Modern Android stack

### 2.2 Technology Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture Pattern:** MVVM with Clean Architecture principles
- **Dependency Injection:** Hilt
- **Local Storage:** Room Database
- **Async Operations:** Kotlin Coroutines and Flow
- **Build System:** Gradle with Kotlin DSL

### 2.3 Offline Requirements

- All calendar calculations must work without network connectivity
- Required data must be bundled within the APK:
  - City/location database
  - Holiday definitions
  - Torah reading schedules
  - Zmanim calculation algorithms
- No external API dependencies for core functionality

---

## 3. Core Features

### 3.1 Calendar Display

#### 3.1.1 Dual Calendar System

The app must support two primary viewing modes:

1. **Hebrew-Primary View:** Hebrew date prominently displayed with corresponding Gregorian date
2. **Gregorian-Primary View:** Gregorian date prominently displayed with corresponding Hebrew date

#### 3.1.2 Calendar Views

- Monthly calendar view with Hebrew/Gregorian dates
- Daily detail view
- Week view (optional, for future consideration)

#### 3.1.3 Date Information Display

Each date should show (where applicable):
- Hebrew date (day, month, year)
- Gregorian date
- Day of week (in Hebrew and/or local language)
- Holiday/special day indication
- Parsha of the week (on Shabbat)
- Omer count (during Sefirat HaOmer)

### 3.2 Zmanim (Halachic Times)

#### 3.2.1 Supported Zmanim

The app should display all commonly used zmanim, including but not limited to:

- Alot HaShachar (dawn)
- Misheyakir (earliest tallit/tefillin)
- Netz HaChama (sunrise)
- Sof Zman Shema (latest Shema)
- Sof Zman Tefillah (latest Shacharit)
- Chatzot HaYom (midday)
- Mincha Gedolah (earliest Mincha)
- Mincha Ketanah
- Plag HaMincha
- Shkiah (sunset)
- Tzet HaKochavim (nightfall)
- Chatzot HaLaylah (midnight)

#### 3.2.2 Calculation Methods

Users must be able to configure zmanim calculation methods independently from general minhag settings:

- **Temporal hours calculation:** Magen Avraham vs. GR"A (Vilna Gaon)
- **Degrees below horizon:** Configurable for various zmanim
- **Rabbeinu Tam:** Option for tzet hakochavim
- **Custom configurations:** Allow advanced users to set specific degrees/times

#### 3.2.3 Location-Based Calculations

- All zmanim calculated based on user's selected location
- Location selection methods (see Section 3.3)

### 3.3 Location Management

#### 3.3.1 Location Selection Methods

1. **City Database:** Bundled database of cities worldwide with coordinates and timezone data
2. **GPS Detection:** Use device GPS to determine current location
3. **Manual Entry:** Allow manual latitude/longitude input

#### 3.3.2 Location Storage

- Single active location at a time
- Persist selected location across app sessions
- Store timezone information with location

### 3.4 Shabbat and Candle Lighting

#### 3.4.1 Candle Lighting Times

- Calculate candle lighting time based on location
- Configurable offset before sunset (default: 18 minutes, configurable per minhag)
- Special handling for Jerusalem (40 minutes minhag option)

#### 3.4.2 Havdalah Times

- Calculate based on configured tzet hakochavim method
- Display on Saturday night and after holidays

### 3.5 Jewish Holidays

#### 3.5.1 Holiday Categories

All holidays should be displayed with the ability to filter:

1. **Torah Holidays:** Pesach, Shavuot, Sukkot, Rosh Hashanah, Yom Kippur
2. **Rabbinic Holidays:** Purim, Chanukah
3. **Fast Days:** Tisha B'Av, Tzom Gedaliah, Asara B'Tevet, Ta'anit Esther, Shiva Asar B'Tammuz
4. **Minor Observances:** Rosh Chodesh, Tu B'Shvat, Lag B'Omer, Tu B'Av
5. **Modern Israeli Holidays (toggleable):** Yom HaShoah, Yom HaZikaron, Yom HaAtzmaut, Yom Yerushalayim

#### 3.5.2 Israel vs. Diaspora

- Automatically adjust holiday observance based on location setting:
  - One day vs. two days for Yom Tov
  - Parsha readings alignment
- User override option for travelers

### 3.6 Torah Readings

#### 3.6.1 Parsha (Weekly Portion)

- Display current week's parsha
- Handle double parshiot correctly
- Adjust for Israel/Diaspora differences

#### 3.6.2 Special Readings

- Indicate special maftir readings (Shekalim, Zachor, Parah, HaChodesh)
- Holiday Torah readings

### 3.7 Omer Counting

- Display current Omer count during Sefirat HaOmer period
- Show count in days and weeks format
- Notification reminder option (see Section 5)

### 3.8 Molad (New Moon)

- Calculate and display molad for each Hebrew month
- Show announcement text for Shabbat Mevarchim

---

## 4. Personal Events

### 4.1 Event Types

#### 4.1.1 Jewish Birthdays

- Store birthdays according to Hebrew date
- Annual recurrence calculation
- Link to Android contacts via custom field

#### 4.1.2 Yahrzeits (Memorial Anniversaries)

- Store yahrzeit date according to Hebrew date
- Handle leap year calculations correctly (Adar I/II)
- Annual recurrence based on halachic rules

### 4.2 Contacts Integration

#### 4.2.1 Implementation Approach

- Extend Android contacts using custom MIME type fields
- Store Hebrew date information in contact records
- Sync personal events with contact data

#### 4.2.2 Data Fields

For each contact, optionally store:
- Hebrew birthday (day, month, year)

### 4.3 Android Calendar Integration

#### 4.3.1 Calendar Selection

- Display list of calendars available on device
- Allow user to select which calendars to show
- Persist calendar visibility preferences

#### 4.3.2 Event Display

- Show selected calendar events on appropriate dates
- Read-only integration (no event creation/modification)
- Respect calendar colors and styling

---

## 5. Notifications

### 5.1 Notification Types

#### 5.1.1 Candle Lighting

- Configurable reminder before candle lighting time
- Customizable advance notice (e.g., 30 minutes, 1 hour before)
- Option to disable

#### 5.1.2 Holiday Notifications

- Notify before holiday begins
- Configurable timing
- Select which holidays trigger notifications

#### 5.1.3 Personal Event Notifications

- Birthday reminders
- Yahrzeit reminders
- Configurable advance notice (day before, week before, etc.)

#### 5.1.4 Omer Counting Reminder

- Daily reminder during Sefirat HaOmer
- Configurable time (after tzet hakochavim)

### 5.2 Notification Configuration

- Global enable/disable
- Per-category enable/disable
- Custom timing for each notification type
- Quiet hours support (respect system DND settings)

---

## 6. User Interface

### 6.1 Design System

#### 6.1.1 Material Design 3

- Full implementation of Material 3 components
- Dynamic color support (Material You)
- Proper elevation and surface treatments

#### 6.1.2 Dynamic Holiday Theming

Automatically adjust app accent colors based on current/upcoming holidays:

| Holiday/Period | Suggested Theme Color |
|---------------|----------------------|
| Regular days | User preference / Material You |
| Shabbat |  |
| Rosh Hashanah | White/Gold |
| Yom Kippur | White/Cream |
| Sukkot | Green |
| Shemini Atzeret / Simchat Torah | Deep Purple |
| Tu Bishvat | Green/ |
| Chanukah | Blue/Silver |
| Purim | Purple/Multicolor |
| Pesach | Orange/Red |
| Yom HaAtzmaut | Blue/White |
| Shavuot | Yellow/Green |
| Tisha B'Av / Fast Days | Muted/Gray |

- User option to disable dynamic theming

### 6.2 Accessibility

#### 6.2.1 Requirements

- Full TalkBack support with meaningful content descriptions
- Sufficient color contrast ratios (WCAG AA minimum)
- Touch targets minimum 48dp
- Scalable text supporting system font size
- Screen reader-friendly date announcements

#### 6.2.2 Navigation

- Logical focus order
- Keyboard navigation support
- Clear visual focus indicators

### 6.3 Localization

#### 6.3.1 Supported Languages

- Hebrew (עברית)
- System default language
- English fallback


#### 6.3.2 Language Selection

- User preference setting
- Options:
  - Hebrew
  - System language
- Hebrew selection activates RTL layout

#### 6.3.3 RTL Support

- Full RTL layout when Hebrew is selected
- Proper mirroring of UI elements
- Bidirectional text handling for mixed content

---

## 7. Settings

### 7.1 Settings Categories

#### 7.1.1 Location Settings

- Current location display
- Location selection (city search, GPS, manual)
- Timezone display/override

#### 7.1.2 Minhag Settings

- General minhag selection (affects various defaults)
- Israel/Diaspora toggle
- Individual zmanim calculation method overrides

#### 7.1.3 Display Settings

- Hebrew-primary vs. Gregorian-primary view
- Modern Israeli holidays toggle
- Calendar display preferences

#### 7.1.4 Notification Settings

- Master notification toggle
- Per-category notification configuration
- Timing preferences

#### 7.1.5 Appearance Settings

- Language selection
- Dynamic holiday theme toggle
- Dark/Light/System theme preference

#### 7.1.6 About

- App version
- License information (GPLv3)
- Open source acknowledgments
- Link to source code repository

---

## 8. Data Architecture

### 8.1 Bundled Data

The following data must be included in the APK:

#### 8.1.1 Location Database

- Worldwide city database with:
  - City name (Hebrew and English)
  - Country
  - Latitude/Longitude
  - Timezone identifier
- Searchable by city name

#### 8.1.2 Calendar Data

- Hebrew calendar calculation algorithms
- Holiday definitions and rules
- Parsha reading schedule
- Omer counting data
- Molad calculation algorithms

### 8.2 User Data (Local Storage)

- Selected location
- User preferences (all settings)
- Personal events (birthdays, yahrzeits)
- Notification preferences

### 8.3 External Data (Read-Only)

- Android Contacts (for birthday/yahrzeit linking)
- Android Calendar (for event display)

---

## 9. Future Considerations

The following features are explicitly out of scope for the initial release but may be considered for future versions:

### 9.1 Potential Future Features

- Widgets (home screen widgets for date, zmanim, etc.)
- WearOS support
- Daf Yomi and other daily study schedules
- Expanded holiday descriptions and laws
- Multiple saved locations
- Lower Android API support
- Additional languages
- Siddur/Tefillah integration
- Community features

### 9.2 Architecture Considerations

Design the architecture to accommodate:
- Plugin system for additional minhagim
- Modular content packages
- Extensible notification system

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| Alot HaShachar | Dawn; earliest time for certain prayers |
| Chatzot | Midday or midnight, depending on context |
| Diaspora | Jewish communities outside of Israel |
| GR"A | Vilna Gaon; a method of calculating halachic times |
| Havdalah | Ceremony marking the end of Shabbat/holidays |
| Minhag (pl. Minhagim) | Custom or tradition |
| Molad | The "birth" of the new moon |
| Netz | Sunrise |
| Parsha | Weekly Torah portion |
| Sefirat HaOmer | 49-day counting period between Pesach and Shavuot |
| Shkiah | Sunset |
| Tzet HaKochavim | Nightfall; when stars become visible |
| Yahrzeit | Anniversary of a death |
| Zmanim | Halachic times for prayers and observances |

---

*Document generated for agentic AI development. This specification should be used to create the technical architecture and implementation roadmap.*
