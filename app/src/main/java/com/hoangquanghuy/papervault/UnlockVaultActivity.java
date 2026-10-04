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
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;
public class UnlockVaultActivity extends AppCompatActivity {
    private Button btnBiometricUnlock;
    private EditText edtMasterPassword;
    private Button btnUnlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock_vault);
        btnBiometricUnlock = findViewById(R.id.btnBiometricUnlock);

        btnBiometricUnlock.setOnClickListener(
                view -> unlockWithBiometric()
        );
        edtMasterPassword = findViewById(R.id.edtMasterPassword);
        btnUnlock = findViewById(R.id.btnUnlock);

        btnUnlock.setOnClickListener(v -> unlockVault());
    }

    private void unlockVault() {
        String password = edtMasterPassword.getText().toString();

        if (!PasswordManager.verifyPassword(this, password)) {
            edtMasterPassword.setError("Master Password không đúng");
            return;
        }

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

            prepareBiometricThenOpen(user.getUid());
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Không thể mở khóa dữ liệu.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
    private void prepareBiometricThenOpen(String ownerUid) {
        if (!isBiometricAvailable()
                || BiometricKeyManager.hasWrappedKey(this, ownerUid)) {

            openVaultHome();
            return;
        }

        try {
            Cipher cipher = BiometricKeyManager.createEncryptCipher(
                    this,
                    ownerUid
            );

            Executor executor = ContextCompat.getMainExecutor(this);

            BiometricPrompt prompt = new BiometricPrompt(
                    this,
                    executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                BiometricPrompt.AuthenticationResult result
                        ) {
                            super.onAuthenticationSucceeded(result);

                            try {
                                Cipher authenticatedCipher = result
                                        .getCryptoObject()
                                        .getCipher();

                                BiometricKeyManager.saveWrappedFileKey(
                                        UnlockVaultActivity.this,
                                        ownerUid,
                                        authenticatedCipher,
                                        VaultSession.requireFileEncryptionKey()
                                );

                                Toast.makeText(
                                        UnlockVaultActivity.this,
                                        "Đã bật mở khóa bằng vân tay.",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } catch (Exception e) {
                                Toast.makeText(
                                        UnlockVaultActivity.this,
                                        "Không thể thiết lập vân tay.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            openVaultHome();
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errorMessage
                        ) {
                            super.onAuthenticationError(
                                    errorCode,
                                    errorMessage
                            );

                            // Người dùng có thể bỏ qua vân tay,
                            // vì Master Password vừa được xác thực.
                            openVaultHome();
                        }
                    }
            );

            BiometricPrompt.PromptInfo promptInfo =
                    new BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Thiết lập mở khóa vân tay")
                            .setSubtitle(
                                    "Xác thực để bảo vệ khóa Vault trên thiết bị này."
                            )
                            .setNegativeButtonText("Bỏ qua")
                            .build();

            prompt.authenticate(
                    promptInfo,
                    new BiometricPrompt.CryptoObject(cipher)
            );

        } catch (Exception e) {
            openVaultHome();
        }
    }

    private void unlockWithBiometric() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Bạn chưa đăng nhập.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String ownerUid = user.getUid();

        if (!isBiometricAvailable()) {
            Toast.makeText(
                    this,
                    "Thiết bị chưa có vân tay được thiết lập.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!BiometricKeyManager.hasWrappedKey(this, ownerUid)) {
            Toast.makeText(
                    this,
                    "Hãy nhập Master Password một lần để thiết lập vân tay.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        try {
            Cipher cipher = BiometricKeyManager.createDecryptCipher(
                    this,
                    ownerUid
            );

            Executor executor = ContextCompat.getMainExecutor(this);

            BiometricPrompt prompt = new BiometricPrompt(
                    this,
                    executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                BiometricPrompt.AuthenticationResult result
                        ) {
                            super.onAuthenticationSucceeded(result);

                            try {
                                Cipher authenticatedCipher = result
                                        .getCryptoObject()
                                        .getCipher();

                                byte[] keyBytes =
                                        BiometricKeyManager.unwrapFileKey(
                                                UnlockVaultActivity.this,
                                                ownerUid,
                                                authenticatedCipher
                                        );

                                VaultSession.restoreFileEncryptionKey(
                                        keyBytes
                                );

                                openVaultHome();

                            } catch (Exception e) {
                                Toast.makeText(
                                        UnlockVaultActivity.this,
                                        "Không thể mở khóa bằng vân tay.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errorMessage
                        ) {
                            super.onAuthenticationError(
                                    errorCode,
                                    errorMessage
                            );

                            Toast.makeText(
                                    UnlockVaultActivity.this,
                                    "Vân tay chưa được xác thực.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

            BiometricPrompt.PromptInfo promptInfo =
                    new BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Mở khóa PaperVault")
                            .setSubtitle(
                                    "Xác thực vân tay để mở Vault."
                            )
                            .setNegativeButtonText("Dùng Master Password")
                            .build();

            prompt.authenticate(
                    promptInfo,
                    new BiometricPrompt.CryptoObject(cipher)
            );

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Không thể khởi tạo mở khóa vân tay.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private boolean isBiometricAvailable() {
        int result = BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
        );

        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void openVaultHome() {
        startActivity(new Intent(this, VaultHomeActivity.class));
        finish();
    }
}