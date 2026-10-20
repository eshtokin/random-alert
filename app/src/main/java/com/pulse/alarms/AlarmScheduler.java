package com.pulse.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Random;

public final class AlarmScheduler {
    private static final Random RANDOM = new Random();

    private AlarmScheduler() {}

    public static void schedule(Context context, Alarm alarm) {
        cancel(context, alarm.id);
        // Recalculate from the user's configured time; nextTriggerMillis may be a
        // stale value from an earlier version or an edited alarm.
        alarm.nextTriggerMillis = 0;
        alarm.nextTriggerMillis = nextTrigger(alarm, System.currentTimeMillis());
        AlarmStore.upsert(context, alarm);
        arm(context, alarm, alarm.nextTriggerMillis);
    }

    public static void scheduleNext(Context context, Alarm alarm, long afterMillis) {
        if (Alarm.ONCE.equals(alarm.repeatMode)) {
            alarm.enabled = false;
            alarm.nextTriggerMillis = 0;
            AlarmStore.upsert(context, alarm);
            return;
        }
        scheduleAt(context, alarm, nextTrigger(alarm, afterMillis));
    }

    private static void scheduleAt(Context context, Alarm alarm, long trigger) {
        cancel(context, alarm.id);
        alarm.nextTriggerMillis = trigger;
        AlarmStore.upsert(context, alarm);
        arm(context, alarm, trigger);
    }

    private static void arm(Context context, Alarm alarm, long trigger) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = pendingIntent(context, alarm.id);
        if (hasExactAccess(context)) {
            try {
                PendingIntent show = PendingIntent.getActivity(
                        context,
                        alarm.id,
                        new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                manager.setAlarmClock(new AlarmManager.AlarmClockInfo(trigger, show), operation);
                return;
            } catch (SecurityException ignored) {
                // Permission can be revoked between the check and the API call.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation);
    }

    public static void cancel(Context context, int id) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent(context, id));
    }

    public static void scheduleAll(Context context) {
        for (Alarm alarm : AlarmStore.all(context)) {
            if (alarm.enabled) schedule(context, alarm);
        }
    }

    public static boolean hasExactAccess(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms();
    }

    /** Upgrades a previously scheduled fallback without changing its chosen random time. */
    public static void rearmStored(Context context) {
        if (!hasExactAccess(context)) return;
        long now = System.currentTimeMillis();
        for (Alarm alarm : AlarmStore.all(context)) {
            if (!alarm.enabled) continue;
            if (alarm.nextTriggerMillis > now + 1000L) {
                scheduleAt(context, alarm, alarm.nextTriggerMillis);
            } else {
                schedule(context, alarm);
            }
        }
    }

    private static PendingIntent pendingIntent(Context context, int id) {
        Intent intent = new Intent(context, AlarmReceiver.class).putExtra(AlarmReceiver.EXTRA_ALARM_ID, id);
        return PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static long nextTrigger(Alarm alarm, long afterMillis) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(afterMillis);
        if (Alarm.INTERVAL.equals(alarm.repeatMode)) {
            long interval = Math.max(1, alarm.intervalMinutes) * 60_000L;
            Calendar first = (Calendar) now.clone();
            first.set(Calendar.HOUR_OF_DAY, alarm.hour);
            first.set(Calendar.MINUTE, alarm.minute);
            first.set(Calendar.SECOND, 0);
            first.set(Calendar.MILLISECOND, 0);
            long candidate = first.getTimeInMillis();
            if (candidate <= afterMillis + 999) {
                long elapsed = afterMillis - candidate;
                candidate += (elapsed / interval + 1) * interval;
            }
            return addRandomOffset(alarm, candidate);
        }

        Calendar candidate = (Calendar) now.clone();
        candidate.set(Calendar.HOUR_OF_DAY, alarm.hour);
        candidate.set(Calendar.MINUTE, alarm.minute);
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 8; i++) {
            int day = mondayIndex(candidate.get(Calendar.DAY_OF_WEEK));
            boolean valid = Alarm.DAILY.equals(alarm.repeatMode)
                    || (Alarm.WEEKDAYS.equals(alarm.repeatMode) && day < 5)
                    || (Alarm.WEEKENDS.equals(alarm.repeatMode) && day >= 5)
                    || (Alarm.CUSTOM.equals(alarm.repeatMode) && ((alarm.customDays & (1 << day)) != 0))
                    || Alarm.ONCE.equals(alarm.repeatMode);
            if (valid && candidate.getTimeInMillis() > afterMillis + 999) {
                return addRandomOffset(alarm, candidate.getTimeInMillis());
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1);
        }
        return addRandomOffset(alarm, candidate.getTimeInMillis());
    }

    private static long addRandomOffset(Alarm alarm, long base) {
        if (!alarm.randomTime || alarm.randomWindowMinutes <= 0) return base;
        return base + RANDOM.nextInt(alarm.randomWindowMinutes * 60_000 + 1);
    }

    private static int mondayIndex(int calendarDay) {
        return (calendarDay + 5) % 7;
    }
}
