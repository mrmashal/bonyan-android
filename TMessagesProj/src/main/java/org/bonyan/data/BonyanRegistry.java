/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * Static registry for Bonyan Entry Point instance
 *
 * This class provides a static holder for the IBonyanEntryPoint instance,
 * allowing LaunchActivity and other components to access Bonyan functionality
 * without direct dependency on ApplicationLoader.
 */

package org.bonyan.data;

import org.telegram.ui.IBonyanEntryPoint;

/**
 * BonyanRegistry - Static registry for the Bonyan entry point.
 *
 * This class maintains a static reference to the IBonyanEntryPoint implementation,
 * allowing decoupled access from LaunchActivity and other UI components.
 *
 * The registration happens in BonyanEntryPointImpl's constructor or via
 * the ContentProvider initialization path.
 *
 * THREAD SAFETY:
 * This class is thread-safe. All operations are synchronized.
 */
public class BonyanRegistry {

    private static final String TAG = "BonyanRegistry";

    // Static holder for the entry point instance
    private static volatile IBonyanEntryPoint instance;

    // Lock object for synchronization
    private static final Object lock = new Object();

    /**
     * Private constructor to prevent instantiation.
     */
    private BonyanRegistry() {
        throw new AssertionError("Cannot instantiate BonyanRegistry");
    }

    /**
     * Registers the Bonyan entry point instance.
     *
     * This method is called once during application startup, typically by
     * BonyanEntryPointImpl's constructor or via ContentProvider initialization.
     *
     * @param entryPoint The Bonyan entry point implementation
     */
    public static void register(IBonyanEntryPoint entryPoint) {
        synchronized (lock) {
            if (instance != null) {
                // Already registered, ignore duplicate registration
                android.util.Log.w(TAG, "Bonyan entry point already registered, ignoring duplicate");
                return;
            }
            instance = entryPoint;
            android.util.Log.d(TAG, "Bonyan entry point registered successfully");
        }
    }

    /**
     * Returns the registered Bonyan entry point instance.
     *
     * @return The Bonyan entry point, or null if not yet registered
     */
    public static IBonyanEntryPoint get() {
        return instance;
    }

    /**
     * Returns whether a Bonyan entry point has been registered.
     *
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered() {
        return instance != null;
    }

    /**
     * Resets the registry. This should ONLY be used for testing.
     * WARNING: Calling this in production will break Bonyan functionality.
     */
    public static void reset() {
        synchronized (lock) {
            instance = null;
            android.util.Log.w(TAG, "Bonyan registry reset");
        }
    }
}
