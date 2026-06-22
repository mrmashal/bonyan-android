package org.bonyan.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.bonyan.data.local.entity.Person;

import java.util.List;

/**
 * Data Access Object for Person entity.
 * Provides methods to interact with the persons table in the database.
 */
@Dao
public interface PersonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Person person);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Person> persons);

    @Update
    void update(Person person);

    @Delete
    void delete(Person person);

    @Query("SELECT * FROM persons WHERE id = :id")
    Person getById(String id);

    @Query("SELECT * FROM persons ORDER BY name ASC")
    List<Person> getAll();

    @Query("SELECT * FROM persons WHERE last_synced_at < :timestamp")
    List<Person> getOutdated(long timestamp);

    @Query("SELECT COUNT(*) FROM persons")
    int getCount();

    @Query("DELETE FROM persons")
    void deleteAll();
}
