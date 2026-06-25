package org.bonyan.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Bonyan Mission List Fragment.
 * Displays the list of missions with offline-first capabilities.
 *
 * This is a placeholder implementation that will be expanded in future phases
 * to show actual mission data from the local database.
 */
public class BonyanMissionListFragment extends BonyanBaseFragment {

    private TextView placeholderText;

    public BonyanMissionListFragment() {
        super();
    }

    public BonyanMissionListFragment(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        // Set up the action bar
        actionBar.setBackButtonImage(0);
        actionBar.setTitle("Bonyan Missions");
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        // Create the main layout
        FrameLayout layout = new FrameLayout(context);
        layout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        // Add placeholder text
        placeholderText = new TextView(context);
        placeholderText.setText("Bonyan Missions - Offline First\n\nMission list will be displayed here.");
        placeholderText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        placeholderText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        placeholderText.setGravity(Gravity.CENTER);
        placeholderText.setPadding(
            AndroidUtilities.dp(32),
            AndroidUtilities.dp(32),
            AndroidUtilities.dp(32),
            AndroidUtilities.dp(32)
        );

        layout.addView(placeholderText, LayoutHelper.createFrame(
            LayoutHelper.MATCH_PARENT,
            LayoutHelper.WRAP_CONTENT,
            Gravity.CENTER
        ));

        fragmentView = layout;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load data from local database here in future implementation
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
