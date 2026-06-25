package org.bonyan.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

/**
 * Base fragment for all Bonyan UI components.
 * Extends Telegram's BaseFragment to ensure consistent theming and lifecycle management.
 *
 * All Bonyan fragments should extend this class to maintain compatibility with
 * Telegram's navigation system and theme engine.
 */
public abstract class BonyanBaseFragment extends BaseFragment {

    public BonyanBaseFragment() {
        super();
    }

    public BonyanBaseFragment(Bundle args) {
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
        // Ensure theme resources are loaded
        Theme.createDialogsResources(context);
        return super.createView(context);
    }

    /**
     * Get the Bonyan bottom navigation visibility state.
     * Override this to control whether the bottom nav should be visible for this fragment.
     *
     * @return true if bottom navigation should be visible, false otherwise
     */
    public boolean shouldShowBottomNav() {
        return true;
    }

    /**
     * Called when the user selects a tab from the bottom navigation.
     * Override this to handle tab selection events.
     *
     * @param tabId The ID of the selected tab (0-3)
     */
    public void onBottomNavTabSelected(int tabId) {
        // Override in subclass to handle tab selection
    }
}
