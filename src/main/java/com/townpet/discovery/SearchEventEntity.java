package com.townpet.discovery;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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

  protected SearchEventEntity() {}

  SearchEventEntity(UUID id, String queryHash, String route) {
    this.id = id;
    this.queryHash = queryHash;
    this.route = route;
    this.createdAt = Instant.now();
  }
}
