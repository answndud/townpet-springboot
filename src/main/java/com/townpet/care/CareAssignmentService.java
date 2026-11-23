package com.townpet.care;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CareAssignmentService {
  private final CareRequestRepository requests;
  private final CareApplicationRepository applications;
  private final CareAssignmentRepository assignments;
  private final Clock clock;
  private final CareFeedbackRepository feedbacks;

  CareAssignmentService(
      CareRequestRepository r,
      CareApplicationRepository a,
      CareAssignmentRepository s,
      CareFeedbackRepository f,
      Clock clock) {
    requests = r;
    applications = a;
    assignments = s;
    feedbacks = f;
    this.clock = clock;
  }

  @Transactional
  CareAssignmentEntity findForParticipant(UUID actor, UUID requestId) {
    CareAssignmentEntity assignment =
        assignments.findByRequestId(requestId).orElseThrow(NoSuchElementException::new);
    CareRequestEntity request =
        requests.findById(requestId).orElseThrow(NoSuchElementException::new);
    if (!request.getRequesterMemberId().equals(actor)
        && !assignment.getCaregiverMemberId().equals(actor)) throw new SecurityException();
    return assignment;
  }

  @Transactional
  CareAssignmentEntity accept(UUID requester, UUID requestId, UUID applicationId, long appVersion) {
    CareRequestEntity request =
        requests
            .findForUpdateByIdAndStatus(requestId, CareRequestStatus.OPEN)
            .orElseThrow(NoSuchElementException::new);
    if (!request.getRequesterMemberId().equals(requester)) throw new SecurityException();
    if (assignments.findByRequestId(requestId).isPresent())
      throw new IllegalStateException("Assignment already exists");
    CareApplicationEntity application =
        applications
            .findByIdAndRequestId(applicationId, requestId)
            .orElseThrow(NoSuchElementException::new);
    if (application.getVersion() != appVersion) throw new IllegalStateException("version conflict");
    application.changeStatus(CareApplicationStatus.ACCEPTED, Instant.now(clock));
    applications.save(application);
    applications.declineOtherPending(requestId, application.getId(), Instant.now(clock));
    CareAssignmentEntity assignment =
        assignments.save(new CareAssignmentEntity(
                requestId, application.getApplicantMemberId(), Instant.now(clock)));
    request.markMatched(Instant.now(clock));
    requests.save(request);
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
    assignment.transition(status, Instant.now(clock));
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
    return feedbacks.saveAndFlush(new CareFeedbackEntity(assignmentId, author, body, Instant.now(clock)));
  }
}
