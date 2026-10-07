package com.hoangquanghuy.papervault;
import javax.crypto.spec.SecretKeySpec;
import android.content.Context;

import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

public final class phiencuakho {

    private static SecretKey fileEncryptionKey;
    private static boolean filePickerOpen = false;
    private phiencuakho() {
    }

    public static void unlock(
            Context context,
            String ownerUid,
            String masterPassword
    ) throws GeneralSecurityException {

        fileEncryptionKey = quanlimatkhau.deriveFileEncryptionKey(
                context,
                ownerUid,
                masterPassword
        );
    }

    public static boolean isUnlocked() {
        return fileEncryptionKey != null;
    }

    public static SecretKey requireFileEncryptionKey() {
        if (fileEncryptionKey == null) {
            throw new IllegalStateException("Vault chưa được mở khóa.");
        }

        return fileEncryptionKey;
    }

    public static void lock() {
        fileEncryptionKey = null;
    }
    public static void restoreFileEncryptionKey(byte[] keyBytes) {
        fileEncryptionKey = new SecretKeySpec(keyBytes, "AES");
    }
    public static void setFilePickerOpen(boolean open) {
        filePickerOpen = open;
    }

    public static boolean isFilePickerOpen() {
        return filePickerOpen;
    }
}