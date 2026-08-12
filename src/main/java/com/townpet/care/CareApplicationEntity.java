package com.townpet.care;

import com.townpet.common.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "care_application")
class CareApplicationEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID requestId;

  @Column(nullable = false)
  private UUID applicantMemberId;

  @Column(nullable = false, length = 2000)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CareApplicationStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected CareApplicationEntity() {}

  CareApplicationEntity(UUID requestId, UUID applicantMemberId, String message) {
    this.id = UuidV7.randomUuid();
    this.requestId = requestId;
    this.applicantMemberId = applicantMemberId;
    this.message = message.trim();
    this.status = CareApplicationStatus.PENDING;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  void changeStatus(CareApplicationStatus next) {
    if (status != CareApplicationStatus.PENDING)
      throw new IllegalStateException("Application is not pending");
    if (next != CareApplicationStatus.ACCEPTED && next != CareApplicationStatus.DECLINED) {
      throw new IllegalStateException("Invalid application transition");
    }
    status = next;
    updatedAt = Instant.now();
  }

  UUID getId() {
    return id;
  }

  UUID getRequestId() {
    return requestId;
  }

  UUID getApplicantMemberId() {
    return applicantMemberId;
  }

  String getMessage() {
    return message;
  }

  CareApplicationStatus getStatus() {
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
