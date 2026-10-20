package com.pulse.alarms;

import org.json.JSONException;
import org.json.JSONObject;

public class Alarm {
    public static final String ONCE = "once";
    public static final String DAILY = "daily";
    public static final String WEEKDAYS = "weekdays";
    public static final String WEEKENDS = "weekends";
    public static final String CUSTOM = "custom";
    public static final String INTERVAL = "interval";

    public int id;
    public int hour = 8;
    public int minute = 0;
    public String repeatMode = ONCE;
    public int customDays = 31; // Monday-Friday
    public int intervalMinutes = 60;
    public boolean randomTime;
    public int randomWindowMinutes = 30;
    public boolean randomPattern;
    public boolean randomPower;
    public String pattern = "d2s p2s d1s p3s d2s";
    public int power = 180;
    public boolean enabled = true;
    public long nextTriggerMillis;

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("hour", hour);
        o.put("minute", minute);
        o.put("repeatMode", repeatMode);
        o.put("customDays", customDays);
        o.put("intervalMinutes", intervalMinutes);
        o.put("randomTime", randomTime);
        o.put("randomWindowMinutes", randomWindowMinutes);
        o.put("randomPattern", randomPattern);
        o.put("randomPower", randomPower);
        o.put("pattern", pattern);
        o.put("power", power);
        o.put("enabled", enabled);
        o.put("nextTriggerMillis", nextTriggerMillis);
        return o;
    }

    public static Alarm fromJson(JSONObject o) throws JSONException {
        Alarm a = new Alarm();
        a.id = o.optInt("id");
        a.hour = o.optInt("hour", 8);
        a.minute = o.optInt("minute", 0);
        a.repeatMode = o.optString("repeatMode", ONCE);
        a.customDays = o.optInt("customDays", 31);
        a.intervalMinutes = o.optInt("intervalMinutes", 60);
        a.randomTime = o.optBoolean("randomTime");
        a.randomWindowMinutes = o.optInt("randomWindowMinutes", 30);
        a.randomPattern = o.optBoolean("randomPattern");
        a.randomPower = o.optBoolean("randomPower");
        a.pattern = o.optString("pattern", "d2s p2s d1s p3s d2s");
        a.power = o.optInt("power", 180);
        a.enabled = o.optBoolean("enabled", true);
        a.nextTriggerMillis = o.optLong("nextTriggerMillis", 0L);
        return a;
    }

    public String repeatLabel() {
        if (INTERVAL.equals(repeatMode)) {
            if (intervalMinutes % 60 == 0) return "Every " + (intervalMinutes / 60) + "h";
            return "Every " + intervalMinutes + "m";
        }
        if (DAILY.equals(repeatMode)) return "Every day";
        if (WEEKDAYS.equals(repeatMode)) return "Weekdays";
        if (WEEKENDS.equals(repeatMode)) return "Weekends";
        if (CUSTOM.equals(repeatMode)) return "Custom days";
        return "Once";
    }
}
