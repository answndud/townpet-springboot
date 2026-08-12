package com.townpet.care;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface CareFeedbackRepository extends JpaRepository<CareFeedbackEntity, UUID> {
  List<CareFeedbackEntity> findByAssignmentIdOrderByCreatedAtAsc(UUID assignmentId);

  boolean existsByAssignmentIdAndAuthorMemberId(UUID assignmentId, UUID authorMemberId);
}
