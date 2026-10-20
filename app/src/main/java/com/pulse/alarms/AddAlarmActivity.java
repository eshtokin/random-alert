package com.pulse.alarms;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Locale;

public class AddAlarmActivity extends Activity {
    private final int ink = Color.rgb(23, 33, 43);
    private final int muted = Color.rgb(100, 113, 125);
    private final int teal = Color.rgb(0, 107, 103);
    private final int cream = Color.rgb(247, 246, 242);
    private final String[] repeatModes = {Alarm.ONCE, Alarm.DAILY, Alarm.WEEKDAYS, Alarm.WEEKENDS, Alarm.CUSTOM, Alarm.INTERVAL};
    private final String[] repeatLabels = {"Once", "Every day", "Weekdays", "Weekends", "Custom days", "Interval"};
    private Alarm alarm;
    private TimePicker timePicker;
    private Spinner repeatSpinner;
    private LinearLayout customDaysLayout;
    private LinearLayout intervalLayout;
    private EditText intervalInput;
    private Switch randomTimeSwitch;
    private EditText randomWindowInput;
    private Switch randomPatternSwitch;
    private Switch randomPowerSwitch;
    private PatternRecorderView patternRecorder;
    private TextView patternPreview;
    private android.widget.Button recordButton;
    private SeekBar powerSeek;
    private TextView powerValue;
    private CheckBox[] dayChecks;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(cream);
        getWindow().setNavigationBarColor(cream);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        int id = getIntent().getIntExtra("alarm_id", -1);
        alarm = id == -1 ? new Alarm() : AlarmStore.get(this, id);
        if (alarm == null) alarm = new Alarm();
        if (alarm.id == 0) alarm.id = Math.max(1, (int) (System.currentTimeMillis() & 0x7fffffff));
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(cream);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), 0);
        TextView back = Ui.text(this, "‹", 38, ink);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 50)));
        TextView heading = Ui.title(this, getIntent().hasExtra("alarm_id") ? "Edit alarm" : "New alarm");
        toolbar.addView(heading, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));
        root.addView(toolbar);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content.addView(Ui.text(this, "WHEN", 11, teal));
        TextView helper = Ui.text(this, "Choose a start time. We’ll wake the device when it’s due.", 13, muted);
        content.addView(helper);
        timePicker = new TimePicker(this);
        timePicker.setIs24HourView(true);
        timePicker.setHour(alarm.hour);
        timePicker.setMinute(alarm.minute);
        // Native clock/spinner pickers vary in height by Android version and theme.
        // A fixed height clips the lower half on newer devices, so let it measure itself.
        timePicker.setMinimumHeight(Ui.dp(this, 240));
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-1, -2);
        timeParams.topMargin = Ui.dp(this, 5);
        content.addView(timePicker, timeParams);

        content.addView(sectionLabel("REPEAT"));
        repeatSpinner = new Spinner(this);
        repeatSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, repeatLabels));
        repeatSpinner.setSelection(indexOf(alarm.repeatMode));
        repeatSpinner.setBackground(Ui.rounded(Color.WHITE, Color.rgb(229, 232, 233), 14, this));
        content.addView(repeatSpinner, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));
        repeatSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (customDaysLayout != null) customDaysLayout.setVisibility(position == 4 ? View.VISIBLE : View.GONE);
                if (intervalLayout != null) intervalLayout.setVisibility(position == 5 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        customDaysLayout = new LinearLayout(this);
        customDaysLayout.setGravity(Gravity.CENTER);
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        dayChecks = new CheckBox[7];
        for (int i = 0; i < days.length; i++) {
            CheckBox check = new CheckBox(this);
            check.setText(days[i]);
            check.setTextColor(ink);
            check.setTextSize(13);
            check.setGravity(Gravity.CENTER);
            check.setChecked((alarm.customDays & (1 << i)) != 0);
            dayChecks[i] = check;
            customDaysLayout.addView(check, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        }
        content.addView(customDaysLayout);

        intervalLayout = new LinearLayout(this);
        intervalLayout.setGravity(Gravity.CENTER_VERTICAL);
        TextView every = Ui.text(this, "Every", 14, muted);
        intervalLayout.addView(every);
        intervalInput = smallInput(String.valueOf(alarm.intervalMinutes));
        intervalLayout.addView(intervalInput, new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 48)));
        TextView minutes = Ui.text(this, "minutes", 14, muted);
        LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(-2, Ui.dp(this, 48));
        minParams.leftMargin = Ui.dp(this, 8);
        intervalLayout.addView(minutes, minParams);
        content.addView(intervalLayout);

        content.addView(sectionLabel("UNPREDICTABILITY"));
        LinearLayout randomCard = new LinearLayout(this);
        randomCard.setOrientation(LinearLayout.VERTICAL);
        randomCard.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10));
        randomCard.setBackgroundResource(R.drawable.bg_card);
        randomTimeSwitch = labeledSwitch("Randomize time", "Pick a different moment inside a window each time", alarm.randomTime);
        randomCard.addView(randomTimeSwitch);
        LinearLayout windowRow = new LinearLayout(this);
        windowRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView windowLabel = Ui.text(this, "Window after start", 13, muted);
        windowRow.addView(windowLabel, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        randomWindowInput = smallInput(String.valueOf(alarm.randomWindowMinutes));
        windowRow.addView(randomWindowInput, new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 42)));
        TextView windowUnit = Ui.text(this, "min", 13, muted);
        LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(-2, Ui.dp(this, 42));
        unitParams.leftMargin = Ui.dp(this, 7);
        windowRow.addView(windowUnit, unitParams);
        randomCard.addView(windowRow);
        randomTimeSwitch.setOnCheckedChangeListener((button, checked) -> windowRow.setVisibility(checked ? View.VISIBLE : View.GONE));
        windowRow.setVisibility(alarm.randomTime ? View.VISIBLE : View.GONE);

        randomPatternSwitch = labeledSwitch("Randomize pattern", "Generate a fresh rhythm for every alarm", alarm.randomPattern);
        randomCard.addView(randomPatternSwitch);
        randomPowerSwitch = labeledSwitch("Randomize power", "Keep the intensity unpredictable", alarm.randomPower);
        randomCard.addView(randomPowerSwitch);
        content.addView(randomCard, new LinearLayout.LayoutParams(-1, -2));

        content.addView(sectionLabel("VIBRATION"));
        LinearLayout powerRow = new LinearLayout(this);
        powerRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView powerLabel = Ui.text(this, "Power", 15, ink);
        powerRow.addView(powerLabel, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        powerValue = Ui.text(this, powerPercent(alarm.power), 14, teal);
        powerValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        powerRow.addView(powerValue);
        content.addView(powerRow);
        powerSeek = new SeekBar(this);
        powerSeek.setMax(254);
        powerSeek.setProgress(Math.max(0, Math.min(254, alarm.power - 1)));
        powerSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { powerValue.setText(powerPercent(progress + 1)); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        content.addView(powerSeek);
        TextView powerHint = Ui.text(this, "The actual maximum depends on your device’s vibration motor.", 12, muted);
        content.addView(powerHint);

        TextView patternHint = Ui.text(this, "Choose power, then tap and hold the pad to record your rhythm.", 13, muted);
        content.addView(patternHint);
        patternRecorder = new PatternRecorderView(this);
        patternRecorder.setPower(alarm.power);
        patternRecorder.setInitialPattern(alarm.pattern);
        patternRecorder.setListener((pattern, recording) -> {
            if (pattern.isEmpty()) {
                patternPreview.setText("No new pattern recorded");
            } else {
                patternPreview.setText(recording ? "Recording  ·  " + pattern : "Recorded  ·  " + pattern);
            }
        });
        LinearLayout.LayoutParams padParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 148));
        padParams.topMargin = Ui.dp(this, 8);
        content.addView(patternRecorder, padParams);

        recordButton = Ui.primaryButton(this, "Record pattern");
        LinearLayout.LayoutParams recordParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 52));
        recordParams.topMargin = Ui.dp(this, 10);
        content.addView(recordButton, recordParams);
        recordButton.setOnClickListener(v -> {
            if (patternRecorder.isRecording()) {
                patternRecorder.stopRecording();
                recordButton.setText("Record pattern");
            } else {
                patternRecorder.setPower(powerSeek.getProgress() + 1);
                patternRecorder.startRecording();
                recordButton.setText("Stop recording");
            }
        });
        patternPreview = Ui.text(this, alarm.pattern.isEmpty() ? "No pattern recorded" : "Current  ·  " + alarm.pattern, 12, teal);
        patternPreview.setMaxLines(3);
        patternPreview.setPadding(Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 2), 0);
        content.addView(patternPreview);

        android.widget.Button test = Ui.primaryButton(this, "Test pattern");
        test.setTextColor(teal);
        test.setBackground(Ui.rounded(Color.rgb(217, 241, 237), 0, 18, this));
        test.setOnClickListener(v -> {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            String testPattern = patternRecorder.getRecordedPattern().isEmpty() ? alarm.pattern : patternRecorder.getRecordedPattern();
            if (vibrator != null) vibrator.vibrate(
                    VibrationPattern.parse(testPattern, powerSeek.getProgress() + 1, randomPowerSwitch.isChecked()),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
        });
        content.addView(test, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));

        android.widget.Button save = Ui.primaryButton(this, "Save alarm");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 56));
        saveParams.topMargin = Ui.dp(this, 14);
        content.addView(save, saveParams);
        save.setOnClickListener(v -> saveAlarm());

        if (getIntent().hasExtra("alarm_id")) {
            TextView delete = Ui.text(this, "Delete this alarm", 14, Color.rgb(184, 58, 69));
            delete.setGravity(Gravity.CENTER);
            delete.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 10));
            delete.setOnClickListener(v -> {
                AlarmScheduler.cancel(this, alarm.id);
                AlarmStore.delete(this, alarm.id);
                finish();
            });
            content.addView(delete);
        }

        setContentView(root);
        Ui.applySafeArea(root, this, 0, 0, 0, 0);
    }

    private void saveAlarm() {
        alarm.hour = timePicker.getHour();
        alarm.minute = timePicker.getMinute();
        int index = repeatSpinner.getSelectedItemPosition();
        alarm.repeatMode = repeatModes[index];
        if (intervalInput != null) alarm.intervalMinutes = clamp(parseInt(intervalInput.getText().toString(), 60), 1, 10080);
        int selectedDays = 0;
        if (dayChecks != null) for (int i = 0; i < dayChecks.length; i++) if (dayChecks[i].isChecked()) selectedDays |= (1 << i);
        if (selectedDays != 0) alarm.customDays = selectedDays;
        alarm.randomTime = randomTimeSwitch.isChecked();
        alarm.randomWindowMinutes = clamp(parseInt(randomWindowInput.getText().toString(), 30), 1, 1440);
        alarm.randomPattern = randomPatternSwitch.isChecked();
        alarm.randomPower = randomPowerSwitch.isChecked();
        alarm.power = powerSeek.getProgress() + 1;
        String recordedPattern = patternRecorder.getRecordedPattern();
        if (!recordedPattern.isEmpty()) alarm.pattern = VibrationPattern.normalize(recordedPattern);
        alarm.enabled = true;
        AlarmStore.upsert(this, alarm);
        AlarmScheduler.schedule(this, alarm);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                && !AlarmScheduler.hasExactAccess(this)) {
            Toast.makeText(this, "Allow exact alarms for reliable timing", Toast.LENGTH_LONG).show();
            try {
                startActivity(new android.content.Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
                // Some device variants do not expose the special-access screen.
            }
        }
        Toast.makeText(this,
                AlarmScheduler.hasExactAccess(this) ? "Alarm saved" : "Saved; exact timing needs permission",
                Toast.LENGTH_LONG).show();
        finish();
    }

    private TextView sectionLabel(String label) {
        TextView view = Ui.text(this, label, 11, teal);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Ui.dp(this, 32));
        params.topMargin = Ui.dp(this, 18);
        view.setLayoutParams(params);
        return view;
    }

    private Switch labeledSwitch(String title, String subtitle, boolean checked) {
        Switch toggle = new Switch(this);
        toggle.setText(title + "\n" + subtitle);
        toggle.setTextSize(15);
        toggle.setTextColor(ink);
        toggle.setChecked(checked);
        toggle.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        return toggle;
    }

    private EditText smallInput(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setTextSize(15);
        input.setTextColor(ink);
        input.setGravity(Gravity.CENTER);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setBackground(Ui.rounded(Color.WHITE, Color.rgb(229, 232, 233), 12, this));
        return input;
    }

    private int indexOf(String mode) {
        for (int i = 0; i < repeatModes.length; i++) if (repeatModes[i].equals(mode)) return i;
        return 0;
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (Exception ignored) { return fallback; }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String powerPercent(int value) { return String.format(Locale.US, "%d%%", Math.round(value / 2.55f)); }
}
