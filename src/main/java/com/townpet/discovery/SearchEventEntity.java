package com.townpet.discovery;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "search_event")
class SearchEventEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 64)
  private String queryHash;

  @Column(nullable = false, length = 200)
  private String route;

  @Column(nullable = false)
  private Instant createdAt;

  @Nullable private UUID clientEventId;

  protected SearchEventEntity() {}

  SearchEventEntity(UUID id, String queryHash, String route, @Nullable UUID clientEventId) {
    this.id = id;
    this.queryHash = queryHash;
    this.route = route;
    this.createdAt = Instant.now();
    this.clientEventId = clientEventId;
  }

  @Nullable UUID getClientEventId() { return clientEventId; }
}
