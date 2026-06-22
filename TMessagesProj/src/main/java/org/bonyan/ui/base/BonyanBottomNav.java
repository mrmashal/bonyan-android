package org.bonyan.ui.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Bonyan Bottom Navigation component.
 * Provides a custom bottom navigation bar integrated with Telegram's UI.
 * This is a simplified implementation that doesn't rely on Material Design components.
 */
public class BonyanBottomNav extends FrameLayout {

    private LinearLayout container;
    private OnTabSelectedListener tabSelectedListener;
    private int selectedTabId = 0;

    // Tab configurations
    private final String[] tabLabels = {"Missions", "Calendar", "Family", "Profile"};
    private final int[] tabIcons = {
        android.R.drawable.ic_menu_agenda,
        android.R.drawable.ic_menu_month,
        android.R.drawable.ic_menu_myplaces,
        android.R.drawable.ic_menu_preferences
    };

    public interface OnTabSelectedListener {
        void onTabSelected(int tabId);
    }

    public BonyanBottomNav(Context context) {
        this(context, null);
    }

    public BonyanBottomNav(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Create container for tabs
        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);

        // Set background color from Telegram theme
        int backgroundColor = Theme.getColor(Theme.key_windowBackgroundWhite);
        setBackgroundColor(backgroundColor);

        // Create tabs
        for (int i = 0; i < tabLabels.length; i++) {
            final int tabId = i;
            LinearLayout tab = createTab(context, i);
            tab.setOnClickListener(v -> {
                if (tabSelectedListener != null) {
                    tabSelectedListener.onTabSelected(tabId);
                }
                setSelectedTab(tabId);
            });
            container.addView(tab, new LinearLayout.LayoutParams(
                0, LayoutHelper.MATCH_PARENT, 1.0f
            ));
        }

        // Add container to frame
        addView(container, LayoutHelper.createFrame(
            LayoutHelper.MATCH_PARENT,
            LayoutHelper.MATCH_PARENT
        ));

        // Set initial selection
        setSelectedTab(0);

        // Set elevation for shadow
        setElevation(AndroidUtilities.dp(4));
    }

    private LinearLayout createTab(Context context, int index) {
        LinearLayout tab = new LinearLayout(context);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);

        // Icon
        android.widget.ImageView icon = new android.widget.ImageView(context);
        icon.setImageResource(tabIcons[index]);
        icon.setColorFilter(Theme.getColor(Theme.key_chat_messagePanelIcons));
        int iconSize = AndroidUtilities.dp(24);
        tab.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        // Label
        TextView label = new TextView(context);
        label.setText(tabLabels[index]);
        label.setTextSize(10);
        label.setTextColor(Theme.getColor(Theme.key_chat_messagePanelIcons));
        label.setGravity(Gravity.CENTER);
        tab.addView(label, new LinearLayout.LayoutParams(
            LayoutHelper.WRAP_CONTENT,
            LayoutHelper.WRAP_CONTENT
        ));

        return tab;
    }

    private void setSelectedTab(int tabId) {
        this.selectedTabId = tabId;

        // Update visual state of tabs
        for (int i = 0; i < container.getChildCount(); i++) {
            LinearLayout tab = (LinearLayout) container.getChildAt(i);
            boolean isSelected = (i == tabId);

            // Update icon color
            android.widget.ImageView icon = (android.widget.ImageView) tab.getChildAt(0);
            icon.setColorFilter(isSelected
                ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon)
                : Theme.getColor(Theme.key_chat_messagePanelIcons));

            // Update label color
            TextView label = (TextView) tab.getChildAt(1);
            label.setTextColor(isSelected
                ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon)
                : Theme.getColor(Theme.key_chat_messagePanelIcons));
        }
    }

    /**
     * Set the listener for tab selection events.
     *
     * @param listener The listener to be called when a tab is selected
     */
    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.tabSelectedListener = listener;
    }

    /**
     * Select a specific tab programmatically.
     *
     * @param tabId The ID of the tab to select (0-3)
     */
    public void selectTab(int tabId) {
        if (tabId >= 0 && tabId < tabLabels.length) {
            setSelectedTab(tabId);
            if (tabSelectedListener != null) {
                tabSelectedListener.onTabSelected(tabId);
            }
        }
    }

    /**
     * Show or hide the bottom navigation.
     *
     * @param visible true to show, false to hide
     */
    public void setVisible(boolean visible) {
        setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
