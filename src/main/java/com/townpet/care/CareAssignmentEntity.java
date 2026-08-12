package com.townpet.care;

import com.townpet.common.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "care_assignment")
class CareAssignmentEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID requestId;

  @Column(nullable = false)
  private UUID caregiverMemberId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CareAssignmentStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected CareAssignmentEntity() {}

  CareAssignmentEntity(UUID requestId, UUID caregiver) {
    id = UuidV7.randomUuid();
    this.requestId = requestId;
    caregiverMemberId = caregiver;
    status = CareAssignmentStatus.MATCHED;
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  void transition(CareAssignmentStatus next) {
    boolean allowed =
        (status == CareAssignmentStatus.MATCHED
                && (next == CareAssignmentStatus.IN_PROGRESS
                    || next == CareAssignmentStatus.CANCELLED_BY_REQUESTER
                    || next == CareAssignmentStatus.CANCELLED_BY_CAREGIVER
                    || next == CareAssignmentStatus.ABORTED))
            || (status == CareAssignmentStatus.IN_PROGRESS
                && (next == CareAssignmentStatus.COMPLETED
                    || next == CareAssignmentStatus.CANCELLED_BY_REQUESTER
                    || next == CareAssignmentStatus.CANCELLED_BY_CAREGIVER
                    || next == CareAssignmentStatus.ABORTED));
    if (!allowed) throw new IllegalStateException("Invalid assignment transition");
    status = next;
    updatedAt = Instant.now();
  }

  UUID getId() {
    return id;
  }

  UUID getRequestId() {
    return requestId;
  }

  UUID getCaregiverMemberId() {
    return caregiverMemberId;
  }

  CareAssignmentStatus getStatus() {
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
