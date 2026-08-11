package com.townpet.media;

import com.townpet.publication.api.PublicationAccess;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MediaService {
  private final UploadAssetRepository assets;
  private final PublicationAccess publications;
  private final ObjectStoragePort storage;

  MediaService(
      UploadAssetRepository assets, PublicationAccess publications, ObjectStoragePort storage) {
    this.assets = assets;
    this.publications = publications;
    this.storage = storage;
  }

  @Transactional
  UploadAssetEntity create(
      UUID ownerMemberId, String checksumSha256, String contentType, long byteSize) {
    Instant now = Instant.now();
    return assets.save(
        new UploadAssetEntity(
            ownerMemberId,
            "uploads/" + ownerMemberId + "/" + UUID.randomUUID(),
            checksumSha256,
            contentType.trim(),
            byteSize,
            now.plus(1, ChronoUnit.HOURS)));
  }

  String uploadUrl(UploadAssetEntity asset) {
    return storage.createUploadUrl(
        asset.getObjectKey(), asset.getContentType(), asset.getByteSize(), asset.getExpiresAt());
  }

  @Transactional
  UploadAssetEntity finalizeUpload(UUID ownerMemberId, UUID assetId, String checksumSha256) {
    UploadAssetEntity asset = ownedAsset(ownerMemberId, assetId);
    StoredObject object =
        storage.inspect(asset.getObjectKey()).orElseThrow(MediaObjectNotFoundException::new);
    if (!object.contentType().equals(asset.getContentType())
        || object.byteSize() != asset.getByteSize()
        || !object.checksumSha256().equalsIgnoreCase(asset.getChecksumSha256())
        || !object.checksumSha256().equalsIgnoreCase(checksumSha256)) {
      throw new MediaObjectMismatchException();
    }
    asset.finalizeUpload(checksumSha256, Instant.now());
    return assets.saveAndFlush(asset);
  }

  @Transactional
  UploadAssetEntity attachToPublication(UUID ownerMemberId, UUID assetId, UUID publicationId) {
    UploadAssetEntity asset = ownedAsset(ownerMemberId, assetId);
    UUID authorId =
        publications
            .activeAuthorMemberId(publicationId)
            .orElseThrow(MediaPublicationNotFoundException::new);
    if (!authorId.equals(ownerMemberId)) throw new MediaOwnershipException();
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

final class MediaAssetStateException extends RuntimeException {}

final class MediaObjectNotFoundException extends RuntimeException {}

final class MediaObjectMismatchException extends RuntimeException {}

final class MediaPublicationNotFoundException extends RuntimeException {}

final class MediaOwnershipException extends RuntimeException {}
