package com.hoangquanghuy.papervault;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hoangquanghuy.papervault.data.local.AppDatabase;
import com.hoangquanghuy.papervault.data.local.DocumentEntity;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class xemtailieu extends AppCompatActivity {

    public static final String EXTRA_DOCUMENT_ID = "document_id";

    private TextView txtTitle;
    private TextView txtInfo;
    private TextView txtError;
    private ImageView imgPreview;
    private ProgressBar progressDecrypt;
    private Button btnRetry;

    private AppDatabase database;
    private ExecutorService executor;
    private String documentId;
    private File decryptedFile;
    private Button btnDelete;
    private DocumentEntity currentDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xemgiayto);

        txtTitle = findViewById(R.id.txtViewDocumentTitle);
        txtInfo = findViewById(R.id.txtViewDocumentInfo);
        txtError = findViewById(R.id.txtPreviewError);
        imgPreview = findViewById(R.id.imgDocumentPreview);
        progressDecrypt = findViewById(R.id.progressDecrypt);
        btnRetry = findViewById(R.id.btnRetryPreview);
        btnDelete = findViewById(R.id.btnDeleteDocument);

        btnDelete.setOnClickListener(view -> showDeleteConfirmation());
        documentId = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);

        if (documentId == null) {
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        btnRetry.setOnClickListener(view -> loadAndDecryptDocument());

        loadAndDecryptDocument();
    }

    private void loadAndDecryptDocument() {
        if (!phiencuakho.isUnlocked()) {
            showError("Vault đã khóa. Hãy quay lại và mở khóa lại.");
            return;
        }

        progressDecrypt.setVisibility(View.VISIBLE);
        imgPreview.setVisibility(View.GONE);
        txtError.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);
        executor.execute(() -> {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    throw new IllegalStateException("Chưa đăng nhập.");
                }

                DocumentEntity document =
                        database.documentDao().getById(documentId);

                if (document == null ||
                        !user.getUid().equals(document.getOwnerUid())) {
                    throw new IllegalStateException("Không có quyền xem giấy tờ.");
                }

                decryptedFile = mahoafile.decryptToCache(
                        this,
                        document.getLocalPath(),
                        document.getId(),
                        phiencuakho.requireFileEncryptionKey(),
                        document.getMimeType()
                );

                Bitmap previewBitmap = createPreview(
                        decryptedFile,
                        document.getMimeType()
                );

                runOnUiThread(() -> {
                    txtTitle.setText(document.getTitle());
                    txtInfo.setText(
                            document.getCategory()
                                    + "\n"
                                    + document.getMimeType()
                    );
                    currentDocument = document;
                    btnDelete.setVisibility(View.VISIBLE);
                    imgPreview.setImageBitmap(previewBitmap);
                    imgPreview.setVisibility(View.VISIBLE);
                    progressDecrypt.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                runOnUiThread(() -> showError(
                        "Không thể giải mã hoặc xem tệp này."
                ));
            }
        });
    }

    private Bitmap createPreview(
            File file,
            String mimeType
    ) throws Exception {

        if (mimeType.startsWith("image/")) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

            if (bitmap == null) {
                throw new IllegalStateException("Không đọc được ảnh.");
            }

            return bitmap;
        }

        if ("application/pdf".equals(mimeType)) {
            ParcelFileDescriptor descriptor =
                    ParcelFileDescriptor.open(
                            file,
                            ParcelFileDescriptor.MODE_READ_ONLY
                    );

            PdfRenderer renderer = new PdfRenderer(descriptor);
            PdfRenderer.Page page = renderer.openPage(0);

            Bitmap bitmap = Bitmap.createBitmap(
                    page.getWidth() * 2,
                    page.getHeight() * 2,
                    Bitmap.Config.ARGB_8888
            );

            page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            );

            page.close();
            renderer.close();
            descriptor.close();

            return bitmap;
        }

        throw new IllegalStateException("Định dạng tệp chưa hỗ trợ.");
    }

    private void showError(String message) {
        progressDecrypt.setVisibility(View.GONE);
        imgPreview.setVisibility(View.GONE);

        txtError.setText(message);
        txtError.setVisibility(View.VISIBLE);

        btnRetry.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executor != null) {
            executor.shutdown();
        }

        if (decryptedFile != null && decryptedFile.exists()) {
            decryptedFile.delete();
        }
    }
    private void showDeleteConfirmation() {
        if (currentDocument == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xóa giấy tờ?")
                .setMessage(
                        "Bản mã hóa của giấy tờ sẽ bị xóa khỏi ứng dụng và không thể khôi phục."
                )
                .setNegativeButton("Hủy", null)
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) -> deleteDocument()
                )
                .show();
    }

    private void deleteDocument() {
        if (currentDocument == null) {
            return;
        }

        btnDelete.setEnabled(false);
        btnDelete.setText("Đang xóa...");

        executor.execute(() -> {
            try {
                File encryptedFile = new File(
                        currentDocument.getLocalPath()
                );

                if (encryptedFile.exists() && !encryptedFile.delete()) {
                    throw new IllegalStateException(
                            "Không thể xóa tệp mã hóa."
                    );
                }

                database.documentDao().delete(currentDocument);

                if (decryptedFile != null && decryptedFile.exists()) {
                    decryptedFile.delete();
                }

                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            "Đã xóa giấy tờ.",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnDelete.setEnabled(true);
                    btnDelete.setText("Xóa giấy tờ");

                    Toast.makeText(
                            this,
                            "Không thể xóa giấy tờ.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (!phiencuakho.isUnlocked()) {
            finish();
        }
    }
}