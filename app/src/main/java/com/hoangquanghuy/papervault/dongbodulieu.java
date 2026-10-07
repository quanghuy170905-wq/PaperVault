package com.hoangquanghuy.papervault;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.hoangquanghuy.papervault.data.local.DocumentEntity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class dongbodulieu {

    public interface SyncCallback {
        void onSuccess(int syncedCount);

        void onError(Exception exception);
    }

    private dongbodulieu() {
    }

    public static void syncMetadata(
            String ownerUid,
            List<DocumentEntity> documents,
            SyncCallback callback
    ) {
        try {
            FirebaseFirestore firestore =
                    FirebaseFirestore.getInstance();

            WriteBatch batch = firestore.batch();

            for (DocumentEntity document : documents) {
                JSONObject metadata = new JSONObject();

                metadata.put("title", document.getTitle());
                metadata.put("category", document.getCategory());
                metadata.put("mimeType", document.getMimeType());
                metadata.put("createdAt", document.getCreatedAt());
                metadata.put("updatedAt", document.getUpdatedAt());

                String encryptedMetadata =
                        mahoafile.encryptText(
                                metadata.toString(),
                                document.getId(),
                                phiencuakho.requireFileEncryptionKey()
                        );

                Map<String, Object> cloudData = new HashMap<>();

                cloudData.put("metadataCipher", encryptedMetadata);
                cloudData.put("updatedAt", document.getUpdatedAt());
                cloudData.put("schemaVersion", 1);
                cloudData.put("hasEncryptedFileLocal", true);

                DocumentReference reference = firestore
                        .collection("users")
                        .document(ownerUid)
                        .collection("documents")
                        .document(document.getId());

                batch.set(reference, cloudData);
            }

            batch.commit()
                    .addOnSuccessListener(unused ->
                            callback.onSuccess(documents.size())
                    )
                    .addOnFailureListener(callback::onError);

        } catch (Exception e) {
            callback.onError(e);
        }
    }
}