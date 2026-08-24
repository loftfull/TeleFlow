/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

public final class TeleFlowHubActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.TeleFlowSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        root.addView(content, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView status = label(context, getString(R.string.TeleFlowFoundationStatus), 20, true);
        content.addView(status, rowParams(0));

        TextView folders = card(context, getString(R.string.TeleFlowSmartFolders), getString(R.string.TeleFlowSmartFoldersInfo));
        folders.setOnClickListener(v -> presentFragment(new TeleFlowSmartFoldersActivity()));
        content.addView(folders, rowParams(14));

        TextView drive = card(context, getString(R.string.TeleFlowDrive), getString(R.string.TeleFlowDriveInfo));
        drive.setOnClickListener(v -> presentFragment(new TeleFlowDriveActivity()));
        content.addView(drive, rowParams(10));
        return fragmentView;
    }

    private TextView card(Context context, String title, String subtitle) {
        TextView view = label(context, title + "\n" + subtitle, 17, true);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        view.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView label(Context context, String text, int size, boolean medium) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        if (medium) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private LinearLayout.LayoutParams rowParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(topMarginDp);
        return params;
    }
}
