package com.hoangquanghuy.papervault;

import java.io.File;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hoangquanghuy.papervault.data.local.AppDatabase;
import com.hoangquanghuy.papervault.data.local.DocumentEntity;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddDocumentActivity extends AppCompatActivity {

    private EditText edtDocumentTitle;
    private EditText edtDocumentCategory;
    private TextView txtSelectedFile;
    private TextView txtFileError;
    private Button btnChooseFile;
    private Button btnSaveDocument;

    private Uri selectedFileUri;
    private ExecutorService databaseExecutor;
    private AppDatabase database;
    private String ownerUid;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        VaultSession.setFilePickerOpen(false);
                        if (uri == null) {
                            return;
                        }

                        selectedFileUri = uri;



                        txtSelectedFile.setText("Đã chọn: " + getFileName(uri));
                        txtFileError.setVisibility(View.GONE);
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        edtDocumentTitle = findViewById(R.id.edtDocumentTitle);
        edtDocumentCategory = findViewById(R.id.edtDocumentCategory);
        txtSelectedFile = findViewById(R.id.txtSelectedFile);
        txtFileError = findViewById(R.id.txtFileError);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        btnSaveDocument = findViewById(R.id.btnSaveDocument);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        ownerUid = user.getUid();
        database = AppDatabase.getInstance(this);
        databaseExecutor = Executors.newSingleThreadExecutor();

        btnChooseFile.setOnClickListener(v -> {
            VaultSession.setFilePickerOpen(true);
            filePickerLauncher.launch(new String[]{"image/*", "application/pdf"});
        });

        btnSaveDocument.setOnClickListener(view -> saveDocument());
    }

    private void saveDocument() {
        String title = edtDocumentTitle.getText().toString().trim();
        String category = edtDocumentCategory.getText().toString().trim();

        if (title.isEmpty()) {
            edtDocumentTitle.setError("Hãy nhập tên giấy tờ.");
            edtDocumentTitle.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            edtDocumentCategory.setError("Hãy nhập danh mục.");
            edtDocumentCategory.requestFocus();
            return;
        }

        if (selectedFileUri == null) {
            txtFileError.setVisibility(View.VISIBLE);
            return;
        }

        if (!VaultSession.isUnlocked()) {
            Toast.makeText(
                    this,
                    "Vault đã khóa. Hãy mở khóa lại.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Uri sourceUri = selectedFileUri;

        btnSaveDocument.setEnabled(false);
        btnSaveDocument.setText("Đang mã hóa...");

        databaseExecutor.execute(() -> {
            String encryptedPath = null;

            try {
                String documentId = UUID.randomUUID().toString();
                long now = System.currentTimeMillis();

                String mimeType = getContentResolver().getType(sourceUri);

                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                encryptedPath = FileCryptoManager.encryptFromUri(
                        this,
                        sourceUri,
                        documentId,
                        VaultSession.requireFileEncryptionKey()
                );

                DocumentEntity document = new DocumentEntity(
                        documentId,
                        ownerUid,
                        title,
                        category,
                        encryptedPath,
                        mimeType,
                        now,
                        now
                );

                database.documentDao().insert(document);

                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            "Đã mã hóa và lưu giấy tờ.",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                });

            } catch (Exception e) {
                if (encryptedPath != null) {
                    new File(encryptedPath).delete();
                }

                runOnUiThread(() -> {
                    btnSaveDocument.setEnabled(true);
                    btnSaveDocument.setText("Lưu giấy tờ");

                    Toast.makeText(
                            this,
                            "Không thể mã hóa tệp.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }
    private String getFileName(Uri uri) {
        String fileName = "Tệp đã chọn";

        Cursor cursor = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex);
            }

            cursor.close();
        }

        return fileName;
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (!VaultSession.isUnlocked()) {
            finish();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}