package com.pulse.alarms;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(intent.getAction())) {
            if (AlarmScheduler.hasExactAccess(context)) AlarmScheduler.rearmStored(context);
            return;
        }
        AlarmScheduler.scheduleAll(context);
    }
}
