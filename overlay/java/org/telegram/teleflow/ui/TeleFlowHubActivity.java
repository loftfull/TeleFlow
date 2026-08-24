/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

public final class TeleFlowHubActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        styleActionBar();
        actionBar.setTitle(getString(R.string.TeleFlowSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackground(TeleFlowUi.screenBackground());
        fragmentView = scroll;

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(22), AndroidUtilities.dp(18), AndroidUtilities.dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView brand = TeleFlowUi.text(context, "TeleFlow", 30, TeleFlowUi.TEXT_PRIMARY, true);
        content.addView(brand, rowParams(0));

        TextView tagline = TeleFlowUi.text(context, getString(R.string.TeleFlowFoundationStatus), 15, TeleFlowUi.TEXT_SECONDARY, false);
        tagline.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        content.addView(tagline, rowParams(0));

        TextView section = TeleFlowUi.text(context, getString(R.string.TeleFlowSettingsInfo).toUpperCase(), 12, TeleFlowUi.TEXT_SECONDARY, true);
        content.addView(section, rowParams(28));

        View folders = featureCard(
            context,
            "F",
            getString(R.string.TeleFlowSmartFolders),
            getString(R.string.TeleFlowSmartFoldersInfo),
            TeleFlowUi.BLUE,
            TeleFlowUi.PURPLE
        );
        folders.setOnClickListener(v -> presentFragment(new TeleFlowSmartFoldersActivity()));
        content.addView(folders, rowParams(10));

        View drive = featureCard(
            context,
            "D",
            getString(R.string.TeleFlowDrive),
            getString(R.string.TeleFlowDriveInfo),
            TeleFlowUi.TEAL,
            TeleFlowUi.BLUE
        );
        drive.setOnClickListener(v -> presentFragment(new TeleFlowDriveActivity()));
        content.addView(drive, rowParams(12));

        LinearLayout safety = new LinearLayout(context);
        safety.setOrientation(LinearLayout.VERTICAL);
        safety.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        safety.setBackground(TeleFlowUi.cardBackground());
        TextView safetyTitle = TeleFlowUi.text(context, "TELEGRAM CORE", 11, TeleFlowUi.TEAL, true);
        safety.addView(safetyTitle);
        TextView safetyText = TeleFlowUi.text(context, "TeleFlow работает поверх проверенной Telegram-основы. Новые модули не включаются до завершения текущего слоя.", 13, TeleFlowUi.TEXT_SECONDARY, false);
        safetyText.setPadding(0, AndroidUtilities.dp(5), 0, 0);
        safety.addView(safetyText);
        content.addView(safety, rowParams(20));
        return fragmentView;
    }

    private void styleActionBar() {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setBackgroundColor(TeleFlowUi.NAVY);
        actionBar.setTitleColor(TeleFlowUi.TEXT_PRIMARY);
        actionBar.setItemsColor(TeleFlowUi.TEXT_PRIMARY, false);
    }

    private View featureCard(Context context, String badge, String title, String subtitle, int startColor, int endColor) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14));
        card.setBackground(TeleFlowUi.cardBackground());

        FrameLayout badgeWrap = new FrameLayout(context);
        badgeWrap.setBackground(TeleFlowUi.accentBackground(startColor, endColor));
        TextView badgeText = TeleFlowUi.text(context, badge, 17, Color.WHITE, true);
        badgeText.setGravity(Gravity.CENTER);
        badgeWrap.addView(badgeText, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(46), AndroidUtilities.dp(46));
        card.addView(badgeWrap, badgeParams);

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = TeleFlowUi.text(context, title, 16, TeleFlowUi.TEXT_PRIMARY, true);
        copy.addView(titleView);
        TextView subtitleView = TeleFlowUi.text(context, subtitle, 13, TeleFlowUi.TEXT_SECONDARY, false);
        subtitleView.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        copy.addView(subtitleView);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        copyParams.leftMargin = AndroidUtilities.dp(13);
        card.addView(copy, copyParams);

        TextView arrow = TeleFlowUi.text(context, "›", 28, TeleFlowUi.TEXT_SECONDARY, false);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(AndroidUtilities.dp(28), AndroidUtilities.dp(46)));
        return card;
    }

    private LinearLayout.LayoutParams rowParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(topMarginDp);
        return params;
    }
}
