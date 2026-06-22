package org.bonyan.data.local;

import android.content.Context;

/**
 * Provider class for BonyanDatabase singleton access.
 * Provides a clean interface for accessing the database instance throughout the app.
 */
public class BonyanDatabaseProvider {

    private static volatile BonyanDatabaseProvider instance;
    private BonyanDatabase database;

    private BonyanDatabaseProvider() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the singleton instance of BonyanDatabaseProvider.
     *
     * @return BonyanDatabaseProvider instance
     */
    public static BonyanDatabaseProvider getInstance() {
        if (instance == null) {
            synchronized (BonyanDatabaseProvider.class) {
                if (instance == null) {
                    instance = new BonyanDatabaseProvider();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize the database. Must be called once before accessing the database.
     *
     * @param context Application context
     */
    public void initialize(Context context) {
        if (database == null) {
            database = BonyanDatabase.getInstance(context.getApplicationContext());
        }
    }

    /**
     * Get the BonyanDatabase instance.
     * Make sure to call initialize() first.
     *
     * @return BonyanDatabase instance
     * @throws IllegalStateException if not initialized
     */
    public BonyanDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("BonyanDatabase not initialized. Call initialize() first.");
        }
        return database;
    }

    /**
     * Check if the database has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return database != null;
    }

    /**
     * Destroy the database instance.
     * Call this when the application is being destroyed to clean up resources.
     */
    public void destroy() {
        if (database != null) {
            BonyanDatabase.destroyInstance();
            database = null;
        }
        instance = null;
    }
}
