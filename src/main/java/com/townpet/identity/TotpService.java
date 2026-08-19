package com.townpet.identity;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
final class TotpService {
  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final int CODE_DIGITS = 6;
  private static final long PERIOD_SECONDS = 30;
  private final SecureRandom random = new SecureRandom();

  String newSecret() {
    byte[] secret = new byte[20];
    random.nextBytes(secret);
    return encode(secret);
  }

  boolean matches(String secret, String code, Instant now) {
    if (!code.matches("\\d{" + CODE_DIGITS + "}")) return false;
    long counter = now.getEpochSecond() / PERIOD_SECONDS;
    for (long offset = -1; offset <= 1; offset++) {
      if (generate(secret, counter + offset).equals(code)) return true;
    }
    return false;
  }

  String generate(String secret, long counter) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
      byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
      int offset = hash[hash.length - 1] & 0x0f;
      int binary =
          ((hash[offset] & 0x7f) << 24)
              | ((hash[offset + 1] & 0xff) << 16)
              | ((hash[offset + 2] & 0xff) << 8)
              | (hash[offset + 3] & 0xff);
      return "%06d".formatted(binary % 1_000_000);
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalStateException("Could not generate MFA code", exception);
    }
  }

  String otpauthUri(String secret, UUID memberId) {
    return "otpauth://totp/TownPet:"
        + memberId
        + "?secret="
        + secret
        + "&issuer=TownPet&digits=6&period=30";
  }

  private static String encode(byte[] bytes) {
    StringBuilder result = new StringBuilder((bytes.length * 8 + 4) / 5);
    int buffer = 0;
    int bits = 0;
    for (byte value : bytes) {
      buffer = (buffer << 8) | (value & 0xff);
      bits += 8;
      while (bits >= 5) {
        bits -= 5;
        result.append(ALPHABET.charAt((buffer >>> bits) & 31));
      }
    }
    if (bits > 0) result.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
    return result.toString();
  }

  private static byte[] decode(String value) {
    String normalized = value.trim().replace("=", "").toUpperCase(java.util.Locale.ROOT);
    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
    int buffer = 0;
    int bits = 0;
    for (int index = 0; index < normalized.length(); index++) {
      char item = normalized.charAt(index);
      int alphabetIndex = ALPHABET.indexOf(item);
      if (alphabetIndex < 0) throw new IllegalArgumentException("Invalid TOTP secret");
      buffer = (buffer << 5) | alphabetIndex;
      bits += 5;
      if (bits >= 8) {
        bits -= 8;
        output.write((buffer >>> bits) & 0xff);
      }
    }
    return output.toByteArray();
  }
}
