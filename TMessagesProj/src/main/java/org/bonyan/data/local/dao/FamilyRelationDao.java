package org.bonyan.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.bonyan.data.local.entity.FamilyRelation;

import java.util.List;

/**
 * Data Access Object for FamilyRelation entity.
 * Provides methods to interact with the family_relations table in the database.
 */
@Dao
public interface FamilyRelationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FamilyRelation relation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FamilyRelation> relations);

    @Update
    void update(FamilyRelation relation);

    @Delete
    void delete(FamilyRelation relation);

    @Query("SELECT * FROM family_relations WHERE id = :id")
    FamilyRelation getById(String id);

    @Query("SELECT * FROM family_relations WHERE person_a_id = :personId OR person_b_id = :personId")
    List<FamilyRelation> getByPersonId(String personId);

    @Query("SELECT * FROM family_relations WHERE person_a_id = :personId")
    List<FamilyRelation> getByPersonAId(String personId);

    @Query("SELECT * FROM family_relations WHERE sync_status = :syncStatus")
    List<FamilyRelation> getBySyncStatus(String syncStatus);

    @Query("SELECT * FROM family_relations ORDER BY updated_at DESC")
    List<FamilyRelation> getAll();

    @Query("SELECT COUNT(*) FROM family_relations WHERE sync_status = :syncStatus")
    int countBySyncStatus(String syncStatus);

    @Query("DELETE FROM family_relations WHERE person_a_id = :personId OR person_b_id = :personId")
    void deleteByPersonId(String personId);

    @Query("DELETE FROM family_relations")
    void deleteAll();
}
