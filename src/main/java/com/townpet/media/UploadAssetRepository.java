package com.townpet.media;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UploadAssetRepository extends JpaRepository<UploadAssetEntity, UUID> {
  Optional<UploadAssetEntity> findByIdAndOwnerMemberId(UUID id, UUID ownerMemberId);

  java.util.List<UploadAssetEntity> findByStatusAndExpiresAtBefore(
      MediaAssetStatus status, Instant expiresAt);
}
