/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

/**
 * Small, dependency-free visual layer for the TeleFlow skeleton screens.
 * The palette mirrors the approved TeleFlow reference while keeping the
 * extension isolated from Telegram's global theme implementation.
 */
public final class TeleFlowUi {
    public static final int NAVY = Color.parseColor("#09111F");
    public static final int CARD = Color.parseColor("#111C2C");
    public static final int CARD_ELEVATED = Color.parseColor("#18263A");
    public static final int BLUE = Color.parseColor("#4C7DFF");
    public static final int PURPLE = Color.parseColor("#8B5CF6");
    public static final int TEAL = Color.parseColor("#2DD4BF");
    public static final int TEXT_PRIMARY = Color.parseColor("#F4F7FB");
    public static final int TEXT_SECONDARY = Color.parseColor("#8FA2BD");
    public static final int BORDER = Color.parseColor("#243650");

    private TeleFlowUi() {}

    public static GradientDrawable screenBackground() {
        return roundedGradient(
            new int[] { NAVY, Color.parseColor("#0B1626"), Color.parseColor("#0D1727") },
            0,
            0
        );
    }

    public static GradientDrawable cardBackground() {
        return roundedGradient(new int[] { CARD, CARD_ELEVATED }, 18, BORDER);
    }

    public static GradientDrawable accentBackground(int startColor, int endColor) {
        return roundedGradient(new int[] { startColor, endColor }, 18, 0);
    }

    public static GradientDrawable softAccentBackground(int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(withAlpha(accent, 36));
        drawable.setCornerRadius(AndroidUtilities.dp(14));
        drawable.setStroke(AndroidUtilities.dp(1), withAlpha(accent, 105));
        return drawable;
    }

    public static TextView text(Context context, CharSequence value, int sizeDp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
        view.setTextColor(color);
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private static GradientDrawable roundedGradient(int[] colors, int radiusDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(AndroidUtilities.dp(radiusDp));
        if (strokeColor != 0) {
            drawable.setStroke(AndroidUtilities.dp(1), strokeColor);
        }
        return drawable;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
