package org.bonyan.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.bonyan.data.local.entity.Mission;

import java.util.List;

/**
 * Data Access Object for Mission entity.
 * Provides methods to interact with the missions table in the database.
 */
@Dao
public interface MissionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Mission mission);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Mission> missions);

    @Update
    void update(Mission mission);

    @Delete
    void delete(Mission mission);

    @Query("SELECT * FROM missions WHERE id = :id")
    Mission getById(String id);

    @Query("SELECT * FROM missions ORDER BY created_at DESC")
    List<Mission> getAll();

    @Query("SELECT * FROM missions WHERE status = :status ORDER BY created_at DESC")
    List<Mission> getByStatus(String status);

    @Query("SELECT * FROM missions WHERE parent_mission_id = :parentId ORDER BY created_at DESC")
    List<Mission> getByParentId(String parentId);

    @Query("SELECT * FROM missions WHERE org_id = :orgId ORDER BY created_at DESC")
    List<Mission> getByOrgId(String orgId);

    @Query("SELECT * FROM missions WHERE last_synced_at < :timestamp")
    List<Mission> getOutdated(long timestamp);

    @Query("SELECT COUNT(*) FROM missions")
    int getCount();

    @Query("DELETE FROM missions")
    void deleteAll();
}
