package com.townpet.operations;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "web_vital_metric")
class WebVitalMetricEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 20)
  private String metricName;

  @Column(nullable = false)
  private double metricValue;

  @Column(nullable = false, length = 200)
  private String route;

  @Column(nullable = false)
  private Instant observedAt;

  protected WebVitalMetricEntity() {}

  WebVitalMetricEntity(UUID id, String metricName, double metricValue, String route) {
    this.id = id;
    this.metricName = metricName;
    this.metricValue = metricValue;
    this.route = route;
    this.observedAt = Instant.now();
  }

  String getMetricName() {
    return metricName;
  }

  double getMetricValue() {
    return metricValue;
  }

  String getRoute() {
    return route;
  }
}
