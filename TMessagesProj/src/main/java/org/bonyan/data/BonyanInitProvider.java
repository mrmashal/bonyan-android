/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * ContentProvider for automatic initialization
 *
 * This ContentProvider enables auto-initialization of Bonyan without requiring
 * any modifications to Telegram's ApplicationLoader or other core files.
 * It runs before Application.onCreate() and initializes the Bonyan subsystem.
 */

package org.bonyan.data;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.bonyan.ui.base.BonyanEntryPointImpl;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;

/**
 * BonyanInitProvider - Auto-initialization via ContentProvider.
 *
 * This ContentProvider is registered in AndroidManifest.xml and runs automatically
 * when the app process starts, before Application.onCreate(). This allows Bonyan
 * to initialize without any code changes to Telegram's core files.
 *
 * USAGE:
 * 1. This ContentProvider is automatically invoked by Android on app startup
 * 2. It creates and initializes the BonyanEntryPointImpl
 * 3. LaunchActivity can then access Bonyan via BonyanRegistry.get()
 *
 * ADVANTAGES:
 * - ZERO modifications to Telegram core files
 * - No build performance impact on ApplicationLoader
 * - Clean separation of concerns
 * - Works with incremental builds
 */
public class BonyanInitProvider extends ContentProvider {

    private static final String TAG = "BonyanInitProvider";

    /**
     * Called when the ContentProvider is created.
     * This runs before Application.onCreate() and initializes Bonyan.
     *
     * @return true if initialization succeeded
     */
    @Override
    public boolean onCreate() {
        try {
            long startTime = System.currentTimeMillis();

            Context context = getContext();
            if (context == null) {
                Log.e(TAG, "Context is null, cannot initialize Bonyan");
                return false;
            }

            // Get the Application context - this will be the Application object
            // ContentProvider runs before Application.onCreate(), but we can
            // still get the Application context via getApplicationContext()
            Application app = (Application) context.getApplicationContext();

            if (BuildVars.LOGS_ENABLED) {
                Log.d(TAG, "Initializing Bonyan via ContentProvider...");
            }

            // Create the Bonyan entry point - this auto-registers with BonyanRegistry
            BonyanEntryPointImpl entryPoint = new BonyanEntryPointImpl();

            // Initialize the entry point with the application context
            entryPoint.initialize(app);

            long duration = System.currentTimeMillis() - startTime;
            if (BuildVars.LOGS_ENABLED) {
                Log.d(TAG, "Bonyan initialized successfully in " + duration + "ms");
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Bonyan", e);
            // Return true anyway so the app can continue without Bonyan
            return true;
        }
    }

    // Required ContentProvider overrides - not used for initialization

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
