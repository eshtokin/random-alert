package com.pulse.alarms;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private final int cream = Color.rgb(247, 246, 242);
    private final int ink = Color.rgb(23, 33, 43);
    private final int muted = Color.rgb(100, 113, 125);
    private final int teal = Color.rgb(0, 107, 103);
    private LinearLayout list;
    private TextView exactAccessNotice;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(cream);
        getWindow().setNavigationBarColor(cream);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 40);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmScheduler.rearmStored(this);
        if (list != null) {
            renderAlarms();
            updateExactAccessNotice();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(cream);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 14));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = Ui.text(this, "PULSE", 12, teal);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setLetterSpacing(.18f);
        header.addView(brand, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1));
        TextView status = Ui.text(this, "VIBRATION LAB", 10, muted);
        status.setLetterSpacing(.12f);
        header.addView(status);
        root.addView(header);

        TextView heading = Ui.title(this, "Your alarms");
        root.addView(heading, new LinearLayout.LayoutParams(-1, Ui.dp(this, 42)));
        TextView intro = Ui.text(this, "Stay on time, even when the app is closed.", 14, muted);
        root.addView(intro);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exactAccessNotice = Ui.text(this,
                    "Exact alarm access is off. Android may delay alarms. Tap to allow reliable timing.", 13, ink);
            exactAccessNotice.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            exactAccessNotice.setBackground(Ui.rounded(Color.rgb(255, 237, 208), 0, 14, this));
            exactAccessNotice.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) { }
            });
            LinearLayout.LayoutParams noticeParams = new LinearLayout.LayoutParams(-1, -2);
            noticeParams.topMargin = Ui.dp(this, 12);
            root.addView(exactAccessNotice, noticeParams);
        }
        root.addView(Ui.space(this, 18));

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        android.widget.Button add = Ui.primaryButton(this, "+   New alarm");
        add.setOnClickListener(v -> startActivity(new Intent(this, AddAlarmActivity.class)));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 56));
        addParams.topMargin = Ui.dp(this, 14);
        root.addView(add, addParams);
        setContentView(root);
        Ui.applySafeArea(root, this, 20, 18, 20, 14);
        updateExactAccessNotice();
    }

    private void renderAlarms() {
        list.removeAllViews();
        List<Alarm> alarms = AlarmStore.all(this);
        if (alarms.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 50), 0, Ui.dp(this, 50));
            TextView icon = Ui.text(this, "◌", 42, teal);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));
            TextView title = Ui.text(this, "Nothing scheduled yet", 18, ink);
            title.setGravity(Gravity.CENTER);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            empty.addView(title);
            TextView body = Ui.text(this, "Create a quiet, custom signal\nfor the moments you want to remember.", 14, muted);
            body.setGravity(Gravity.CENTER);
            empty.addView(body);
            list.addView(empty, new LinearLayout.LayoutParams(-1, -1));
            return;
        }
        for (Alarm alarm : alarms) list.addView(alarmCard(alarm));
    }

    private boolean hasEnabledAlarm() {
        for (Alarm alarm : AlarmStore.all(this)) if (alarm.enabled) return true;
        return false;
    }

    private void updateExactAccessNotice() {
        if (exactAccessNotice != null) {
            exactAccessNotice.setVisibility(hasEnabledAlarm() && !AlarmScheduler.hasExactAccess(this)
                    ? View.VISIBLE : View.GONE);
        }
    }

    private View alarmCard(Alarm alarm) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setClickable(true);
        card.setOnClickListener(v -> startActivity(new Intent(this, AddAlarmActivity.class).putExtra("alarm_id", alarm.id)));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView time = Ui.text(this, String.format("%02d:%02d", alarm.hour, alarm.minute), 34, ink);
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(time, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(alarm.enabled);
        toggle.setOnClickListener(v -> {
            alarm.enabled = toggle.isChecked();
            if (alarm.enabled) AlarmScheduler.schedule(this, alarm); else AlarmScheduler.cancel(this, alarm.id);
            AlarmStore.upsert(this, alarm);
        });
        top.addView(toggle);
        card.addView(top);

        TextView repeat = Ui.text(this, alarm.repeatLabel(), 14, teal);
        repeat.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(repeat);
        TextView details = Ui.text(this, (alarm.randomTime ? "Random time · " : "") +
                (alarm.randomPattern ? "Random pattern" : "Pattern " + alarm.pattern) +
                (alarm.randomPower ? " · random power" : " · power " + Math.round(alarm.power / 2.55f) + "%"), 13, muted);
        details.setMaxLines(2);
        details.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(details);
        if (alarm.enabled && alarm.nextTriggerMillis > 0) {
            TextView next = Ui.text(this, "Next · " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(alarm.nextTriggerMillis)), 12, muted);
            card.addView(next);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = Ui.dp(this, 12);
        card.setLayoutParams(params);
        return card;
    }
}
