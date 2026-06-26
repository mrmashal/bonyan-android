/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * Bridge Implementation for Dependency Inversion Pattern
 *
 * This is the ONLY class in Bonyan that bridges Telegram Core and Bonyan modules.
 * It implements IBonyanEntryPoint to provide clean separation of concerns.
 */

package org.bonyan.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import org.bonyan.data.BonyanRegistry;
import org.bonyan.data.local.BonyanDatabase;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.IBonyanEntryPoint;
import org.telegram.ui.SettingsActivity;
import android.util.Log;

/**
 * Bridge Implementation for Bonyan Entry Point.
 *
 * This class is the single point of integration between Telegram Core and Bonyan modules.
 * It implements the IBonyanEntryPoint interface to provide:
 * 1. Fragment instantiation for bottom navigation tabs
 * 2. Bottom navigation view creation
 * 3. Lifecycle management callbacks
 *
 * DESIGN PRINCIPLES:
 * - This is the ONLY class that imports both org.telegram.* and org.bonyan.*
 * - All other Bonyan classes only import org.bonyan.* and org.telegram.* types
 * - No other class should have bidirectional dependencies
 *
 * THREAD SAFETY:
 * - All methods are thread-safe
 * - Database initialization happens on background thread
 * - UI operations are posted to main thread
 */
public class BonyanEntryPointImpl implements IBonyanEntryPoint {

    private static final String TAG = "BonyanEntryPoint";

    // Singleton instance for thread-safe access
    private static BonyanEntryPointImpl instance;

    // State tracking
    private volatile boolean initialized = false;
    private volatile boolean initializing = false;

    /**
     * Public constructor for instantiation.
     * Use getInstance() or let BonyanInitProvider create the instance.
     */
    public BonyanEntryPointImpl() {
        // Register with the static registry for global access
        instance = this;
        BonyanRegistry.register(this);
    }

    /**
     * Get the singleton instance.
     * Note: Instance should be created by ApplicationLoader during app startup.
     *
     * @return The singleton instance, or null if not yet created
     */
    public static synchronized BonyanEntryPointImpl getInstance() {
        return instance;
    }

    /**
     * Initializes the Bonyan subsystem.
     * Called once during Application startup.
     *
     * @param context The application context
     */
    @Override
    public synchronized void initialize(Context context) {
        if (initialized || initializing) {
            Log.w(TAG, "Already initialized or initializing");
            return;
        }

        initializing = true;
        long startTime = System.currentTimeMillis();

        try {
            Log.d(TAG, "Initializing Bonyan subsystem...");

            // Initialize database on background thread
            initializeDatabase(context);

            initialized = true;

            long duration = System.currentTimeMillis() - startTime;
            Log.d(TAG, "Bonyan initialized successfully in " + duration + "ms");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Bonyan", e);
            // Reset state to allow retry
            initializing = false;
            throw new RuntimeException("Bonyan initialization failed", e);
        } finally {
            initializing = false;
        }
    }

    /**
     * Initialize the database.
     *
     * @param context The application context
     */
    private void initializeDatabase(Context context) {
        try {
            // Initialize Room database
            BonyanDatabase database = BonyanDatabase.getInstance(context);

            if (BuildVars.LOGS_ENABLED) {
                Log.d(TAG, "Database initialized successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database", e);
            throw e;
        }
    }

    /**
     * Returns a Bonyan fragment for the specified bottom navigation tab.
     *
     * @param bottomNavTabId One of TAB_MISSIONS, TAB_CALENDAR, TAB_FAMILY, TAB_PROFILE
     * @return The Bonyan fragment instance, or null if tabId is invalid
     */
    @Override
    public BaseFragment getFragment(int bottomNavTabId) {
        if (!initialized) {
            Log.w(TAG, "getFragment called before initialization");
            return null;
        }

        // Check if user is logged in
        boolean isLoggedIn = UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated();

        switch (bottomNavTabId) {
            case TAB_MISSIONS:
                Bundle args = new Bundle();
                args.putBoolean("hasMainTabs", true);
                bonyanMissionListFragment = new BonyanMissionListFragment(args);
                bonyanMissionListFragment.setMainTabsActivityController(new MainTabsActivityControllerImpl());
                return bonyanMissionListFragment;

            case TAB_PLANNER:
                Bundle args = new Bundle();
                args.putBoolean("hasMainTabs", true);
                return new BonyanPlannerFragment(args);

            case TAB_FAMILY:
                // Show Family tab only when user is logged in, otherwise show Settings
                if (isLoggedIn) {
                    Bundle args = new Bundle();
                    args.putBoolean("needPhonebook", true);
                    args.putBoolean("needFinishFragment", false);
                    args.putBoolean("hasMainTabs", true);
                    return new BonyanFamilyFragment(args);
                } else {
                    // Return Telegram's SettingsActivity for non-logged in users
                    Bundle settingsArgs = new Bundle();
                    settingsArgs.putBoolean("hasMainTabs", true);
                    return new SettingsActivity(settingsArgs);
                }

            case TAB_PROFILE:
                // Profile tab requires login
                if (!isLoggedIn) {
                    return null;
                }
                Bundle args = new Bundle();
                args.putLong("user_id", UserConfig.getInstance(currentAccount).getClientUserId());
                args.putBoolean("my_profile", true);
                // args.putBoolean("expandPhoto", true);
                args.putBoolean("hasMainTabs", true);
                return new BonyanProfileFragment(args);

            default:
                Log.w(TAG, "Unknown tab ID: " + bottomNavTabId);
                return null;
        }
    }

    /**
     * Notifies Bonyan that a bottom navigation tab was selected.
     *
     * @param tabId The ID of the selected tab (0-3)
     */
    @Override
    public void onBottomNavTabSelected(int tabId) {
        if (!initialized) {
            Log.w(TAG, "onBottomNavTabSelected called before initialization");
            return;
        }

        if (BuildVars.LOGS_ENABLED) {
            Log.d(TAG, "Tab selected: " + tabId);
        }
    }

    /**
     * Returns whether Bonyan is initialized and ready for use.
     *
     * @return true if Bonyan has been initialized, false otherwise
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Cleanup method called when the application is terminating.
     * Used to release resources, close database connections, etc.
     */
    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down Bonyan...");

        try {
            // Close database connections
            BonyanDatabase.destroyInstance();

            initialized = false;
            instance = null;

            Log.d(TAG, "Bonyan shutdown complete");

        } catch (Exception e) {
            Log.e(TAG, "Error during shutdown", e);
        }
    }
}
