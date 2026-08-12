package com.townpet.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converts transport validation failures into the stable RFC 9457 contract. */
@RestControllerAdvice
public class GlobalProblemHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalProblemHandler.class);
  private static final String TRACE_HEADER = "X-Trace-Id";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(
      MethodArgumentNotValidException exception,
      HttpServletRequest request,
      HttpServletResponse response) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setTitle("Invalid request");
    problem.setProperty("code", "VALIDATION_FAILED");
    problem.setProperty("traceId", traceId(request, response));
    List<FieldError> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new FieldError(
                        error.getField(),
                        Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")))
            .toList();
    problem.setProperty("fieldErrors", fieldErrors);
    return problem;
  }

  @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
  ProblemDetail handleConstraintViolation(
      Exception exception, HttpServletRequest request, HttpServletResponse response) {
    return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", request, response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ProblemDetail handleUploadTooLarge(
      MaxUploadSizeExceededException exception,
      HttpServletRequest request,
      HttpServletResponse response) {
    return problem(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", request, response);
  }

  @ExceptionHandler(ResponseStatusException.class)
  ProblemDetail handleStatus(
      ResponseStatusException exception, HttpServletRequest request, HttpServletResponse response) {
    return problem(exception.getStatusCode(), code(exception.getStatusCode()), request, response);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    HttpMessageNotReadableException.class,
    HttpMediaTypeNotSupportedException.class
  })
  ProblemDetail handleMalformedRequest(
      Exception exception, HttpServletRequest request, HttpServletResponse response) {
    return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", request, response);
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    ObjectOptimisticLockingFailureException.class
  })
  ProblemDetail handleConflict(
      Exception exception, HttpServletRequest request, HttpServletResponse response) {
    return problem(HttpStatus.CONFLICT, "STATE_CONFLICT", request, response);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ProblemDetail handleMissingResource(
      NoResourceFoundException exception,
      HttpServletRequest request,
      HttpServletResponse response) {
    return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", request, response);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(
      AccessDeniedException exception, HttpServletRequest request, HttpServletResponse response) {
    return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", request, response);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(
      Exception exception, HttpServletRequest request, HttpServletResponse response) {
    log.error(
        "Unhandled request failure traceId={} path={}",
        traceId(request, response),
        request.getRequestURI(),
        exception);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", request, response);
  }

  private static ProblemDetail problem(
      HttpStatusCode status,
      String code,
      HttpServletRequest request,
      HttpServletResponse response) {
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setTitle(title(status.value()));
    problem.setDetail(detail(status.value()));
    problem.setProperty("code", code);
    problem.setProperty("traceId", traceId(request, response));
    return problem;
  }

  private static String traceId(HttpServletRequest request, HttpServletResponse response) {
    String supplied = request.getHeader(TRACE_HEADER);
    if (supplied == null || !supplied.matches("[A-Za-z0-9._:-]{1,128}")) {
      supplied = response.getHeader(TRACE_HEADER);
    }
    String traceId =
        supplied != null && supplied.matches("[A-Za-z0-9._:-]{1,128}")
            ? supplied
            : UUID.randomUUID().toString();
    response.setHeader(TRACE_HEADER, traceId);
    return traceId;
  }

  private static String code(HttpStatusCode status) {
    return switch (status.value()) {
      case 400 -> "BAD_REQUEST";
      case 401 -> "UNAUTHENTICATED";
      case 403 -> "FORBIDDEN";
      case 404 -> "RESOURCE_NOT_FOUND";
      case 409 -> "STATE_CONFLICT";
      case 413 -> "PAYLOAD_TOO_LARGE";
      case 415 -> "UNSUPPORTED_MEDIA_TYPE";
      case 422 -> "UNPROCESSABLE_REQUEST";
      case 429 -> "RATE_LIMITED";
      case 503 -> "SERVICE_UNAVAILABLE";
      default -> status.is4xxClientError() ? "CLIENT_ERROR" : "INTERNAL_ERROR";
    };
  }

  private static String title(int status) {
    HttpStatus resolved = HttpStatus.resolve(status);
    return resolved == null ? "Request failed" : resolved.getReasonPhrase();
  }

  private static String detail(int status) {
    if (status >= 400 && status < 500) return "The request could not be processed.";
    return "The server could not complete the request.";
  }

  private record FieldError(String field, String message) {}
}
