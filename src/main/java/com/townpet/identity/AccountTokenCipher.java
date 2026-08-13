package com.townpet.identity;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
final class AccountTokenCipher {
  private static final int IV_SIZE = 12;
  private final SecretKeySpec key;

  AccountTokenCipher(TownpetEmailProperties properties) {
    byte[] decoded = Base64.getDecoder().decode(properties.tokenEncryptionKey());
    if (decoded.length != 32) {
      throw new IllegalArgumentException(
          "townpet.email.token-encryption-key must be a base64 256-bit key");
    }
    key = new SecretKeySpec(decoded, "AES");
  }

  String encrypt(String rawToken) {
    try {
      byte[] iv = new byte[IV_SIZE];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      return Base64.getEncoder().encodeToString(iv)
          + "."
          + Base64.getEncoder()
              .encodeToString(cipher.doFinal(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Could not encrypt account token", exception);
    }
  }

  String decrypt(String encryptedToken) {
    try {
      String[] parts = encryptedToken.split("\\.", 2);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key,
          new GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])));
      return new String(
          cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | RuntimeException exception) {
      throw new IllegalArgumentException("Invalid encrypted account token", exception);
    }
  }
}
