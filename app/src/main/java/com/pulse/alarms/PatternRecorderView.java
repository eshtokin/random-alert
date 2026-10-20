package com.pulse.alarms;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatternRecorderView extends View {
    public interface Listener {
        void onPatternChanged(String pattern, boolean recording);
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<String> segments = new ArrayList<>();
    private final Vibrator vibrator;
    private Listener listener;
    private String recordedPattern = "";
    private String initialPattern = "";
    private boolean recording;
    private long downAt;
    private long lastUpAt;
    private int power = 180;

    public PatternRecorderView(Context context) {
        super(context);
        setFocusable(true);
        setClickable(true);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        setBackground(Ui.rounded(android.graphics.Color.rgb(217, 241, 237), 0, 22, context));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setPower(int power) {
        this.power = Math.max(1, Math.min(255, power));
    }

    public void setInitialPattern(String pattern) {
        initialPattern = pattern == null ? "" : pattern;
        invalidate();
    }

    public boolean isRecording() {
        return recording;
    }

    public void startRecording() {
        segments.clear();
        recordedPattern = "";
        downAt = 0;
        lastUpAt = 0;
        recording = true;
        notifyChanged();
        invalidate();
    }

    public String stopRecording() {
        if (downAt > 0) finishTap(SystemClock.elapsedRealtime());
        recording = false;
        recordedPattern = renderSegments();
        notifyChanged();
        invalidate();
        return recordedPattern;
    }

    public String getRecordedPattern() {
        return recordedPattern;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!recording) return true;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            long now = SystemClock.elapsedRealtime();
            if (lastUpAt > 0) segments.add("p" + formatSeconds(now - lastUpAt) + "s");
            downAt = now;
            triggerTapVibration();
            invalidate();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            finishTap(SystemClock.elapsedRealtime());
            invalidate();
            return true;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        fill.setColor(recording ? android.graphics.Color.rgb(0, 107, 103) : android.graphics.Color.rgb(217, 241, 237));
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), Ui.dp(getContext(), 22), Ui.dp(getContext(), 22), fill);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(Ui.dp(getContext(), recording ? 20 : 18));
        text.setColor(recording ? android.graphics.Color.WHITE : android.graphics.Color.rgb(0, 107, 103));
        String title = recording ? "TAP OR HOLD" : "TAP TO RECORD";
        canvas.drawText(title, getWidth() / 2f, getHeight() / 2f - Ui.dp(getContext(), 4), text);
        text.setTypeface(Typeface.DEFAULT);
        text.setTextSize(Ui.dp(getContext(), 12));
        String subtitle = recording ? "Each press vibrates and becomes a d / p segment" : "Hold for longer vibration · release for the pause";
        canvas.drawText(subtitle, getWidth() / 2f, getHeight() / 2f + Ui.dp(getContext(), 22), text);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    private void finishTap(long upAt) {
        if (downAt <= 0) return;
        stopTapVibration();
        segments.add("d" + formatSeconds(Math.max(50, upAt - downAt)) + "s");
        lastUpAt = upAt;
        downAt = 0;
        notifyChanged();
    }

    private void triggerTapVibration() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(30_000, power),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
        } else {
            vibrator.vibrate(30_000);
        }
    }

    private void stopTapVibration() {
        if (vibrator != null) vibrator.cancel();
    }

    private void notifyChanged() {
        if (listener != null) listener.onPatternChanged(renderSegments(), recording);
    }

    private String renderSegments() {
        if (segments.isEmpty()) return "";
        return String.join(" ", segments);
    }

    private String formatSeconds(long millis) {
        if (millis < 1000) return String.format(Locale.US, "%.1f", millis / 1000f);
        if (millis % 1000 == 0) return String.format(Locale.US, "%d", millis / 1000);
        return String.format(Locale.US, "%.1f", millis / 1000f);
    }
}
