package com.townpet.media;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.townpet.publication.api.PublicationAccess;

class MediaServiceClockTest {
  private static final Instant NOW = Instant.parse("2026-04-01T08:00:00Z");
  private static final UUID OWNER = UUID.fromString("00000000-0000-4000-8000-000000000201");
  private static final UUID ASSET_ID = UUID.fromString("00000000-0000-4000-8000-000000000601");

  private final UploadAssetRepository assets = mock(UploadAssetRepository.class);
  private final MediaService service =
      new MediaService(
          assets,
          mock(PublicationAccess.class),
          mock(ObjectStoragePort.class),
          mock(JdbcTemplate.class),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void rejectsUploadsToAnAssetThatExpiresAtOrBeforeNow() {
    when(assets.findByIdAndOwnerMemberId(ASSET_ID, OWNER))
        .thenReturn(Optional.of(assetExpiringAt(NOW)));

    assertThatThrownBy(() -> service.uploadContent(OWNER, ASSET_ID, "image/jpeg", new byte[3]))
        .isInstanceOf(MediaAssetStateException.class);
  }

  @Test
  void acceptsUploadsUntilTheAssetExpires() {
    when(assets.findByIdAndOwnerMemberId(ASSET_ID, OWNER))
        .thenReturn(Optional.of(assetExpiringAt(NOW.plusSeconds(1))));

    // A different content type proves the request got past the expiry check.
    assertThatThrownBy(() -> service.uploadContent(OWNER, ASSET_ID, "image/png", new byte[3]))
        .isInstanceOf(MediaObjectMismatchException.class);
  }

  private static UploadAssetEntity assetExpiringAt(Instant expiresAt) {
    return new UploadAssetEntity(
        OWNER, "uploads/key", "a".repeat(64), "image/jpeg", 3, expiresAt, NOW.minusSeconds(60));
  }
}
