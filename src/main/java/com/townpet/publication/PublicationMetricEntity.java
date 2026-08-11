package com.townpet.publication;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "publication_metric")
class PublicationMetricEntity {
  @Id private UUID publicationId;

  @Column(nullable = false)
  private long viewCount;

  protected PublicationMetricEntity() {}

  PublicationMetricEntity(UUID publicationId) {
    this.publicationId = publicationId;
  }

  long getViewCount() {
    return viewCount;
  }

  void increment() {
    viewCount++;
  }
}
