package com.hoangquanghuy.papervault;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class dangky extends AppCompatActivity {

    private EditText edtDisplayName;
    private EditText edtEmail;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private Button btnRegister;
    private TextView txtGoToLogin;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dangky);

        edtDisplayName = findViewById(R.id.edtDisplayName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);

        mAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(v -> registerAccount());

        txtGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(
                    dangky.this,
                    dangnhap.class
            );
            startActivity(intent);
            finish();
        });
    }

    private void registerAccount() {
        String displayName = edtDisplayName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (displayName.isEmpty()) {
            edtDisplayName.setError("Hãy nhập tên hiển thị");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            return;
        }

        if (password.length() < 8) {
            edtPassword.setError("Mật khẩu cần ít nhất 8 ký tự");
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang tạo tài khoản...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            updateDisplayName(displayName);
                        } else {
                            btnRegister.setEnabled(true);
                            btnRegister.setText("Đăng ký");

                            Toast.makeText(
                                    dangky.this,
                                    "Không thể tạo tài khoản: "
                                            + task.getException().getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                });
    }

    private void updateDisplayName(String displayName) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        user.updateProfile(profile).addOnCompleteListener(task -> {
            Toast.makeText(
                    dangky.this,
                    "Đăng ký thành công",
                    Toast.LENGTH_SHORT
            ).show();

            Intent intent = new Intent(
                    dangky.this,
                    taomasterpassword.class
            );
            startActivity(intent);
            finish();
        });
    }
}