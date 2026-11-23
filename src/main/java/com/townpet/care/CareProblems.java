package com.townpet.care;

import com.townpet.common.DomainProblemException;
import org.springframework.http.HttpStatus;

/** Care request, application and assignment failures that clients can act on. */
final class CareNotFoundException extends DomainProblemException {
  CareNotFoundException() {
    super(HttpStatus.NOT_FOUND, "CARE_NOT_FOUND");
  }
}

final class CareAccessDeniedException extends DomainProblemException {
  CareAccessDeniedException() {
    super(HttpStatus.FORBIDDEN, "CARE_ACCESS_DENIED");
  }
}

/** The target is not in a state that allows the action (expired, closed, duplicate, ...). */
final class CareStateConflictException extends DomainProblemException {
  CareStateConflictException(String message) {
    super(HttpStatus.CONFLICT, "CARE_STATE_CONFLICT", message);
  }

  CareStateConflictException(String message, Throwable cause) {
    this(message);
    initCause(cause);
  }
}

/** The caller acted on a stale version of the request, application or assignment. */
final class CareVersionConflictException extends DomainProblemException {
  CareVersionConflictException() {
    super(HttpStatus.CONFLICT, "CARE_VERSION_CONFLICT");
  }
}

final class CareInvalidRequestException extends DomainProblemException {
  CareInvalidRequestException(String message) {
    super(HttpStatus.BAD_REQUEST, "CARE_INVALID_REQUEST", message);
  }
}
