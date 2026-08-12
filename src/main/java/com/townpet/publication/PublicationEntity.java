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

  @Nullable @Column private UUID authorMemberId;

  @Nullable private UUID guestAuthorId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private PublicationType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PublicationScope scope;

  @Nullable private UUID neighborhoodId;

  @Nullable
  @Column(length = 40)
  private String animalInterestCode;

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
    this(authorMemberId, scope, neighborhoodId, null, title, body);
  }

  PublicationEntity(
      UUID authorMemberId,
      PublicationScope scope,
      @Nullable UUID neighborhoodId,
      @Nullable String animalInterestCode,
      String title,
      String body) {
    this.id = UuidV7.randomUuid();
    this.authorMemberId = authorMemberId;
    this.type = PublicationType.FREE_BOARD;
    this.scope = scope;
    this.neighborhoodId = neighborhoodId;
    this.animalInterestCode = animalInterestCode;
    this.title = title;
    this.body = body;
    this.lifecycle = PublicationLifecycle.ACTIVE;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  static PublicationEntity forGuest(UUID guestAuthorId, String title, String body) {
    PublicationEntity publication = new PublicationEntity();
    publication.id = UuidV7.randomUuid();
    publication.guestAuthorId = guestAuthorId;
    publication.type = PublicationType.FREE_BOARD;
    publication.scope = PublicationScope.GLOBAL;
    publication.title = title.trim();
    publication.body = body.trim();
    publication.lifecycle = PublicationLifecycle.ACTIVE;
    publication.createdAt = Instant.now();
    publication.updatedAt = publication.createdAt;
    return publication;
  }

  UUID getId() {
    return id;
  }

  @Nullable
  UUID getAuthorMemberId() {
    return authorMemberId;
  }

  @Nullable
  UUID getGuestAuthorId() {
    return guestAuthorId;
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

  @Nullable
  String getAnimalInterestCode() {
    return animalInterestCode;
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
    edit(scope, neighborhoodId, null, title, body, changedAt);
  }

  void edit(
      PublicationScope scope,
      @Nullable UUID neighborhoodId,
      @Nullable String animalInterestCode,
      String title,
      String body,
      Instant changedAt) {
    this.scope = scope;
    this.neighborhoodId = neighborhoodId;
    this.animalInterestCode = animalInterestCode;
    this.title = title;
    this.body = body;
    this.updatedAt = changedAt;
  }

  void delete(Instant changedAt) {
    this.lifecycle = PublicationLifecycle.DELETED;
    this.updatedAt = changedAt;
  }

  void restore(Instant changedAt) {
    this.lifecycle = PublicationLifecycle.ACTIVE;
    this.updatedAt = changedAt;
  }

  void hide(Instant changedAt) {
    this.lifecycle = PublicationLifecycle.HIDDEN;
    this.updatedAt = changedAt;
  }

  void makeVisible(Instant changedAt) {
    this.lifecycle = PublicationLifecycle.ACTIVE;
    this.updatedAt = changedAt;
  }
}
