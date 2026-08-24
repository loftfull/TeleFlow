/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

public final class TeleFlowSmartFoldersActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(getString(R.string.TeleFlowSmartFolders));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;
        TextView text = new TextView(context);
        text.setText(getString(R.string.TeleFlowSmartFoldersStatus));
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        text.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        text.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        text.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0);
        root.addView(text, params);
        return fragmentView;
    }
}
