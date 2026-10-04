package com.hoangquanghuy.papervault.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DocumentEntity document);

    @Update
    void update(DocumentEntity document);

    @Delete
    void delete(DocumentEntity document);

    @Query("SELECT * FROM documents " +
            "WHERE owner_uid = :ownerUid " +
            "ORDER BY updated_at DESC")
    List<DocumentEntity> getAllByOwner(String ownerUid);

    @Query("SELECT * FROM documents " +
            "WHERE owner_uid = :ownerUid " +
            "AND (title LIKE '%' || :keyword || '%' " +
            "OR category LIKE '%' || :keyword || '%') " +
            "ORDER BY updated_at DESC")
    List<DocumentEntity> searchDocuments(
            String ownerUid,
            String keyword
    );

    @Query("SELECT * FROM documents " +
            "WHERE id = :documentId LIMIT 1")
    DocumentEntity getById(String documentId);
}