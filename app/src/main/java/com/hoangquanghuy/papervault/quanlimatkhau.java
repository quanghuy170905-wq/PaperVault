package com.hoangquanghuy.papervault;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class quanlimatkhau {

    private static final String PREF_NAME = "vault_preferences";
    private static final String KEY_SALT = "master_password_salt";
    private static final String KEY_HASH = "master_password_hash";

    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 600_000;

    private static final String CRYPTO_PREFS = "paper_vault_crypto";
    private static final String KEY_FILE_SALT_PREFIX = "file_salt_";
    private static final int FILE_KEY_ITERATIONS = 600_000;
    private static final int FILE_KEY_LENGTH_BITS = 256;

    public static boolean isVaultCreated(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );

        return preferences.contains(KEY_SALT)
                && preferences.contains(KEY_HASH);
    }

    public static void createVault(Context context, String password) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] passwordHash = deriveKey(password.toCharArray(), salt);

        SharedPreferences preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );

        preferences.edit()
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_HASH, Base64.encodeToString(passwordHash, Base64.NO_WRAP))
                .apply();

        Arrays.fill(passwordHash, (byte) 0);
    }

    public static boolean verifyPassword(Context context, String password) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );

        String saltText = preferences.getString(KEY_SALT, null);
        String savedHashText = preferences.getString(KEY_HASH, null);

        if (saltText == null || savedHashText == null) {
            return false;
        }

        byte[] salt = Base64.decode(saltText, Base64.NO_WRAP);
        byte[] savedHash = Base64.decode(savedHashText, Base64.NO_WRAP);
        byte[] enteredHash = deriveKey(password.toCharArray(), salt);

        boolean isCorrect = MessageDigest.isEqual(savedHash, enteredHash);

        Arrays.fill(savedHash, (byte) 0);
        Arrays.fill(enteredHash, (byte) 0);

        return isCorrect;
    }

    private static byte[] deriveKey(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(
                password,
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
            );

            return factory.generateSecret(spec).getEncoded();

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Không thể xử lý Master Password",
                    e
            );

        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }
    public static SecretKey deriveFileEncryptionKey(
            Context context,
            String ownerUid,
            String masterPassword
    ) throws GeneralSecurityException {

        byte[] salt = getOrCreateFileSalt(context, ownerUid);

        PBEKeySpec keySpec = new PBEKeySpec(
                masterPassword.toCharArray(),
                salt,
                FILE_KEY_ITERATIONS,
                FILE_KEY_LENGTH_BITS
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] keyBytes = factory.generateSecret(keySpec).getEncoded();

            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            keySpec.clearPassword();
        }
    }

    private static byte[] getOrCreateFileSalt(
            Context context,
            String ownerUid
    ) {
        SharedPreferences preferences = context.getSharedPreferences(
                CRYPTO_PREFS,
                Context.MODE_PRIVATE
        );

        String preferenceKey = KEY_FILE_SALT_PREFIX + ownerUid;

        String savedSalt = preferences.getString(preferenceKey, null);

        if (savedSalt != null) {
            return Base64.decode(savedSalt, Base64.NO_WRAP);
        }

        byte[] newSalt = new byte[16];
        new SecureRandom().nextBytes(newSalt);

        preferences.edit()
                .putString(
                        preferenceKey,
                        Base64.encodeToString(newSalt, Base64.NO_WRAP)
                )
                .apply();

        return newSalt;
    }
}