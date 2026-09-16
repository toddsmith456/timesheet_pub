# Time Sheet v1.0.0

First public release of Time Sheet — alarm-driven time tracking for Android.

## Highlights
- Jobs = alarms: name, color, time, repeat days
- Full-screen hours prompt when the alarm rings (works over lock screen), with quick picks, snooze & dismiss
- Day-grouped hour log with Today/Week/Month totals, per-job filters, edit/delete, manual entry
- CSV export via share sheet
- Settings: theme (system/light/dark), Material You dynamic color, alarm style (tone/vibrate/silent), tone picker, snooze config
- Exact alarms (AlarmManager.setAlarmClock), reboot restore, ringing foreground service
- Material 3 / Material You throughout; custom clock+paper adaptive icon

## Install
Download `TimeSheet-v1.0.0.apk` (also in `apk/`) and install it on any Android 8.0+ device (allow "install unknown apps").

## Build
JDK 17 + Android SDK 35: `./gradlew assembleRelease`
