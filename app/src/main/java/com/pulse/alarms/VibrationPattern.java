package com.pulse.alarms;

import android.os.Build;
import android.os.VibrationEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VibrationPattern {
    private static final Pattern TOKEN = Pattern.compile("([dp])\\s*(\\d+(?:\\.\\d+)?)\\s*s?", Pattern.CASE_INSENSITIVE);
    private static final Random RANDOM = new Random();

    private VibrationPattern() {}

    public static VibrationEffect parse(String source, int power, boolean randomPower) {
        List<Long> timings = new ArrayList<>();
        List<Integer> amplitudes = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(source == null ? "" : source);
        while (matcher.find()) {
            long millis = Math.max(50L, Math.min(30_000L,
                    Math.round(Double.parseDouble(matcher.group(2)) * 1000.0)));
            boolean on = matcher.group(1).equalsIgnoreCase("d");
            timings.add(millis);
            amplitudes.add(on ? (randomPower ? randomAmplitude() : clamp(power)) : 0);
        }
        if (timings.isEmpty()) {
            timings.add(500L);
            amplitudes.add(clamp(power));
        }
        long[] timeArray = new long[timings.size() + 1];
        int[] amplitudeArray = new int[amplitudes.size() + 1];
        timeArray[0] = 0;
        amplitudeArray[0] = 0;
        for (int i = 0; i < timings.size(); i++) {
            timeArray[i + 1] = timings.get(i);
            amplitudeArray[i + 1] = amplitudes.get(i);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return VibrationEffect.createWaveform(timeArray, amplitudeArray, -1);
        }
        return VibrationEffect.createWaveform(timeArray, -1);
    }

    public static String randomPattern() {
        int bursts = 3 + RANDOM.nextInt(4);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bursts; i++) {
            if (i > 0) builder.append(' ');
            builder.append("d").append(1 + RANDOM.nextInt(3)).append("s p")
                    .append(1 + RANDOM.nextInt(4)).append("s");
        }
        return builder.toString();
    }

    public static String normalize(String source) {
        Matcher matcher = TOKEN.matcher(source == null ? "" : source);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(matcher.group(1).toLowerCase(Locale.US)).append(matcher.group(2)).append('s');
        }
        return builder.length() == 0 ? "d1s p1s" : builder.toString();
    }

    private static int clamp(int value) { return Math.max(1, Math.min(255, value)); }
    private static int randomAmplitude() { return 90 + RANDOM.nextInt(166); }
}
