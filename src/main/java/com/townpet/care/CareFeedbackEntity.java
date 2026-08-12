package com.townpet.care;

import com.townpet.common.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "care_feedback")
class CareFeedbackEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID assignmentId;

  @Column(nullable = false)
  private UUID authorMemberId;

  @Column(nullable = false, length = 2000)
  private String body;

  @Column(nullable = false)
  private Instant createdAt;

  protected CareFeedbackEntity() {}

  CareFeedbackEntity(UUID assignment, UUID author, String body) {
    id = UuidV7.randomUuid();
    assignmentId = assignment;
    authorMemberId = author;
    this.body = body.trim();
    createdAt = Instant.now();
  }

  UUID getId() {
    return id;
  }

  UUID getAssignmentId() {
    return assignmentId;
  }

  UUID getAuthorMemberId() {
    return authorMemberId;
  }

  String getBody() {
    return body;
  }

  Instant getCreatedAt() {
    return createdAt;
  }
}
