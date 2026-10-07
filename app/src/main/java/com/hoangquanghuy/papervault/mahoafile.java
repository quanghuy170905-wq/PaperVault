package com.hoangquanghuy.papervault;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedOutputStream;
import javax.crypto.CipherOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.io.BufferedInputStream;
import javax.crypto.CipherInputStream;
import java.io.FileInputStream;
import java.util.Arrays;
import android.util.Base64;
public final class mahoafile {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final byte[] FILE_HEADER = {
            'P', 'V', '0', '1'
    };

    private mahoafile() {
    }

    public static String encryptFromUri(
            Context context,
            Uri sourceUri,
            String documentId,
            SecretKey encryptionKey
    ) throws Exception {

        File encryptedDirectory = new File(
                context.getFilesDir(),
                "encrypted_documents"
        );

        if (!encryptedDirectory.exists() && !encryptedDirectory.mkdirs()) {
            throw new IOException("Không thể tạo thư mục mã hóa.");
        }

        File temporaryFile = new File(
                encryptedDirectory,
                documentId + ".tmp"
        );

        File encryptedFile = new File(
                encryptedDirectory,
                documentId + ".pvault"
        );

        boolean success = false;

        try (InputStream inputStream =
                     context.getContentResolver().openInputStream(sourceUri)) {

            if (inputStream == null) {
                throw new IOException("Không thể đọc tệp đã chọn.");
            }

            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    encryptionKey,
                    parameterSpec
            );

            cipher.updateAAD(
                    documentId.getBytes(StandardCharsets.UTF_8)
            );

            try (FileOutputStream fileOutputStream =
                         new FileOutputStream(temporaryFile);

                 BufferedOutputStream bufferedOutputStream =
                         new BufferedOutputStream(fileOutputStream)) {

                bufferedOutputStream.write(FILE_HEADER);
                bufferedOutputStream.write(iv);

                try (CipherOutputStream cipherOutputStream =
                             new CipherOutputStream(
                                     bufferedOutputStream,
                                     cipher
                             )) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        cipherOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            }

            if (!temporaryFile.renameTo(encryptedFile)) {
                throw new IOException("Không thể hoàn tất lưu tệp mã hóa.");
            }

            success = true;

            return encryptedFile.getAbsolutePath();

        } finally {
            if (!success && temporaryFile.exists()) {
                temporaryFile.delete();
            }
        }
    }
    public static File decryptToCache(
            Context context,
            String encryptedPath,
            String documentId,
            SecretKey encryptionKey,
            String mimeType
    ) throws Exception {

        File encryptedFile = new File(encryptedPath);

        if (!encryptedFile.exists()) {
            throw new IOException("Không tìm thấy tệp mã hóa.");
        }

        String suffix = ".tmp";

        if ("application/pdf".equals(mimeType)) {
            suffix = ".pdf";
        }

        File decryptedFile = File.createTempFile(
                "paper_vault_",
                suffix,
                context.getCacheDir()
        );

        boolean success = false;

        try (BufferedInputStream inputStream =
                     new BufferedInputStream(
                             new FileInputStream(encryptedFile)
                     )) {

            byte[] header = new byte[FILE_HEADER.length];
            readFully(inputStream, header);

            if (!Arrays.equals(header, FILE_HEADER)) {
                throw new IOException("Tệp mã hóa không hợp lệ.");
            }

            byte[] iv = new byte[IV_LENGTH];
            readFully(inputStream, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    encryptionKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            cipher.updateAAD(
                    documentId.getBytes(StandardCharsets.UTF_8)
            );

            try (CipherInputStream cipherInputStream =
                         new CipherInputStream(inputStream, cipher);

                 FileOutputStream outputStream =
                         new FileOutputStream(decryptedFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            success = true;
            return decryptedFile;

        } finally {
            if (!success && decryptedFile.exists()) {
                decryptedFile.delete();
            }
        }
    }

    private static void readFully(
            InputStream inputStream,
            byte[] buffer
    ) throws IOException {

        int offset = 0;

        while (offset < buffer.length) {
            int bytesRead = inputStream.read(
                    buffer,
                    offset,
                    buffer.length - offset
            );

            if (bytesRead == -1) {
                throw new IOException("Tệp mã hóa bị thiếu dữ liệu.");
            }

            offset += bytesRead;
        }
    }
    public static String encryptText(
            String plainText,
            String documentId,
            SecretKey encryptionKey
    ) throws Exception {

        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        cipher.init(
                Cipher.ENCRYPT_MODE,
                encryptionKey,
                new GCMParameterSpec(TAG_LENGTH_BITS, iv)
        );

        cipher.updateAAD(
                documentId.getBytes(StandardCharsets.UTF_8)
        );

        byte[] encryptedBytes = cipher.doFinal(
                plainText.getBytes(StandardCharsets.UTF_8)
        );

        byte[] result = new byte[iv.length + encryptedBytes.length];

        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(
                encryptedBytes,
                0,
                result,
                iv.length,
                encryptedBytes.length
        );

        return Base64.encodeToString(result, Base64.NO_WRAP);
    }
}