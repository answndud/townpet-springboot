package com.townpet.gathering;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gathering")
class GatheringEntity {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID hostMemberId;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 5000)
  private String description;

  @Column(nullable = false, length = 200)
  private String location;

  @Column(nullable = false)
  private Instant startsAt;

  @Column(nullable = false)
  private int capacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private GatheringStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Version private long version;

  protected GatheringEntity() {}

  GatheringEntity(
      UUID id,
      UUID hostMemberId,
      String title,
      String description,
      String location,
      Instant startsAt,
      int capacity) {
    this.id = id;
    this.hostMemberId = hostMemberId;
    this.title = title;
    this.description = description;
    this.location = location;
    this.startsAt = startsAt;
    this.capacity = capacity;
    this.status = GatheringStatus.ACTIVE;
    this.createdAt = Instant.now();
  }

  UUID getId() {
    return id;
  }

  UUID getHostMemberId() {
    return hostMemberId;
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

  int getCapacity() {
    return capacity;
  }

  GatheringStatus getStatus() {
    return status;
  }

  long getVersion() {
    return version;
  }

  void cancel() {
    status = GatheringStatus.CANCELLED;
  }
}
