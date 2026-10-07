/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.teleflow.folders.ChatDescriptor;
import org.telegram.teleflow.folders.DefaultFolderRules;
import org.telegram.teleflow.folders.FolderAssignment;
import org.telegram.teleflow.folders.FolderPreviewPlan;
import org.telegram.teleflow.folders.FolderPreviewPlanner;
import org.telegram.teleflow.folders.FolderRuleEngine;
import org.telegram.teleflow.integration.TelegramDialogReader;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

public final class TeleFlowSmartFoldersActivity extends BaseFragment {
    private FolderPreviewPlan preview;

    @Override
    public View createView(Context context) {
        styleActionBar();
        actionBar.setTitle(getString(R.string.TeleFlowSmartFolders));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        String reviewFolder = getString(R.string.TeleFlowReviewFolder);
        List<ChatDescriptor> chats = TelegramDialogReader.readLoadedDialogs(getMessagesController());
        FolderRuleEngine engine = new FolderRuleEngine(DefaultFolderRules.create(), reviewFolder);
        preview = FolderPreviewPlanner.plan(chats, engine, reviewFolder);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackground(TeleFlowUi.screenBackground());
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(28));
        scroll.addView(column, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        fragmentView = scroll;

        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(17), AndroidUtilities.dp(18), AndroidUtilities.dp(17));
        hero.setBackground(TeleFlowUi.accentBackground(TeleFlowUi.BLUE, TeleFlowUi.PURPLE));
        TextView heroLabel = TeleFlowUi.text(context, getString(R.string.TeleFlowSmartFolders).toUpperCase(), 11, TeleFlowUi.TEXT_PRIMARY, true);
        hero.addView(heroLabel);
        LinearLayout metrics = new LinearLayout(context);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setPadding(0, AndroidUtilities.dp(12), 0, 0);
        metrics.addView(metric(context, String.valueOf(preview.getTotalChats()), getString(R.string.TeleFlowSmartFoldersScanned)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        metrics.addView(metric(context, String.valueOf(preview.getReviewCount()), getString(R.string.TeleFlowSmartFoldersNeedsReview)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        hero.addView(metrics);
        column.addView(hero, rowParams(0));

        TextView summaryView = TeleFlowUi.text(context, buildSummary(), 14, TeleFlowUi.TEXT_PRIMARY, false);
        summaryView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(15), AndroidUtilities.dp(16), AndroidUtilities.dp(15));
        summaryView.setBackground(TeleFlowUi.cardBackground());
        column.addView(summaryView, rowParams(12));

        TextView statusView = TeleFlowUi.text(context, getString(R.string.TeleFlowSmartFoldersStatus), 13, TeleFlowUi.TEXT_SECONDARY, false);
        statusView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        statusView.setBackground(TeleFlowUi.cardBackground());
        column.addView(statusView, rowParams(12));
        return fragmentView;
    }

    private void styleActionBar() {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setBackgroundColor(TeleFlowUi.NAVY);
        actionBar.setTitleColor(TeleFlowUi.TEXT_PRIMARY);
        actionBar.setItemsColor(TeleFlowUi.TEXT_PRIMARY, false);
    }

    private LinearLayout metric(Context context, String value, String label) {
        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        TextView valueView = TeleFlowUi.text(context, value, 28, TeleFlowUi.TEXT_PRIMARY, true);
        block.addView(valueView);
        TextView labelView = TeleFlowUi.text(context, label, 12, TeleFlowUi.TEXT_PRIMARY, false);
        labelView.setAlpha(0.78f);
        block.addView(labelView);
        return block;
    }

    private String buildSummary() {
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<String, List<FolderAssignment>> entry : preview.getFolders().entrySet()) {
            if (summary.length() > 0) summary.append("\n");
            summary.append(entry.getKey()).append("  ·  ").append(entry.getValue().size());
        }
        if (summary.length() == 0) {
            summary.append(getString(R.string.TeleFlowSmartFoldersStatus));
        }
        return summary.toString();
    }

    private LinearLayout.LayoutParams rowParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(topMarginDp);
        return params;
    }
}
