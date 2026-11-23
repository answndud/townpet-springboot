package com.townpet.common;

import org.springframework.http.HttpStatus;

/** The cursor or page size of a list request is malformed or out of range. */
public final class InvalidPageRequestException extends DomainProblemException {
  public InvalidPageRequestException(String message) {
    super(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST", message);
  }
}
