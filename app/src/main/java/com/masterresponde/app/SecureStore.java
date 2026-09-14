package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecureStore {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "master_responde_secure_v1";
    private static final String PREFS = "master_responde_secure";

    private final SharedPreferences prefs;

    public SecureStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void put(String name, String value) throws Exception {
        SecretKey key = getOrCreateKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encrypted = cipher.doFinal(
                value.getBytes(StandardCharsets.UTF_8)
        );

        String iv = Base64.encodeToString(
                cipher.getIV(),
                Base64.NO_WRAP
        );

        String data = Base64.encodeToString(
                encrypted,
                Base64.NO_WRAP
        );

        prefs.edit()
                .putString(name + "_iv", iv)
                .putString(name + "_data", data)
                .apply();
    }

    public String get(String name) {
        try {
            String ivText = prefs.getString(name + "_iv", "");
            String dataText = prefs.getString(name + "_data", "");

            if (ivText.isEmpty() || dataText.isEmpty()) {
                return "";
            }

            SecretKey key = getOrCreateKey();

            byte[] iv = Base64.decode(
                    ivText,
                    Base64.NO_WRAP
            );

            byte[] data = Base64.decode(
                    dataText,
                    Base64.NO_WRAP
            );

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(128, iv)
            );

            byte[] clear = cipher.doFinal(data);

            return new String(
                    clear,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            return "";
        }
    }

    public void remove(String name) {
        prefs.edit()
                .remove(name + "_iv")
                .remove(name + "_data")
                .apply();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry =
                    (KeyStore.SecretKeyEntry) keyStore.getEntry(
                            KEY_ALIAS,
                            null
                    );

            return entry.getSecretKey();
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE
        );

        generator.init(
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT
                                | KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(
                                KeyProperties.BLOCK_MODE_GCM
                        )
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .setKeySize(256)
                        .build()
        );

        return generator.generateKey();
    }
}
