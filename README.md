# ⏰ Time Sheet

**Alarm-driven time tracking for Android.** Set an alarm for each job or project — when it rings, a full-screen prompt asks how many hours you worked. Type a number, hit submit, and the hours are logged for the day. No timers to remember to start and stop.

Built with **Jetpack Compose** and a full **Material 3 / Material You** design: dynamic wallpaper colors on Android 12+, dark & light themes, rounded shape language throughout, and a custom clock-and-paper adaptive icon (with themed-icon monochrome layer).

---

## How it works

1. **Jobs tab** — create a job (name + color) and give it an alarm schedule: a time and repeat days (e.g. *Weekdays 5:30 PM*). Each job **is** an alarm.
2. **When the alarm rings** — a full-screen card shows the job, a live clock, and an hours field with quick picks (2 / 4 / 6 / 8 h). Snooze or dismiss if now's not the time.
3. **Submit** — the hours are instantly written to that day in your log. The next occurrence is scheduled automatically.
4. **Hour Log tab** — every day, card by card, with per-day totals, a Today / Week / Month summary, per-job filters, tap-to-edit entries, manual entry, and CSV export.
5. **Settings tab** — theme (system / light / dark), Material You dynamic color, alarm style (**Tone / Vibrate / Silent**), alarm tone picker, snooze configuration, CSV export, and clear-all-data.

## Features

- ⏰ Exact alarms via `AlarmManager.setAlarmClock` — survives Doze, shows the system alarm icon
- 📴 Full-screen intent + heads-up notification with **Snooze** / **Dismiss** actions (works on the lock screen)
- 🔁 Per-job repeat days (Mon–Sun), one-time alarms supported
- 🧮 Decimal hours (7.5, 8.25…) with sanitizing input + half-hour steppers
- 📅 Day-grouped history that survives job renames/deletes (entries snapshot job name & color)
- 📤 CSV export through the system share sheet
- 🔊 Ringing foreground service: looping tone and/or vibration, wake lock, reboot restore
- 🎨 Material 3 everywhere: dynamic color, tonal surfaces, rounded 22–30 dp cards & sheets
- 🌗 Dark / light / system themes
- 🔒 100% local & offline — Room database + DataStore, zero network permissions

## Icon

![Time Sheet icon](docs/icon-preview.png)

A timesheet page with a 5-o'clock clock on the corner — *the workday is done, log your hours*. Adaptive icon with gradient background + themed-icon monochrome layer.

## Requirements

- Android 8.0 (API 26) or newer
- On Android 13+, allow notifications when first prompted
- On Android 12+, grant the *Alarms & reminders* special access if the app asks (a banner appears in the Jobs tab when it's missing)

## Build from source

```bash
# JDK 17 required
./gradlew assembleRelease        # release APK (debug-signed unless keystore.properties exists)
./gradlew test                   # unit tests
```

To sign release builds, create `keystore.properties` in the repo root (see `keystore.properties.example`) and point `storeFile` at your keystore. CI builds automatically fall back to debug signing when the file is absent.

### Tech stack

| Layer      | Library                                        |
|------------|------------------------------------------------|
| UI         | Jetpack Compose + Material 3 (dynamic color)   |
| Persistence| Room, DataStore Preferences                    |
| Alarms     | AlarmManager (exact), foreground service       |
| Async      | Kotlin Coroutines + Flow                       |
| DI         | Hand-rolled `AppContainer`                     |

## Project structure

```
app/src/main/java/com/timesheet/app/
├── MainActivity.kt          # 3-tab home (Jobs / Hour Log / Settings)
├── alarm/                   # scheduler, receivers, ringing service, full-screen activity
├── data/                    # Room DB, models, settings (DataStore), repository
├── ui/                      # Compose screens, view models, theme, shared components
└── util/                    # scheduling math, formatting, CSV
```

## License

MIT — see [LICENSE](LICENSE).
