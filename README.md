# Pulse

Pulse is a native Android alarm app focused on tactile, customizable reminders.

## Included

- One-time, daily, weekday, weekend, custom-day, and interval alarms.
- Vibration patterns such as `d2s p2s d1s p3s d2s` (`d` = duration, `p` = pause).
- Touch recorder that vibrates while the pad is held and converts each hold/release gap into `d`/`p` segments.
- Random time windows, random patterns, and random vibration intensity per occurrence.
- `AlarmManager` delivery with boot, timezone, and clock-change rescheduling.
- Waveform vibration directly from a background `BroadcastReceiver`, so the app does not need to stay open.
- A test button for previewing a pattern before saving.

## Build

Open the folder in Android Studio and run the `app` configuration, or use an installed Gradle 9.x runtime:

```text
gradle :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

On Android 12+, allow Pulse under **Alarms & reminders** for reliable timing. Pulse uses Android's alarm-clock scheduling path when that access is available, and restores alarms after reboot and after exact-alarm access is granted. Without the access, Android may delay alarms, especially under battery-saving modes. Scheduled alarms run without Pulse staying in memory. An alarm cannot fire while the phone is powered off, and force-stopping Pulse in Android settings can prevent delivery until the app is opened again. Vibration strength is bounded by the phone's motor and system settings.
