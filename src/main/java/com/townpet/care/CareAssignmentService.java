package com.townpet.care;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CareAssignmentService {
  private final CareRequestRepository requests;
  private final CareApplicationRepository applications;
  private final CareAssignmentRepository assignments;
  private final CareFeedbackRepository feedbacks;

  CareAssignmentService(
      CareRequestRepository r,
      CareApplicationRepository a,
      CareAssignmentRepository s,
      CareFeedbackRepository f) {
    requests = r;
    applications = a;
    assignments = s;
    feedbacks = f;
  }

  @Transactional
  CareAssignmentEntity accept(UUID requester, UUID requestId, UUID applicationId, long appVersion) {
    CareRequestEntity request =
        requests
            .findByIdAndStatus(requestId, CareRequestStatus.OPEN)
            .orElseThrow(NoSuchElementException::new);
    if (!request.getRequesterMemberId().equals(requester)) throw new SecurityException();
    if (assignments.findByRequestId(requestId).isPresent())
      throw new IllegalStateException("Assignment already exists");
    CareApplicationEntity application =
        applications
            .findByIdAndRequestId(applicationId, requestId)
            .orElseThrow(NoSuchElementException::new);
    if (application.getVersion() != appVersion) throw new IllegalStateException("version conflict");
    application.changeStatus(CareApplicationStatus.ACCEPTED);
    applications.save(application);
    for (CareApplicationEntity other :
        applications.findByRequestIdOrderByCreatedAtAscIdAsc(requestId)) {
      if (!other.getId().equals(application.getId())
          && other.getStatus() == CareApplicationStatus.PENDING)
        other.changeStatus(CareApplicationStatus.DECLINED);
    }
    CareAssignmentEntity assignment =
        assignments.save(new CareAssignmentEntity(requestId, application.getApplicantMemberId()));
    return assignments.saveAndFlush(assignment);
  }

  @Transactional
  CareAssignmentEntity transition(UUID actor, UUID id, CareAssignmentStatus status, long version) {
    CareAssignmentEntity assignment =
        assignments.findById(id).orElseThrow(NoSuchElementException::new);
    CareRequestEntity request =
        requests.findById(assignment.getRequestId()).orElseThrow(NoSuchElementException::new);
    if (!request.getRequesterMemberId().equals(actor)
        && !assignment.getCaregiverMemberId().equals(actor)) throw new SecurityException();
    if (assignment.getVersion() != version) throw new IllegalStateException("version conflict");
    if (status == CareAssignmentStatus.CANCELLED_BY_REQUESTER
        && !request.getRequesterMemberId().equals(actor)) throw new SecurityException();
    if (status == CareAssignmentStatus.CANCELLED_BY_CAREGIVER
        && !assignment.getCaregiverMemberId().equals(actor)) throw new SecurityException();
    assignment.transition(status);
    return assignments.saveAndFlush(assignment);
  }

  @Transactional
  CareFeedbackEntity feedback(UUID author, UUID assignmentId, String body) {
    CareAssignmentEntity a =
        assignments.findById(assignmentId).orElseThrow(NoSuchElementException::new);
    if (a.getStatus() != CareAssignmentStatus.COMPLETED)
      throw new IllegalStateException("Feedback requires completed assignment");
    CareRequestEntity r =
        requests.findById(a.getRequestId()).orElseThrow(NoSuchElementException::new);
    if (!r.getRequesterMemberId().equals(author) && !a.getCaregiverMemberId().equals(author))
      throw new SecurityException();
    if (feedbacks.existsByAssignmentIdAndAuthorMemberId(assignmentId, author))
      throw new IllegalStateException("Feedback already exists");
    return feedbacks.saveAndFlush(new CareFeedbackEntity(assignmentId, author, body));
  }
}
