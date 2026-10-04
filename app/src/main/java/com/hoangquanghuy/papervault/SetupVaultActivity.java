package com.hoangquanghuy.papervault;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hoangquanghuy.papervault.PasswordManager;

public class SetupVaultActivity extends AppCompatActivity {

    private EditText edtMasterPassword;
    private EditText edtConfirmPassword;
    private Button btnCreateVault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_vault);

        edtMasterPassword = findViewById(R.id.edtMasterPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnCreateVault = findViewById(R.id.btnCreateVault);

        btnCreateVault.setOnClickListener(v -> createVault());
    }

    private void createVault() {
        String password = edtMasterPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (password.length() < 12) {
            edtMasterPassword.setError(
                    "Master Password cần ít nhất 12 ký tự"
            );
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        PasswordManager.createVault(this, password);

        Toast.makeText(
                this,
                "Đã thiết lập kho dữ liệu",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent = new Intent(
                SetupVaultActivity.this,
                VaultHomeActivity.class
        );
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                Toast.makeText(
                        this,
                        "Không tìm thấy tài khoản đăng nhập.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            VaultSession.unlock(this, user.getUid(), password);

            startActivity(new Intent(this, VaultHomeActivity.class));
            finish();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Không thể khởi tạo khóa mã hóa.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}