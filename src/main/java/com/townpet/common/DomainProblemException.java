package com.townpet.common;

import org.springframework.http.HttpStatus;

/**
 * Base class for business-rule failures that map to a fixed HTTP status and a stable error code.
 *
 * <p>Controllers do not catch these: {@code GlobalProblemHandler} turns them into RFC 9457
 * responses whose {@code code} identifies the failure (for example {@code COMMENT_NOT_FOUND}),
 * while the {@code detail} stays generic so no internal message reaches clients.
 */
public abstract class DomainProblemException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  protected DomainProblemException(HttpStatus status, String code) {
    super(code);
    this.status = status;
    this.code = code;
  }

  protected DomainProblemException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }
}
