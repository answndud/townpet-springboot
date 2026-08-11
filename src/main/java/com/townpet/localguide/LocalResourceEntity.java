package com.townpet.localguide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "local_resource")
class LocalResourceEntity {
  @Id private UUID id;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LocalResourceKind kind;
  @Column(nullable = false, length = 160) private String title;
  @Column(nullable = false, length = 500) private String summary;
  @Column(nullable = false, length = 10000) private String content;
  @Column(nullable = false, length = 120) private String sourceName;
  @Column(length = 500) private String sourceUrl;
  @Column(nullable = false) private Instant updatedAt;

  protected LocalResourceEntity() {}

  UUID getId() { return id; }
  LocalResourceKind getKind() { return kind; }
  String getTitle() { return title; }
  String getSummary() { return summary; }
  String getContent() { return content; }
  String getSourceName() { return sourceName; }
  String getSourceUrl() { return sourceUrl; }
  Instant getUpdatedAt() { return updatedAt; }
}
