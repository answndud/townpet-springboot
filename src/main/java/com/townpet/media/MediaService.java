package com.townpet.media;

import com.townpet.media.api.MediaOperations;
import com.townpet.publication.api.PublicationAccess;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MediaService implements MediaOperations {
  private final UploadAssetRepository assets;
  private final PublicationAccess publications;
  private final ObjectStoragePort storage;
  private final JdbcTemplate jdbc;

  MediaService(
      UploadAssetRepository assets,
      PublicationAccess publications,
      ObjectStoragePort storage,
      JdbcTemplate jdbc) {
    this.assets = assets;
    this.publications = publications;
    this.storage = storage;
    this.jdbc = jdbc;
  }

  @Transactional
  UploadAssetEntity create(
      UUID ownerMemberId, String checksumSha256, String contentType, long byteSize) {
    String normalizedType = contentType.trim().toLowerCase(java.util.Locale.ROOT);
    if (!java.util.Set.of("image/jpeg", "image/png", "image/gif", "application/pdf")
            .contains(normalizedType)
        || byteSize < 1
        || byteSize > 10 * 1024 * 1024) {
      throw new MediaInputNotAllowedException();
    }
    Instant now = Instant.now();
    return assets.save(
        new UploadAssetEntity(
            ownerMemberId,
            "uploads/" + ownerMemberId + "/" + UUID.randomUUID(),
            checksumSha256,
            normalizedType,
            byteSize,
            now.plus(1, ChronoUnit.HOURS)));
  }

  String uploadUrl(UploadAssetEntity asset) {
    return storage.createUploadUrl(
        asset.getObjectKey(), asset.getContentType(), asset.getByteSize(), asset.getExpiresAt());
  }

  @Transactional
  UploadAssetEntity uploadContent(
      UUID ownerMemberId, UUID assetId, String contentType, byte[] content) {
    UploadAssetEntity asset = ownedAsset(ownerMemberId, assetId);
    if (asset.getStatus() != MediaAssetStatus.UPLOADING
        || !asset.getExpiresAt().isAfter(Instant.now())) {
      throw new MediaAssetStateException();
    }
    if (!asset.getContentType().equalsIgnoreCase(contentType)
        || asset.getByteSize() != content.length) {
      throw new MediaObjectMismatchException();
    }
    if (content.length > 10 * 1024 * 1024) {
      throw new MediaInputNotAllowedException();
    }
    if (contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
      try {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
        if (image == null || image.getWidth() > 8000 || image.getHeight() > 8000) {
          throw new MediaObjectMismatchException();
        }
      } catch (java.io.IOException exception) {
        throw new MediaObjectMismatchException();
      }
    }
    storage.store(asset.getObjectKey(), contentType, content);
    StoredObject stored =
        storage.inspect(asset.getObjectKey()).orElseThrow(MediaObjectNotFoundException::new);
    if (!stored.checksumSha256().equalsIgnoreCase(asset.getChecksumSha256())
        || !stored.detectedContentType().equals(asset.getContentType())) {
      storage.delete(asset.getObjectKey());
      throw new MediaObjectMismatchException();
    }
    return asset;
  }

  @Transactional
  UploadAssetEntity finalizeUpload(UUID ownerMemberId, UUID assetId, String checksumSha256) {
    UploadAssetEntity asset = ownedAsset(ownerMemberId, assetId);
    if (asset.getStatus() != MediaAssetStatus.UPLOADING
        || !asset.getExpiresAt().isAfter(Instant.now())) {
      throw new MediaAssetStateException();
    }
    StoredObject object =
        storage.inspect(asset.getObjectKey()).orElseThrow(MediaObjectNotFoundException::new);
    if (!object.contentType().equals(asset.getContentType())
        || object.byteSize() != asset.getByteSize()
        || !object.checksumSha256().equalsIgnoreCase(asset.getChecksumSha256())
        || !object.checksumSha256().equalsIgnoreCase(checksumSha256)
        || !object.detectedContentType().equals(asset.getContentType())) {
      throw new MediaObjectMismatchException();
    }
    asset.finalizeUpload(checksumSha256, Instant.now());
    return assets.saveAndFlush(asset);
  }

  @Override
  @Transactional(readOnly = true)
  public CleanupReport inspectExpiredUploads(Instant now) {
    List<UploadAssetEntity> expired =
        assets.findTop500ByStatusAndExpiresAtBeforeOrderByExpiresAtAscIdAsc(
            MediaAssetStatus.UPLOADING, now);
    return new CleanupReport(
        expired.size(), expired.stream().mapToLong(UploadAssetEntity::getByteSize).sum(), 0, now);
  }

  @Override
  @Transactional
  public CleanupReport cleanupExpiredUploads(Instant now) {
    List<UploadAssetEntity> expired =
        assets.findTop500ByStatusAndExpiresAtBeforeOrderByExpiresAtAscIdAsc(
            MediaAssetStatus.UPLOADING, now);
    expired.forEach(asset -> storage.delete(asset.getObjectKey()));
    assets.deleteAll(expired);
    return new CleanupReport(
        expired.size(),
        expired.stream().mapToLong(UploadAssetEntity::getByteSize).sum(),
        expired.size(),
        now);
  }

  @Transactional
  UploadAssetEntity attachToPublication(UUID ownerMemberId, UUID assetId, UUID publicationId) {
    UploadAssetEntity asset = ownedAsset(ownerMemberId, assetId);
    UUID authorId =
        publications
            .activeAuthorMemberId(publicationId)
            .orElseThrow(MediaPublicationNotFoundException::new);
    if (!authorId.equals(ownerMemberId)) throw new MediaOwnershipException();
    jdbc.queryForObject(
        "SELECT id FROM publication WHERE id = ? FOR UPDATE", UUID.class, publicationId);
    Integer attachmentCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM upload_asset WHERE publication_id = ? AND status = 'ATTACHED'",
            Integer.class,
            publicationId);
    if (attachmentCount != null && attachmentCount >= 5) throw new MediaAttachmentLimitException();
    asset.attach(publicationId, Instant.now());
    return assets.saveAndFlush(asset);
  }

  private UploadAssetEntity ownedAsset(UUID ownerMemberId, UUID assetId) {
    return assets
        .findByIdAndOwnerMemberId(assetId, ownerMemberId)
        .orElseThrow(MediaAssetNotFoundException::new);
  }
}

final class MediaAssetNotFoundException extends RuntimeException {}

final class MediaInputNotAllowedException extends RuntimeException {}

final class MediaAttachmentLimitException extends RuntimeException {}

final class MediaAssetStateException extends RuntimeException {}

final class MediaObjectNotFoundException extends RuntimeException {}

final class MediaObjectMismatchException extends RuntimeException {}

final class MediaPublicationNotFoundException extends RuntimeException {}

final class MediaOwnershipException extends RuntimeException {}
