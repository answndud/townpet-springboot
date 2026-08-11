package com.townpet.operations;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "correction_request")
public class CorrectionRequestEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID memberId;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 2000)
  private String body;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private Instant createdAt;

  protected CorrectionRequestEntity() {}

  public CorrectionRequestEntity(UUID memberId, String title, String body) {
    this.id = UuidV7.randomUuid();
    this.memberId = memberId;
    this.title = title.trim();
    this.body = body.trim();
    this.status = "OPEN";
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getMemberId() {
    return memberId;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
