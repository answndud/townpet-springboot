package com.townpet.welfare;

import com.townpet.common.DomainProblemException;
import org.springframework.http.HttpStatus;

/** Volunteer opportunity and hospital review failures that clients can act on. */
final class VolunteerNotFoundException extends DomainProblemException {
  VolunteerNotFoundException() {
    super(HttpStatus.NOT_FOUND, "VOLUNTEER_NOT_FOUND");
  }
}

/** The opportunity is closed, started, full, or the member already applied. */
final class VolunteerStateException extends DomainProblemException {
  VolunteerStateException(String message) {
    super(HttpStatus.CONFLICT, "VOLUNTEER_STATE_CONFLICT", message);
  }
}

final class HospitalReviewAlreadyExistsException extends DomainProblemException {
  HospitalReviewAlreadyExistsException() {
    super(HttpStatus.CONFLICT, "HOSPITAL_REVIEW_ALREADY_EXISTS");
  }
}
