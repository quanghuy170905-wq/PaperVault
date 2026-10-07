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

public class dangnhap extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtPassword;
    private Button btnLogin;
    private TextView txtGoToRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dangnhap);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginAccount());

        txtGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    dangnhap.this,
                    dangky.class
            );
            startActivity(intent);
            finish();
        });
    }

    private void loginAccount() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            return;
        }

        if (password.isEmpty()) {
            edtPassword.setError("Hãy nhập mật khẩu");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            openVaultScreen();
                        } else {
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Đăng nhập");

                            Toast.makeText(
                                    dangnhap.this,
                                    "Email hoặc mật khẩu không đúng",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                });
    }

    private void openVaultScreen() {
        Intent intent;

        if (quanlimatkhau.isVaultCreated(this)) {
            intent = new Intent(this, mokhoa.class);
        } else {
            intent = new Intent(this, taomasterpassword.class);
        }

        startActivity(intent);
        finish();
    }
}