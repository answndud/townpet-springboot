package com.townpet.media;

final class MediaContentSniffer {
  private MediaContentSniffer() {}

  static String detect(byte[] content) {
    if (startsWith(content, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) {
      return "image/jpeg";
    }
    if (startsWith(content, new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a})) {
      return "image/png";
    }
    if (startsWith(content, new byte[] {'G', 'I', 'F', '8'})) {
      return "image/gif";
    }
    if (startsWith(content, new byte[] {'%', 'P', 'D', 'F'})) {
      return "application/pdf";
    }
    return "application/octet-stream";
  }

  private static boolean startsWith(byte[] content, byte[] signature) {
    if (content.length < signature.length) return false;
    for (int index = 0; index < signature.length; index++) {
      if (content[index] != signature[index]) return false;
    }
    return true;
  }
}
