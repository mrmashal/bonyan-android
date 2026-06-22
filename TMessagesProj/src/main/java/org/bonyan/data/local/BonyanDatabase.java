package org.bonyan.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.bonyan.data.local.dao.MissionDao;
import org.bonyan.data.local.dao.PersonDao;
import org.bonyan.data.local.dao.SyncQueueDao;
import org.bonyan.data.local.entity.Mission;
import org.bonyan.data.local.entity.Person;
import org.bonyan.data.local.entity.SyncQueue;

/**
 * Room Database for Bonyan application.
 * This is the main database class that holds the database and serves as
 * the main access point for the underlying connection.
 *
 * Database version: 1
 * Entities: Person, Mission, SyncQueue
 */
@Database(
    entities = {
        Person.class,
        Mission.class,
        SyncQueue.class
    },
    version = 1,
    exportSchema = false
)
public abstract class BonyanDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "bonyan_database.db";
    private static volatile BonyanDatabase INSTANCE;

    // DAOs
    public abstract PersonDao personDao();
    public abstract MissionDao missionDao();
    public abstract SyncQueueDao syncQueueDao();

    /**
     * Get singleton instance of the database.
     * Thread-safe implementation using double-checked locking.
     *
     * @param context Application context
     * @return BonyanDatabase instance
     */
    public static BonyanDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BonyanDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildDatabase(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Build the Room database with fallback to destructive migration.
     * In production, proper migrations should be implemented.
     *
     * @param context Application context
     * @return BonyanDatabase instance
     */
    private static BonyanDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(
                context,
                BonyanDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build();
    }

    /**
     * Destroy the singleton instance.
     * Call this when the application is destroyed to clean up resources.
     */
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
