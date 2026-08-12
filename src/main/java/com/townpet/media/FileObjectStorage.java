package com.townpet.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
class FileObjectStorage implements ObjectStoragePort {
  private final Path root;

  FileObjectStorage(@Value("${townpet.media.local-root:./.townpet/uploads}") String root) {
    this.root = Path.of(root).toAbsolutePath().normalize();
  }

  @Override
  public String createUploadUrl(
      String objectKey, String contentType, long byteSize, Instant expiresAt) {
    return "/api/v1/media/objects/" + objectKey;
  }

  @Override
  public Optional<StoredObject> inspect(String objectKey) {
    Path path = path(objectKey);
    if (!Files.isRegularFile(path)) return Optional.empty();
    try {
      byte[] content = Files.readAllBytes(path);
      String contentType = Files.readString(path.resolveSibling(path.getFileName() + ".type"));
      return Optional.of(
          new StoredObject(
              contentType, content.length, sha256(content), MediaContentSniffer.detect(content)));
    } catch (IOException exception) {
      return Optional.empty();
    }
  }

  @Override
  public void store(String objectKey, String contentType, byte[] content) {
    Path path = path(objectKey);
    try {
      Files.createDirectories(path.getParent());
      Files.write(path, content);
      Files.writeString(path.resolveSibling(path.getFileName() + ".type"), contentType);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not persist local media", exception);
    }
  }

  @Override
  public void delete(String objectKey) {
    Path path = path(objectKey);
    try {
      Files.deleteIfExists(path);
      Files.deleteIfExists(path.resolveSibling(path.getFileName() + ".type"));
    } catch (IOException exception) {
      throw new IllegalStateException("Could not delete local media", exception);
    }
  }

  private Path path(String objectKey) {
    Path resolved = root.resolve(objectKey).normalize();
    if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid object key");
    return resolved;
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }
}
