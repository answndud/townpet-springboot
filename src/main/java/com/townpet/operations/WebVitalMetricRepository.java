package com.townpet.operations;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WebVitalMetricRepository extends JpaRepository<WebVitalMetricEntity, UUID> {
  List<WebVitalMetricEntity> findTop1000ByOrderByObservedAtDesc();
}
