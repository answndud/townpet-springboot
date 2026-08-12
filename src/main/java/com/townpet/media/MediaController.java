package com.townpet.media;

import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

  MediaController(MediaService media) {
    this.media = media;
  }

  @PostMapping
  @MemberOnly
  MediaResponse create(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody CreateUploadRequest request) {
    try {
      return toResponse(
          media.create(
              memberId(principal),
              request.checksumSha256(),
              request.contentType(),
              request.byteSize()));
    } catch (MediaInputNotAllowedException exception) {
      throw new ResponseStatusException(
          HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media metadata");
    }
  }

  @PostMapping("/{assetId}/finalize")
  @MemberOnly
  MediaResponse finalizeUpload(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID assetId,
      @Valid @RequestBody FinalizeUploadRequest request) {
    try {
      return toResponse(
          media.finalizeUpload(memberId(principal), assetId, request.checksumSha256()));
    } catch (MediaAssetNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (MediaObjectNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Uploaded object is missing");
    } catch (MediaObjectMismatchException exception) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Uploaded object does not match metadata");
    } catch (MediaAssetStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload is not finalizable");
    }
  }

  @PutMapping(value = "/{assetId}/content", consumes = "multipart/form-data")
  @MemberOnly
  MediaResponse uploadContent(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID assetId,
      @RequestPart("file") MultipartFile file) {
    try {
      String contentType = file.getContentType();
      if (contentType == null || contentType.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Missing content type");
      }
      return toResponse(
          media.uploadContent(memberId(principal), assetId, contentType, file.getBytes()));
    } catch (MediaAssetNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (MediaObjectMismatchException exception) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Uploaded object does not match metadata");
    } catch (MediaInputNotAllowedException exception) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Upload is too large");
    } catch (MediaAssetStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload is not active");
    } catch (java.io.IOException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
    }
  }

  @PostMapping("/{assetId}/attachments/publications/{publicationId}")
  @MemberOnly
  MediaResponse attachPublication(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID assetId,
      @PathVariable UUID publicationId) {
    try {
      return toResponse(media.attachToPublication(memberId(principal), assetId, publicationId));
    } catch (MediaAssetNotFoundException | MediaPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (MediaOwnershipException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } catch (MediaAttachmentLimitException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Publication attachment limit reached");
    } catch (MediaAssetStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload is not attachable");
    }
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private MediaResponse toResponse(UploadAssetEntity asset) {
    return new MediaResponse(
        asset.getId(),
        media.uploadUrl(asset),
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
      String objectKey,
      String checksumSha256,
      String contentType,
      long byteSize,
      MediaAssetStatus status,
      @Nullable UUID publicationId,
      Instant expiresAt,
      long version) {}
}
