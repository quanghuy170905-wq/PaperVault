package com.hoangquanghuy.papervault;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class mokhoavantay {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String PREFS_NAME = "paper_vault_biometric";

    private static final String KEY_ALIAS_PREFIX = "bio_key_";
    private static final String KEY_WRAPPED_DATA_PREFIX = "wrapped_data_";
    private static final String KEY_WRAPPED_IV_PREFIX = "wrapped_iv_";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private mokhoavantay() {
    }

    public static boolean hasWrappedKey(
            Context context,
            String ownerUid
    ) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        return preferences.contains(KEY_WRAPPED_DATA_PREFIX + ownerUid)
                && preferences.contains(KEY_WRAPPED_IV_PREFIX + ownerUid);
    }

    public static Cipher createEncryptCipher(
            Context context,
            String ownerUid
    ) throws Exception {

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        cipher.init(
                Cipher.ENCRYPT_MODE,
                getOrCreateKeystoreKey(ownerUid)
        );

        return cipher;
    }

    public static void saveWrappedFileKey(
            Context context,
            String ownerUid,
            Cipher authenticatedCipher,
            SecretKey fileEncryptionKey
    ) throws Exception {

        byte[] encryptedKey = authenticatedCipher.doFinal(
                fileEncryptionKey.getEncoded()
        );

        SharedPreferences preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        preferences.edit()
                .putString(
                        KEY_WRAPPED_DATA_PREFIX + ownerUid,
                        Base64.encodeToString(
                                encryptedKey,
                                Base64.NO_WRAP
                        )
                )
                .putString(
                        KEY_WRAPPED_IV_PREFIX + ownerUid,
                        Base64.encodeToString(
                                authenticatedCipher.getIV(),
                                Base64.NO_WRAP
                        )
                )
                .apply();
    }

    public static Cipher createDecryptCipher(
            Context context,
            String ownerUid
    ) throws Exception {

        SharedPreferences preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        String savedIv = preferences.getString(
                KEY_WRAPPED_IV_PREFIX + ownerUid,
                null
        );

        if (savedIv == null) {
            throw new IllegalStateException(
                    "Chưa thiết lập mở khóa vân tay."
            );
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKeystoreKey(ownerUid),
                new GCMParameterSpec(
                        GCM_TAG_LENGTH_BITS,
                        Base64.decode(savedIv, Base64.NO_WRAP)
                )
        );

        return cipher;
    }

    public static byte[] unwrapFileKey(
            Context context,
            String ownerUid,
            Cipher authenticatedCipher
    ) throws Exception {

        SharedPreferences preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        String savedData = preferences.getString(
                KEY_WRAPPED_DATA_PREFIX + ownerUid,
                null
        );

        if (savedData == null) {
            throw new IllegalStateException(
                    "Không tìm thấy khóa đã bảo vệ."
            );
        }

        return authenticatedCipher.doFinal(
                Base64.decode(savedData, Base64.NO_WRAP)
        );
    }

    private static SecretKey getOrCreateKeystoreKey(
            String ownerUid
    ) throws Exception {

        String alias = KEY_ALIAS_PREFIX + ownerUid;

        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(alias)) {
            return (SecretKey) keyStore.getKey(alias, null);
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
        );

        KeyGenParameterSpec parameterSpec =
                new KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT
                                | KeyProperties.PURPOSE_DECRYPT
                )
                        .setKeySize(256)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(-1)
                        .setInvalidatedByBiometricEnrollment(true)
                        .build();

        keyGenerator.init(parameterSpec);

        return keyGenerator.generateKey();
    }
}