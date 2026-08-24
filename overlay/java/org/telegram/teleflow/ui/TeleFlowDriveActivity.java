/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

public final class TeleFlowDriveActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        styleActionBar();
        actionBar.setTitle(getString(R.string.TeleFlowDrive));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackground(TeleFlowUi.screenBackground());
        fragmentView = scroll;

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        hero.setBackground(TeleFlowUi.accentBackground(TeleFlowUi.TEAL, TeleFlowUi.BLUE));
        TextView label = TeleFlowUi.text(context, "TELEFLOW DRIVE", 11, TeleFlowUi.TEXT_PRIMARY, true);
        hero.addView(label);
        TextView title = TeleFlowUi.text(context, getString(R.string.TeleFlowDrive), 28, TeleFlowUi.TEXT_PRIMARY, true);
        title.setPadding(0, AndroidUtilities.dp(7), 0, 0);
        hero.addView(title);
        TextView subtitle = TeleFlowUi.text(context, getString(R.string.TeleFlowDriveInfo), 14, TeleFlowUi.TEXT_PRIMARY, false);
        subtitle.setAlpha(0.82f);
        subtitle.setPadding(0, AndroidUtilities.dp(5), 0, 0);
        hero.addView(subtitle);
        content.addView(hero, rowParams(0));

        LinearLayout status = new LinearLayout(context);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(15), AndroidUtilities.dp(16), AndroidUtilities.dp(15));
        status.setBackground(TeleFlowUi.cardBackground());
        TextView statusLabel = TeleFlowUi.text(context, "STATUS", 11, TeleFlowUi.TEAL, true);
        status.addView(statusLabel);
        TextView statusText = TeleFlowUi.text(context, getString(R.string.TeleFlowDriveStatus), 14, TeleFlowUi.TEXT_PRIMARY, false);
        statusText.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        status.addView(statusText);
        TextView note = TeleFlowUi.text(context, "Экран показывает только подтверждённый текущий слой Drive — без демонстрационных файлов, объёмов и незавершённых действий.", 13, TeleFlowUi.TEXT_SECONDARY, false);
        note.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        status.addView(note);
        content.addView(status, rowParams(12));
        return fragmentView;
    }

    private void styleActionBar() {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setBackgroundColor(TeleFlowUi.NAVY);
        actionBar.setTitleColor(TeleFlowUi.TEXT_PRIMARY);
        actionBar.setItemsColor(TeleFlowUi.TEXT_PRIMARY, false);
    }

    private LinearLayout.LayoutParams rowParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(topMarginDp);
        return params;
    }
}
