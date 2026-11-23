package com.townpet.media;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/v1/media/uploads", "/api/upload", "/api/upload/client"})
class MediaController {
  private final MediaService media;
  private final MeterRegistry metrics;

  MediaController(MediaService media, MeterRegistry metrics) {
    this.media = media;
    this.metrics = metrics;
  }

  @PostMapping
  @MemberOnly
  MediaResponse create(
      @CurrentMember UUID currentMemberId,
      @Valid @RequestBody CreateUploadRequest request) {
    try {
      return toResponse(
          media.create(
              currentMemberId,
              request.checksumSha256(),
              request.contentType(),
              request.byteSize()));
    } catch (MediaQuotaExceededException exception) {
      metrics.counter("townpet.security.media_quota.rejections").increment();
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Media quota exceeded");
    }
  }

  @GetMapping("/{assetId}/url")
  @MemberOnly
  ReadUrlResponse readUrl(
      @CurrentMember UUID currentMemberId, @PathVariable UUID assetId) {
    return new ReadUrlResponse(media.readUrl(currentMemberId, assetId));
  }

  @PostMapping("/{assetId}/finalize")
  @MemberOnly
  MediaResponse finalizeUpload(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID assetId,
      @Valid @RequestBody FinalizeUploadRequest request) {
    return toResponse(
        media.finalizeUpload(currentMemberId, assetId, request.checksumSha256()));
  }

  @PutMapping(value = "/{assetId}/content", consumes = "multipart/form-data")
  @MemberOnly
  MediaResponse uploadContent(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID assetId,
      @RequestPart("file") MultipartFile file) {
    try {
      String contentType = file.getContentType();
      if (contentType == null || contentType.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Missing content type");
      }
      return toResponse(
          media.uploadContent(currentMemberId, assetId, contentType, file.getBytes()));
    } catch (java.io.IOException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
    }
  }

  @PostMapping("/{assetId}/attachments/publications/{publicationId}")
  @MemberOnly
  MediaResponse attachPublication(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID assetId,
      @PathVariable UUID publicationId) {
    return toResponse(media.attachToPublication(currentMemberId, assetId, publicationId));
  }

  private MediaResponse toResponse(UploadAssetEntity asset) {
    boolean uploadable = asset.getStatus() == MediaAssetStatus.UPLOADING;
    return new MediaResponse(
        asset.getId(),
        uploadable ? media.uploadUrl(asset) : "",
        uploadable ? media.uploadFields(asset) : Map.of(),
        asset.getObjectKey(),
        asset.getChecksumSha256(),
        asset.getContentType(),
        asset.getByteSize(),
        asset.getStatus(),
        asset.getPublicationId(),
        asset.getExpiresAt(),
        asset.getVersion());
  }

  record CreateUploadRequest(
      @NotBlank @Pattern(regexp = "^[a-fA-F0-9]{64}$") String checksumSha256,
      @NotBlank @Size(max = 120) String contentType,
      @NotNull @Min(1) Long byteSize) {}

  record FinalizeUploadRequest(
      @NotBlank @Pattern(regexp = "^[a-fA-F0-9]{64}$") String checksumSha256) {}

  record MediaResponse(
      UUID id,
      String uploadUrl,
      Map<String, String> uploadFields,
      String objectKey,
      String checksumSha256,
      String contentType,
      long byteSize,
      MediaAssetStatus status,
      @Nullable UUID publicationId,
      Instant expiresAt,
      long version) {}

  record ReadUrlResponse(String url) {}
}
