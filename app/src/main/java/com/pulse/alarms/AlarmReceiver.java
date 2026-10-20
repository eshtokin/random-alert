package com.pulse.alarms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.media.AudioAttributes;
import android.os.Vibrator;
import android.os.VibratorManager;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALARM_ID = "alarm_id";
    private static final String CHANNEL_ID = "pulse_alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(EXTRA_ALARM_ID, -1);
        Alarm alarm = AlarmStore.get(context, id);
        if (alarm == null || !alarm.enabled) return;

        int power = alarm.randomPower ? 180 : alarm.power;
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = manager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(
                        VibrationPattern.parse(alarm.randomPattern ? VibrationPattern.randomPattern() : alarm.pattern, power, alarm.randomPower),
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build());
            }
        } catch (RuntimeException ignored) {
            // Keep recurring alarms scheduled even if this device rejects vibration.
        }
        try {
            showNotification(context, alarm);
        } catch (RuntimeException ignored) {
            // Notifications are optional; their denial must not break alarm recurrence.
        } finally {
            AlarmScheduler.scheduleNext(context, alarm, System.currentTimeMillis());
        }
    }

    private void showNotification(Context context, Alarm alarm) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Pulse alarms", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(false);
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, alarm.id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Pulse alarm")
                .setContentText("Vibration pattern is playing")
                .setContentIntent(pending)
                .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setPriority(android.app.Notification.PRIORITY_HIGH);
        manager.notify(alarm.id, builder.build());
    }
}
