package com.pulse.alarms;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;

public final class Ui {
    private Ui() {}

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context context, String value, float size, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        return view;
    }

    public static TextView title(Context context, String value) {
        TextView view = text(context, value, 28, Color.rgb(23, 33, 43));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    public static View space(Context context, int height) {
        View view = new View(context);
        view.setLayoutParams(new ViewGroup.LayoutParams(1, dp(context, height)));
        return view;
    }

    public static GradientDrawable rounded(int fill, int stroke, int radius, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radius));
        if (stroke != 0) drawable.setStroke(dp(context, 1), stroke);
        return drawable;
    }

    public static Button primaryButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(context, 52));
        button.setPadding(dp(context, 18), 0, dp(context, 18), 0);
        button.setBackgroundResource(com.pulse.alarms.R.drawable.bg_primary_button);
        return button;
    }

    public static void margins(View view, int left, int top, int right, int bottom, Context context) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) params).setMargins(dp(context, left), dp(context, top), dp(context, right), dp(context, bottom));
            view.setLayoutParams(params);
        }
    }

    /** Applies a stable content inset for status bars, navigation bars, and cutouts. */
    public static void applySafeArea(View view, Context context, int left, int top, int right, int bottom) {
        final int baseLeft = dp(context, left);
        final int baseTop = dp(context, top);
        final int baseRight = dp(context, right);
        final int baseBottom = dp(context, bottom);
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            int insetLeft;
            int insetTop;
            int insetRight;
            int insetBottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.ime() | WindowInsets.Type.displayCutout());
                insetLeft = safe.left;
                insetTop = safe.top;
                insetRight = safe.right;
                insetBottom = safe.bottom;
            } else {
                insetLeft = insets.getSystemWindowInsetLeft();
                insetTop = insets.getSystemWindowInsetTop();
                insetRight = insets.getSystemWindowInsetRight();
                insetBottom = insets.getSystemWindowInsetBottom();
            }
            target.setPadding(baseLeft + insetLeft, baseTop + insetTop,
                    baseRight + insetRight, baseBottom + insetBottom);
            return insets;
        });
        view.requestApplyInsets();
    }
}
