package com.pulse.alarms;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class AlarmStore {
    private static final String PREFS = "pulse_alarms";
    private static final String ITEMS = "items";

    private AlarmStore() {}

    public static synchronized List<Alarm> all(Context context) {
        List<Alarm> result = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) result.add(Alarm.fromJson(array.getJSONObject(i)));
        } catch (JSONException ignored) { }
        return result;
    }

    public static synchronized Alarm get(Context context, int id) {
        for (Alarm alarm : all(context)) if (alarm.id == id) return alarm;
        return null;
    }

    public static synchronized void upsert(Context context, Alarm alarm) {
        List<Alarm> alarms = all(context);
        boolean replaced = false;
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).id == alarm.id) {
                alarms.set(i, alarm);
                replaced = true;
                break;
            }
        }
        if (!replaced) alarms.add(alarm);
        save(context, alarms);
    }

    public static synchronized void delete(Context context, int id) {
        List<Alarm> alarms = all(context);
        alarms.removeIf(alarm -> alarm.id == id);
        save(context, alarms);
    }

    private static void save(Context context, List<Alarm> alarms) {
        JSONArray array = new JSONArray();
        for (Alarm alarm : alarms) {
            try { array.put(alarm.toJson()); } catch (JSONException ignored) { }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(ITEMS, array.toString()).apply();
    }
}
