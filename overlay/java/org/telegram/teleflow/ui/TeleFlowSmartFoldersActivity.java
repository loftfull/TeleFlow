/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
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
import org.telegram.teleflow.integration.SmartFoldersBatchApply;
import org.telegram.teleflow.integration.TelegramDialogReader;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

public final class TeleFlowSmartFoldersActivity extends BaseFragment {
    private FolderPreviewPlan preview;
    private String reviewFolder;
    private TextView statusView;
    private TextView applyButton;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(getString(R.string.TeleFlowSmartFolders));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        reviewFolder = getString(R.string.TeleFlowReviewFolder);
        List<ChatDescriptor> chats = TelegramDialogReader.readLoadedDialogs(getMessagesController());
        FolderRuleEngine engine = new FolderRuleEngine(DefaultFolderRules.create(), reviewFolder);
        preview = FolderPreviewPlanner.plan(chats, engine, reviewFolder);

        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(column, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        fragmentView = scroll;

        TextView summaryView = card(context, buildSummary());
        column.addView(summaryView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        statusView = card(context, getString(R.string.TeleFlowSmartFoldersStatus));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = AndroidUtilities.dp(12);
        column.addView(statusView, statusParams);

        applyButton = new TextView(context);
        applyButton.setText(getString(R.string.TeleFlowApply));
        applyButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        applyButton.setGravity(Gravity.CENTER);
        applyButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        applyButton.setBackgroundColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        applyButton.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(14), AndroidUtilities.dp(18), AndroidUtilities.dp(14));
        applyButton.setEnabled(preview.getTotalChats() > 0);
        applyButton.setAlpha(applyButton.isEnabled() ? 1f : 0.5f);
        applyButton.setOnClickListener(v -> showApplyConfirmation());
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        applyParams.topMargin = AndroidUtilities.dp(16);
        column.addView(applyButton, applyParams);
        return fragmentView;
    }

    private TextView card(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        view.setGravity(Gravity.START);
        view.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        return view;
    }

    private String buildSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(getString(R.string.TeleFlowSmartFoldersScanned)).append(": ").append(preview.getTotalChats());
        summary.append("\n").append(getString(R.string.TeleFlowSmartFoldersNeedsReview)).append(": ").append(preview.getReviewCount());
        for (Map.Entry<String, List<FolderAssignment>> entry : preview.getFolders().entrySet()) {
            summary.append("\n\n").append(entry.getKey()).append(" — ").append(entry.getValue().size());
        }
        return summary.toString();
    }

    private void showApplyConfirmation() {
        if (getParentActivity() == null || preview == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString(R.string.TeleFlowApply));
        builder.setMessage(getString(R.string.TeleFlowApplyConfirm));
        builder.setNegativeButton(getString(R.string.Cancel), null);
        builder.setPositiveButton(getString(R.string.TeleFlowApply), (dialog, which) -> applyPreview());
        showDialog(builder.create());
    }

    private void applyPreview() {
        if (applyButton == null || statusView == null) return;
        applyButton.setEnabled(false);
        applyButton.setAlpha(0.5f);
        statusView.setText(getString(R.string.TeleFlowApplying));
        SmartFoldersBatchApply.apply(this, currentAccount, preview, reviewFolder, result -> {
            applyButton.setEnabled(true);
            applyButton.setAlpha(1f);
            if (result.isSuccess()) {
                statusView.setText(getString(R.string.TeleFlowApplySuccess));
            } else {
                statusView.setText(getString(R.string.TeleFlowApplyFailed) + "\n" + result.getCode() + (result.getDetail().isEmpty() ? "" : ": " + result.getDetail()));
            }
        });
    }
}
