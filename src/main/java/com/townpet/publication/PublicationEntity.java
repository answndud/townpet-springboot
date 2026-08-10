package com.townpet.publication;

import com.townpet.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "publication")
class PublicationEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID authorMemberId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private PublicationType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PublicationScope scope;

  @Nullable private UUID neighborhoodId;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 20000)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PublicationLifecycle lifecycle;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected PublicationEntity() {}

  PublicationEntity(
      UUID authorMemberId,
      PublicationScope scope,
      @Nullable UUID neighborhoodId,
      String title,
      String body) {
    this.id = UuidV7.randomUuid();
    this.authorMemberId = authorMemberId;
    this.type = PublicationType.FREE_BOARD;
    this.scope = scope;
    this.neighborhoodId = neighborhoodId;
    this.title = title;
    this.body = body;
    this.lifecycle = PublicationLifecycle.ACTIVE;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  UUID getId() {
    return id;
  }

  UUID getAuthorMemberId() {
    return authorMemberId;
  }

  PublicationType getType() {
    return type;
  }

  PublicationScope getScope() {
    return scope;
  }

  @Nullable
  UUID getNeighborhoodId() {
    return neighborhoodId;
  }

  String getTitle() {
    return title;
  }

  String getBody() {
    return body;
  }

  PublicationLifecycle getLifecycle() {
    return lifecycle;
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

  void edit(
      PublicationScope scope,
      @Nullable UUID neighborhoodId,
      String title,
      String body,
      Instant changedAt) {
    this.scope = scope;
    this.neighborhoodId = neighborhoodId;
    this.title = title;
    this.body = body;
    this.updatedAt = changedAt;
  }

  void delete(Instant changedAt) {
    this.lifecycle = PublicationLifecycle.DELETED;
    this.updatedAt = changedAt;
  }
}
