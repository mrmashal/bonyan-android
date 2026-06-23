/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * Bridge Interface for Dependency Inversion between Telegram Core and Bonyan Modules
 *
 * CRITICAL: This interface must NOT import any org.bonyan.* classes.
 * It only uses Telegram core types to avoid circular dependencies.
 */

package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import org.telegram.ui.ActionBar.BaseFragment;

/**
 * Bridge Interface for Bonyan Entry Point.
 *
 * This interface defines the contract between Telegram's core (LaunchActivity)
 * and Bonyan's modules. It enables:
 * 1. Dependency Inversion - Telegram core depends on this interface, not Bonyan
 * 2. Logical Isolation - Bonyan modules are isolated but interoperable
 * 3. Minimal Core Changes - Only this interface is added to Telegram core
 *
 * Implementation: org.bonyan.ui.base.BonyanEntryPointImpl
 *
 * ARCHITECTURE RULES:
 * - This interface must only reference Telegram core classes
 * - No org.bonyan.* imports allowed here
 * - All methods must be thread-safe
 * - Context should be ApplicationContext where possible
 */
public interface IBonyanEntryPoint {

    /**
     * Tab IDs for bottom navigation.
     * These must match the implementation in BonyanEntryPointImpl.
     */
    int TAB_MISSIONS = 0;
    int TAB_CALENDAR = 1;
    int TAB_FAMILY = 2;
    int TAB_PROFILE = 3;

    /**
     * Initializes the Bonyan subsystem.
     * Called once during Application startup.
     *
     * This method should:
     * - Initialize database connections
     * - Set up sync workers
     * - Register notification handlers
     * - Perform any one-time setup
     *
     * @param context The application context
     */
    void initialize(Context context);

    /**
     * Returns a Bonyan fragment for the specified bottom navigation tab.
     *
     * This method is called by LaunchActivity when:
     * - User selects a tab from bottom navigation
     * - Activity is recreated (rotation, etc.)
     * - Initial fragment is needed
     *
     * @param bottomNavTabId One of TAB_MISSIONS, TAB_CALENDAR, TAB_FAMILY, TAB_PROFILE
     * @return The Bonyan fragment instance, or null if tabId is invalid
     */
    BaseFragment getFragment(int bottomNavTabId);

    /**
     * Notifies Bonyan that a bottom navigation tab was selected.
     *
     * This allows Bonyan to:
     * - Track user navigation patterns
     * - Update UI state
     * - Trigger lazy loading of data
     * - Handle any tab-specific logic
     *
     * @param tabId The ID of the selected tab (0-3)
     */
    void onBottomNavTabSelected(int tabId);

    /**
     * Returns the bottom navigation view for injection into LaunchActivity.
     *
     * This method creates and configures the Bonyan bottom navigation bar
     * that will be displayed at the bottom of the screen.
     *
     * The returned view should:
     * - Be fully configured with tab labels and icons
     * - Have appropriate styling for the current theme
     * - Include any necessary touch handlers
     *
     * @param activity The host activity (for context and lifecycle)
     * @return The configured bottom navigation view
     */
    View getBottomNavigationView(Activity activity);

    /**
     * Returns whether Bonyan is initialized and ready for use.
     *
     * @return true if Bonyan has been initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * Cleanup method called when the application is terminating.
     * Used to release resources, close database connections, etc.
     */
    void shutdown();
}
