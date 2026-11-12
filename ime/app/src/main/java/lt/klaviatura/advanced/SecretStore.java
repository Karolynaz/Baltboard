package lt.klaviatura.advanced;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecretStore {
  private static final String KEY_ALIAS = "gemini_api_key_aes";
  private static final String PREFS_NAME = "encrypted_credentials";
  private final SharedPreferences mPreferences;

  public SecretStore(@NonNull Context context) {
    this(context, "gemini");
  }

  public SecretStore(@NonNull Context context, @NonNull String provider) {
    mPreferences = context.getSharedPreferences(
        "gemini".equals(provider) ? PREFS_NAME : PREFS_NAME + "_" + provider, Context.MODE_PRIVATE);
  }

  public void save(@NonNull String value) throws Exception {
    final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
    final byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
    mPreferences
        .edit()
        .putString("value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
        .apply();
  }

  @Nullable
  public String get() {
    try {
      final String encrypted = mPreferences.getString("value", null);
      final String iv = mPreferences.getString("iv", null);
      if (encrypted == null || iv == null) return null;
      final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          getOrCreateKey(),
          new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
      return new String(
          cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), StandardCharsets.UTF_8);
    } catch (Exception ignored) {
      return null;
    }
  }

  public boolean exists() {
    // Treat incomplete or undecryptable state as absent so callers can safely re-enter a key.
    return get() != null;
  }

  public void remove() {
    mPreferences.edit().clear().apply();
  }

  @NonNull
  private static SecretKey getOrCreateKey() throws Exception {
    final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
    keyStore.load(null);
    final SecretKey existing = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    if (existing != null) return existing;

    final KeyGenerator generator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
    generator.init(
        new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build());
    return generator.generateKey();
  }
}
