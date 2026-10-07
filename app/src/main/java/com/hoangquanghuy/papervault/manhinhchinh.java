package com.hoangquanghuy.papervault;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hoangquanghuy.papervault.data.local.AppDatabase;
import com.hoangquanghuy.papervault.data.local.DocumentEntity;
import com.hoangquanghuy.papervault.ui.adapter.DocumentAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class manhinhchinh extends AppCompatActivity {

    private TextView txtWelcome;
    private TextView txtDocumentCount;
    private TextView txtEmptyState;

    private RecyclerView recyclerDocuments;
    private ImageButton btnAddDocument;
    private ImageButton btnSyncMetadata;
    private ImageButton btnLockVault;
    private ImageButton btnLogout;
    private AppDatabase database;
    private ExecutorService databaseExecutor;
    private String ownerUid;
    private EditText edtSearchDocuments;
    private final List<DocumentEntity> documentList = new ArrayList<>();
    private DocumentAdapter documentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manhinhchinh);
        btnSyncMetadata = findViewById(R.id.btnSyncMetadata);
        btnSyncMetadata.setOnClickListener(view -> syncMetadata());
        txtWelcome = findViewById(R.id.txtWelcome);
        txtDocumentCount = findViewById(R.id.txtDocumentCount);
        txtEmptyState = findViewById(R.id.txtEmptyState);
        btnAddDocument = findViewById(R.id.btnAddDocument);
        recyclerDocuments = findViewById(R.id.recyclerDocuments);
        btnLockVault = findViewById(R.id.btnLockVault);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> logout());
        btnLockVault.setOnClickListener(view -> lockVaultNow());
        edtSearchDocuments = findViewById(R.id.edtSearchDocuments);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            startActivity(new Intent(this, dangnhap.class));
            finish();
            return;
        }

        ownerUid = firebaseUser.getUid();

        String displayName = firebaseUser.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = firebaseUser.getEmail();
        }

        txtWelcome.setText("Xin chào, " + displayName);

        database = AppDatabase.getInstance(this);
        databaseExecutor = Executors.newSingleThreadExecutor();

        recyclerDocuments.setLayoutManager(new LinearLayoutManager(this));

        documentAdapter = new DocumentAdapter(
                documentList,
                document -> {
                    Intent intent = new Intent(
                            manhinhchinh.this,
                            xemtailieu.class
                    );

                    intent.putExtra(
                            xemtailieu.EXTRA_DOCUMENT_ID,
                            document.getId()
                    );

                    startActivity(intent);
                }
        );
        recyclerDocuments.setAdapter(documentAdapter);

        btnAddDocument.setOnClickListener(view -> {
            startActivity(new Intent(this, themtailieu.class));
        });

        loadDocuments("");
    }

    private void loadDocuments(String keyword) {
        String searchKeyword = keyword.trim();

        databaseExecutor.execute(() -> {
            List<DocumentEntity> documents;

            if (searchKeyword.isEmpty()) {
                documents = database.documentDao()
                        .getAllByOwner(ownerUid);
            } else {
                documents = database.documentDao()
                        .searchDocuments(ownerUid, searchKeyword);
            }

            runOnUiThread(() -> {
                documentList.clear();
                documentList.addAll(documents);
                documentAdapter.notifyDataSetChanged();

                if (searchKeyword.isEmpty()) {
                    txtDocumentCount.setText(
                            "Số giấy tờ đang lưu: " + documents.size()
                    );
                } else {
                    txtDocumentCount.setText(
                            "Kết quả tìm kiếm: " + documents.size()
                    );
                }

                if (documents.isEmpty()) {
                    if (searchKeyword.isEmpty()) {
                        txtEmptyState.setText(
                                "Chưa có giấy tờ nào.\nHãy thêm một giấy tờ để bắt đầu."
                        );
                    } else {
                        txtEmptyState.setText(
                                "Không tìm thấy giấy tờ phù hợp.\nThử tên hoặc danh mục khác."
                        );
                    }

                    txtEmptyState.setVisibility(View.VISIBLE);
                } else {
                    txtEmptyState.setVisibility(View.GONE);
                }
            });
            edtSearchDocuments.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(
                        CharSequence s,
                        int start,
                        int count,
                        int after
                ) {
                }

                @Override
                public void onTextChanged(
                        CharSequence s,
                        int start,
                        int before,
                        int count
                ) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    loadDocuments(editable.toString().trim());
                }
            });
        });
    }

    private void addSampleDocument() {
        btnAddDocument.setEnabled(false);
        btnAddDocument.setAlpha(0.45f);

        databaseExecutor.execute(() -> {
            long now = System.currentTimeMillis();

            DocumentEntity sampleDocument = new DocumentEntity(
                    UUID.randomUUID().toString(),
                    ownerUid,
                    "CCCD mẫu",
                    "Giấy tờ cá nhân",
                    "sample/cccd_mau.jpg",
                    "image/jpeg",
                    now,
                    now
            );

            database.documentDao().insert(sampleDocument);

            runOnUiThread(() -> {
                btnAddDocument.setEnabled(true);
                btnAddDocument.setAlpha(0.45f);
                loadDocuments("");
            });
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (!phiencuakho.isUnlocked()) {
            startActivity(new Intent(this, mokhoa.class));
            finish();
            return;
        }

        if (database != null && ownerUid != null) {
            loadDocuments(
                    edtSearchDocuments.getText().toString().trim()
            );
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
    private void syncMetadata() {
        if (!phiencuakho.isUnlocked()) {
            Toast.makeText(
                    this,
                    "Vault đã khóa. Hãy mở khóa lại.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnSyncMetadata.setEnabled(false);
        btnSyncMetadata.setAlpha(0.45f);

        databaseExecutor.execute(() -> {
            List<DocumentEntity> documents = database
                    .documentDao()
                    .getAllByOwner(ownerUid);

            dongbodulieu.syncMetadata(
                    ownerUid,
                    documents,
                    new dongbodulieu.SyncCallback() {
                        @Override
                        public void onSuccess(int syncedCount) {
                            runOnUiThread(() -> {
                                btnSyncMetadata.setEnabled(true);
                                btnSyncMetadata.setAlpha(0.45f);


                                Toast.makeText(
                                        manhinhchinh.this,
                                        "Đã đồng bộ "
                                                + syncedCount
                                                + " giấy tờ đã mã hóa.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        }

                        @Override
                        public void onError(Exception exception) {
                            runOnUiThread(() -> {
                                btnSyncMetadata.setEnabled(true);
                                btnSyncMetadata.setAlpha(0.45f);


                                Toast.makeText(
                                        manhinhchinh.this,
                                        "Đồng bộ thất bại. Kiểm tra Internet và Firestore Rules.",
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }
                    }
            );
        });
    }
    private void lockVaultNow() {
        phiencuakho.lock();

        Intent intent = new Intent(
                this,
                mokhoa.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
    private void logout() {
        phiencuakho.lock();
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, dangnhap.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}