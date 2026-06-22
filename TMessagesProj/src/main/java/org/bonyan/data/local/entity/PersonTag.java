package org.bonyan.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * PersonTag entity representing a tag/attribute associated with a person.
 * Used for extensible attributes like Skills, Economic status, Media preferences, etc.
 */
@Entity(
    tableName = "person_tags",
    foreignKeys = @ForeignKey(
        entity = Person.class,
        parentColumns = "id",
        childColumns = "person_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "person_id"),
        @Index(value = {"person_id", "category"})
    }
)
public class PersonTag {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "person_id")
    private String personId;

    @ColumnInfo(name = "category")
    private String category; // e.g., 'SKILL', 'ECONOMIC', 'MEDIA', 'INTEREST'

    @ColumnInfo(name = "value")
    private String value; // e.g., 'Programming', 'High', 'Cinema'

    @ColumnInfo(name = "display_order")
    private int displayOrder;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public PersonTag() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.displayOrder = 0;
    }

    // Getters and Setters
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
