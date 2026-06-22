package org.bonyan.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.bonyan.data.local.entity.SyncQueue;

import java.util.List;

/**
 * Data Access Object for SyncQueue entity.
 * Provides methods to interact with the sync_queue table in the database.
 * This table is used for offline-first sync tracking.
 */
@Dao
public interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SyncQueue syncQueue);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SyncQueue> syncQueueItems);

    @Update
    void update(SyncQueue syncQueue);

    @Delete
    void delete(SyncQueue syncQueue);

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    SyncQueue getById(long id);

    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    List<SyncQueue> getAll();

    @Query("SELECT * FROM sync_queue WHERE entity_type = :entityType ORDER BY created_at ASC")
    List<SyncQueue> getByEntityType(String entityType);

    @Query("SELECT * FROM sync_queue WHERE retry_count < :maxRetries ORDER BY created_at ASC LIMIT :limit")
    List<SyncQueue> getPending(int maxRetries, int limit);

    @Query("SELECT COUNT(*) FROM sync_queue")
    int getCount();

    @Query("DELETE FROM sync_queue WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM sync_queue")
    void deleteAll();

    @Query("UPDATE sync_queue SET retry_count = retry_count + 1, last_attempt_at = :timestamp WHERE id = :id")
    void incrementRetryCount(long id, long timestamp);
}
