package com.townpet.trustsafety;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trust_report")
class TrustReportEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID reporterMemberId;

  @Column(nullable = false, length = 30)
  private String targetType;

  @Column(nullable = false)
  private UUID targetId;

  @Column(nullable = false, length = 40)
  private String reason;

  @Column(length = 1000)
  private String detail;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private Instant createdAt;

  protected TrustReportEntity() {}

  TrustReportEntity(
      UUID id, UUID reporter, String targetType, UUID targetId, String reason, String detail) {
    this.id = id;
    this.reporterMemberId = reporter;
    this.targetType = targetType;
    this.targetId = targetId;
    this.reason = reason;
    this.detail = detail;
    this.status = "OPEN";
    this.createdAt = Instant.now();
  }

  UUID getId() {
    return id;
  }

  UUID getReporterMemberId() {
    return reporterMemberId;
  }

  String getTargetType() {
    return targetType;
  }

  UUID getTargetId() {
    return targetId;
  }

  String getReason() {
    return reason;
  }

  String getDetail() {
    return detail;
  }

  String getStatus() {
    return status;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  void review(String next) {
    status = next;
  }
}
