package com.hoangquanghuy.papervault.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "documents")
public class DocumentEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "owner_uid")
    private String ownerUid;

    @NonNull
    private String title;

    @NonNull
    private String category;

    @NonNull
    @ColumnInfo(name = "local_path")
    private String localPath;

    @NonNull
    @ColumnInfo(name = "mime_type")
    private String mimeType;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public DocumentEntity(
            @NonNull String id,
            @NonNull String ownerUid,
            @NonNull String title,
            @NonNull String category,
            @NonNull String localPath,
            @NonNull String mimeType,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.ownerUid = ownerUid;
        this.title = title;
        this.category = category;
        this.localPath = localPath;
        this.mimeType = mimeType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getOwnerUid() {
        return ownerUid;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    @NonNull
    public String getLocalPath() {
        return localPath;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}