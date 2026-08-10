package com.townpet.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts transport validation failures into the stable RFC 9457 contract. */
@RestControllerAdvice
public class GlobalProblemHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setTitle("Invalid request");
    problem.setProperty("code", "VALIDATION_FAILED");
    problem.setProperty("traceId", traceId(request));
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

  private static String traceId(HttpServletRequest request) {
    String supplied = request.getHeader("X-Trace-Id");
    return supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied;
  }

  private record FieldError(String field, String message) {}
}
