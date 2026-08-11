package com.townpet.operations;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "acquisition_event")
class AcquisitionEventEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 80)
  private String eventName;

  @Column(nullable = false, length = 200)
  private String route;

  @Nullable
  @Column(length = 64)
  private String anonymousKeyHash;

  @Column(nullable = false)
  private Instant createdAt;

  protected AcquisitionEventEntity() {}

  AcquisitionEventEntity(
      UUID id, String eventName, String route, @Nullable String anonymousKeyHash) {
    this.id = id;
    this.eventName = eventName;
    this.route = route;
    this.anonymousKeyHash = anonymousKeyHash;
    this.createdAt = Instant.now();
  }
}
