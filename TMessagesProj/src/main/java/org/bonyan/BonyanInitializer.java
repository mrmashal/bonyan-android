/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * Initialization point for Bonyan subsystem
 *
 * This class is responsible for:
 * 1. Registering the BonyanEntryPointImpl with ApplicationLoader
 * 2. Initializing the Bonyan subsystem during app startup
 * 3. Providing a single entry point for Bonyan lifecycle management
 */

package org.bonyan;

import android.content.Context;
import android.util.Log;

import org.bonyan.ui.base.BonyanEntryPointImpl;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;

/**
 * Bonyan Initializer - Single entry point for Bonyan subsystem startup.
 *
 * This class implements the Bridge Pattern registration by:
 * 1. Creating the BonyanEntryPointImpl instance
 * 2. Registering it with ApplicationLoader via registerBonyan()
 * 3. Calling initialize() to set up the Bonyan subsystem
 *
 * USAGE:
 * Call BonyanInitializer.init(context) once during Application.onCreate()
 * after ApplicationLoader has been initialized.
 *
 * EXAMPLE (in ApplicationLoader.onCreate() or Application subclass):
 *     BonyanInitializer.init(this);
 *
 * THREAD SAFETY:
 * This class is thread-safe. The init() method can be called from any thread,
 * but it will perform initialization on the calling thread.
 */
public class BonyanInitializer {

    private static final String TAG = "BonyanInitializer";

    // Volatile for thread-safe double-checked locking
    private static volatile boolean initialized = false;
    private static final Object lock = new Object();

    /**
     * Private constructor to prevent instantiation.
     */
    private BonyanInitializer() {
        throw new AssertionError("Cannot instantiate BonyanInitializer");
    }

    /**
     * Initializes the Bonyan subsystem.
     *
     * This method is the single entry point for Bonyan startup. It:
     * 1. Creates the BonyanEntryPointImpl instance
     * 2. Registers it with ApplicationLoader
     * 3. Initializes the Bonyan subsystem
     *
     * This method is thread-safe and idempotent - calling it multiple times
     * will have no effect after the first successful initialization.
     *
     * @param context The application context (not an Activity context)
     * @return true if initialization succeeded or was already initialized, false on failure
     */
    public static boolean init(Context context) {
        // Fast path - already initialized
        if (initialized) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d(TAG + ": Already initialized, skipping");
            }
            return true;
        }

        // Synchronized initialization
        synchronized (lock) {
            // Double-check after acquiring lock
            if (initialized) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d(TAG + ": Already initialized (double-check), skipping");
                }
                return true;
            }

            try {
                long startTime = System.currentTimeMillis();
                FileLog.i(TAG + ": Starting Bonyan initialization...");

                // Validate context
                if (context == null) {
                    throw new IllegalArgumentException("Context cannot be null");
                }

                // Ensure we have application context
                Context appContext = context.getApplicationContext();
                if (appContext == null) {
                    appContext = context;
                }

                // Step 1: Create the Bonyan entry point implementation
                FileLog.d(TAG + ": Creating BonyanEntryPointImpl...");
                BonyanEntryPointImpl entryPoint = new BonyanEntryPointImpl();

                // Step 2: Register with ApplicationLoader (Bridge Pattern)
                FileLog.d(TAG + ": Registering with ApplicationLoader...");
                ApplicationLoader.registerBonyan(entryPoint);

                // Step 3: Initialize the Bonyan subsystem
                FileLog.d(TAG + ": Initializing Bonyan subsystem...");
                entryPoint.initialize(appContext);

                // Mark as initialized
                initialized = true;

                long duration = System.currentTimeMillis() - startTime;
                FileLog.i(TAG + ": Bonyan initialized successfully in " + duration + "ms");

                return true;

            } catch (Exception e) {
                FileLog.e(TAG + ": Failed to initialize Bonyan", e);
                // Don't mark as initialized on failure - allow retry
                return false;
            }
        }
    }

    /**
     * Returns whether Bonyan has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Resets the initialization state. Used primarily for testing.
     * WARNING: This should not be called in production code.
     */
    public static void reset() {
        synchronized (lock) {
            initialized = false;
            FileLog.w(TAG + ": Initialization state reset");
        }
    }
}
