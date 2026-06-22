package org.bonyan.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * SyncQueue entity for tracking offline changes that need to be synchronized
 * with the remote server. This enables the offline-first architecture.
 */
@Entity(tableName = "sync_queue")
public class SyncQueue {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "entity_type")
    private String entityType; // e.g., "PERSON", "MISSION", "FAMILY_RELATION"

    @NonNull
    @ColumnInfo(name = "entity_id")
    private String entityId;

    @NonNull
    @ColumnInfo(name = "action")
    private String action; // INSERT, UPDATE, DELETE

    @ColumnInfo(name = "payload_json")
    private String payloadJson; // JSON representation of the entity

    @ColumnInfo(name = "retry_count")
    private int retryCount;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "last_attempt_at")
    private long lastAttemptAt;

    public SyncQueue() {
        this.createdAt = System.currentTimeMillis();
        this.retryCount = 0;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(@NonNull String entityType) {
        this.entityType = entityType;
    }

    @NonNull
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(@NonNull String entityId) {
        this.entityId = entityId;
    }

    @NonNull
    public String getAction() {
        return action;
    }

    public void setAction(@NonNull String action) {
        this.action = action;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(long lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
}
