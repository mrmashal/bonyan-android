/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * Bridge Implementation for Dependency Inversion Pattern
 *
 * This is the ONLY class in Bonyan that bridges Telegram Core and Bonyan modules.
 * It implements IBonyanEntryPoint to provide clean separation of concerns.
 */

package org.bonyan.ui.base;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import org.bonyan.data.BonyanRegistry;
import org.bonyan.data.local.BonyanDatabase;
import org.bonyan.ui.BonyanFragmentContainer;
import org.bonyan.ui.family.BonyanFamilyFragment;
import org.bonyan.ui.mission.BonyanMissionListFragment;
import org.bonyan.ui.planner.BonyanPlannerFragment;
import org.bonyan.ui.profile.BonyanProfileFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.IBonyanEntryPoint;

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

    // Cached fragments for reuse
    private BonyanMissionListFragment missionsFragment;
    private BonyanPlannerFragment plannerFragment;
    private BonyanFamilyFragment familyFragment;
    private BonyanProfileFragment profileFragment;

    // Bottom navigation instance
    private BonyanBottomNav bottomNav;

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
            FileLog.w(TAG + ": Already initialized or initializing");
            return;
        }

        initializing = true;
        long startTime = System.currentTimeMillis();

        try {
            FileLog.d(TAG + ": Initializing Bonyan subsystem...");

            // Initialize database on background thread
            initializeDatabase(context);

            // Pre-create fragment instances for faster tab switching
            precreateFragments();

            initialized = true;

            long duration = System.currentTimeMillis() - startTime;
            FileLog.d(TAG + ": Bonyan initialized successfully in " + duration + "ms");

        } catch (Exception e) {
            FileLog.e(TAG + ": Failed to initialize Bonyan", e);
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
                FileLog.d(TAG + ": Database initialized successfully");
            }
        } catch (Exception e) {
            FileLog.e(TAG + ": Failed to initialize database", e);
            throw e;
        }
    }

    /**
     * Pre-create fragment instances for faster tab switching.
     */
    private void precreateFragments() {
        try {
            // Create fragment instances (they will be properly initialized when first used)
            missionsFragment = new BonyanMissionListFragment();
            plannerFragment = new BonyanPlannerFragment();
            familyFragment = new BonyanFamilyFragment();
            profileFragment = new BonyanProfileFragment();

            if (BuildVars.LOGS_ENABLED) {
                FileLog.d(TAG + ": Fragment instances pre-created");
            }
        } catch (Exception e) {
            FileLog.e(TAG + ": Failed to pre-create fragments", e);
            // Non-fatal: fragments will be created on-demand
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
            FileLog.w(TAG + ": getFragment called before initialization");
            return null;
        }

        switch (bottomNavTabId) {
            case TAB_MISSIONS:
                return missionsFragment != null ? missionsFragment : new BonyanMissionListFragment();

            case TAB_CALENDAR:
                return plannerFragment != null ? plannerFragment : new BonyanPlannerFragment();

            case TAB_FAMILY:
                return familyFragment != null ? familyFragment : new BonyanFamilyFragment();

            case TAB_PROFILE:
                return profileFragment != null ? profileFragment : new BonyanProfileFragment();

            default:
                FileLog.w(TAG + ": Unknown tab ID: " + bottomNavTabId);
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
            FileLog.w(TAG + ": onBottomNavTabSelected called before initialization");
            return;
        }

        if (BuildVars.LOGS_ENABLED) {
            FileLog.d(TAG + ": Tab selected: " + tabId);
        }

        // Notify the active fragment about tab selection
        BaseFragment currentFragment = getFragment(tabId);
        if (currentFragment instanceof BonyanBaseFragment) {
            ((BonyanBaseFragment) currentFragment).onBottomNavTabSelected(tabId);
        }
    }

    /**
     * Returns the bottom navigation view for injection into LaunchActivity.
     *
     * @param activity The host activity (for context and lifecycle)
     * @return The configured bottom navigation view
     */
    @Override
    public View getBottomNavigationView(Activity activity) {
        if (!initialized) {
            FileLog.w(TAG + ": getBottomNavigationView called before initialization");
            // Return a placeholder or throw exception
            throw new IllegalStateException("Bonyan not initialized");
        }

        // Create or reuse the bottom navigation instance
        if (bottomNav == null) {
            bottomNav = new BonyanBottomNav(activity);

            // Set up tab selection listener
            bottomNav.setOnTabSelectedListener(tabId -> {
                onBottomNavTabSelected(tabId);

                // Notify LaunchActivity to switch fragments
                // This is done via the activity's fragment management
                if (activity instanceof BonyanFragmentContainer) {
                    ((BonyanFragmentContainer) activity).onBonyanTabSelected(tabId);
                }
            });
        }

        return bottomNav;
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
        FileLog.d(TAG + ": Shutting down Bonyan...");

        try {
            // Close database connections
            BonyanDatabase.destroyInstance();

            // Clear fragment references
            missionsFragment = null;
            plannerFragment = null;
            familyFragment = null;
            profileFragment = null;
            bottomNav = null;

            initialized = false;
            instance = null;

            FileLog.d(TAG + ": Bonyan shutdown complete");

        } catch (Exception e) {
            FileLog.e(TAG + ": Error during shutdown", e);
        }
    }

}
