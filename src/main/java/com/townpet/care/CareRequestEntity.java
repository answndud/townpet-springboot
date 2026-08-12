package com.townpet.care;

import com.townpet.common.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "care_request")
class CareRequestEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID requesterMemberId;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 5000)
  private String description;

  @Column(nullable = false, length = 200)
  private String location;

  @Column(nullable = false)
  private Instant startsAt;

  @Column(nullable = false)
  private Instant endsAt;

  @Nullable
  @Column(length = 200)
  private String rewardHint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CareRequestStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected CareRequestEntity() {}

  CareRequestEntity(
      UUID requester,
      String title,
      String description,
      String location,
      Instant startsAt,
      Instant endsAt,
      String rewardHint) {
    this.id = UuidV7.randomUuid();
    this.requesterMemberId = requester;
    this.title = title.trim();
    this.description = description.trim();
    this.location = location.trim();
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.rewardHint = rewardHint == null || rewardHint.isBlank() ? null : rewardHint.trim();
    this.status = CareRequestStatus.OPEN;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  void cancel() {
    if (status != CareRequestStatus.OPEN)
      throw new IllegalStateException("Care request is not open");
    status = CareRequestStatus.CANCELLED;
    updatedAt = Instant.now();
  }

  UUID getId() {
    return id;
  }

  UUID getRequesterMemberId() {
    return requesterMemberId;
  }

  String getTitle() {
    return title;
  }

  String getDescription() {
    return description;
  }

  String getLocation() {
    return location;
  }

  Instant getStartsAt() {
    return startsAt;
  }

  Instant getEndsAt() {
    return endsAt;
  }

  @Nullable
  String getRewardHint() {
    return rewardHint;
  }

  CareRequestStatus getStatus() {
    return status;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  long getVersion() {
    return version;
  }
}
