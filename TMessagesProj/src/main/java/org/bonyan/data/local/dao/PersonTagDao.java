package org.bonyan.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.bonyan.data.local.entity.PersonTag;

import java.util.List;

/**
 * Data Access Object for PersonTag entity.
 * Provides methods to interact with the person_tags table in the database.
 */
@Dao
public interface PersonTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PersonTag tag);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PersonTag> tags);

    @Update
    void update(PersonTag tag);

    @Delete
    void delete(PersonTag tag);

    @Query("SELECT * FROM person_tags WHERE id = :id")
    PersonTag getById(String id);

    @Query("SELECT * FROM person_tags WHERE person_id = :personId ORDER BY category, display_order")
    List<PersonTag> getByPersonId(String personId);

    @Query("SELECT * FROM person_tags WHERE person_id = :personId AND category = :category ORDER BY display_order")
    List<PersonTag> getByPersonIdAndCategory(String personId, String category);

    @Query("SELECT * FROM person_tags ORDER BY person_id, category, display_order")
    List<PersonTag> getAll();

    @Query("DELETE FROM person_tags WHERE person_id = :personId")
    void deleteByPersonId(String personId);

    @Query("DELETE FROM person_tags")
    void deleteAll();
}
