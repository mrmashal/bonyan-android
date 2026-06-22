package org.bonyan.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * FamilyRelation entity representing a relationship between two persons.
 * Used to build family trees and connection networks.
 * Sync status tracks whether the relationship is pending, synced, or confirmed.
 */
@Entity(
    tableName = "family_relations",
    foreignKeys = {
        @ForeignKey(
            entity = Person.class,
            parentColumns = "id",
            childColumns = "person_a_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Person.class,
            parentColumns = "id",
            childColumns = "person_b_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = "person_a_id"),
        @Index(value = "person_b_id"),
        @Index(value = {"person_a_id", "person_b_id"}, unique = true)
    }
)
public class FamilyRelation {

    public static final String SYNC_STATUS_PENDING = "PENDING";
    public static final String SYNC_STATUS_SYNCED = "SYNCED";
    public static final String SYNC_STATUS_CONFIRMED = "CONFIRMED";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "person_a_id")
    private String personAId;

    @ColumnInfo(name = "person_b_id")
    private String personBId;

    @ColumnInfo(name = "relation_type")
    private String relationType; // e.g., 'PARENT', 'CHILD', 'SPOUSE', 'SIBLING'

    @ColumnInfo(name = "sync_status")
    private String syncStatus; // PENDING, SYNCED, CONFIRMED

    @ColumnInfo(name = "requested_by")
    private String requestedBy; // person_id who initiated the relation

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public FamilyRelation() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncStatus = SYNC_STATUS_PENDING;
    }

    // Getters and Setters
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getPersonAId() {
        return personAId;
    }

    public void setPersonAId(String personAId) {
        this.personAId = personAId;
    }

    public String getPersonBId() {
        return personBId;
    }

    public void setPersonBId(String personBId) {
        this.personBId = personBId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
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
