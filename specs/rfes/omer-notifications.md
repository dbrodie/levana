# Omer Notification: Tzait + Morning Reminder

## Summary

Improve the Sefirat HaOmer reminder with two changes: fix the evening notification to fire at tzait (nightfall) instead of sunset, and add an optional morning reminder at a user-configurable time.

## Motivation

The existing Omer notification fired at sunset, which is halachically early — the omer is counted after nightfall (tzait hakokhavim). The single notification also gave users no flexibility: if they missed the evening reminder, there was no morning follow-up. This feature corrects the timing and adds a second optional reminder.

## Changes

### `data/ZmanimRepository.kt`
- Add `getTzaitTime(date, location)` using `czc.tzais` (same source as havdalah time).

### `domain/model/UserPreferences.kt`
- Add `notifyOmerTzait: Boolean = true` — controls the evening (tzait) notification.
- Add `notifyOmerMorning: Boolean = false` — controls the morning reminder.
- Add `notifyOmerMorningTime: Int = 420` — morning time in minutes since midnight (420 = 7:00 AM), same convention as `candleLightingMorningTime`.

### `data/PreferencesRepository.kt`
- Add DataStore keys: `NOTIFY_OMER_TZAIT`, `NOTIFY_OMER_MORNING`, `NOTIFY_OMER_MORNING_TIME`.
- Map new fields in the preferences flow.
- Add `saveNotifyOmerTzait`, `saveNotifyOmerMorning`, `saveNotifyOmerMorningTime` save methods.

### `notifications/NotificationAlarmScheduler.kt`
- Add `ACTION_OMER_MORNING = "com.levana.app.OMER_MORNING_ALARM"` and `REQUEST_OMER_MORNING = 201000`.
- Add `scheduleOmerMorningReminder(date, morningTime, omerDay)` using `ZoneId.systemDefault()` (wall-clock time, no location needed).
- Update `cancelAll()` to cancel morning omer alarms alongside evening ones.

### `notifications/NotificationAlarmReceiver.kt`
- Handle `ACTION_OMER_MORNING` → call `NotificationPoster.postOmerMorning()`.

### `notifications/NotificationPoster.kt`
- Add `postOmerMorning()` with "Today is day X of the Omer" text (vs. "Tonight" for the evening).
- Uses notification ID `notificationId(CHANNEL_OMER_ORDINAL, date) + 1` to avoid collision with the evening notification.

### `notifications/DailyNotificationWorker.kt`
- Update `handleOmer()` to accept `prefs` parameter.
- When `notifyOmerTzait`: fetch tzait time and schedule evening alarm.
- When `notifyOmerMorning`: construct `LocalTime` from `notifyOmerMorningTime` and schedule morning alarm.

### Settings UI (`ui/settings/`)
- `SettingsState`: add `notifyOmerTzait`, `notifyOmerMorning`, `notifyOmerMorningTime`.
- `SettingsIntent`: add `SetNotifyOmerTzait`, `SetNotifyOmerMorning`, `SetNotifyOmerMorningTime`.
- `SettingsViewModel`: handle new intents; map new prefs in `observePreferences`.
- `SettingsScreen`: expand the omer section:
  - Master toggle label changes from "Omer (at sunset)" to "Sefirat HaOmer".
  - When master is ON, show two indented sub-toggles: "At tzait (nightfall)" and "Morning reminder".
  - When morning is ON, show an indented time-adjuster row (±30 min, range 6 AM–12 PM).

## Notification Text

| Trigger | Title | Body |
|---|---|---|
| Evening (tzait) | Sefirat HaOmer | Tonight is day X of the Omer [weeks/days] |
| Morning | Sefirat HaOmer | Today is day X of the Omer [weeks/days] |
