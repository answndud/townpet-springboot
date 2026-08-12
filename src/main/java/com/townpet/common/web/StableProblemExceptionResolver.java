package com.townpet.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.ObjectMapper;

/** Applies the same problem JSON to ResponseStatusException before MVC's default resolver runs. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class StableProblemExceptionResolver implements HandlerExceptionResolver {
  private final GlobalProblemHandler problems;
  private final ObjectMapper objectMapper;

  StableProblemExceptionResolver(GlobalProblemHandler problems, ObjectMapper objectMapper) {
    this.problems = problems;
    this.objectMapper = objectMapper;
  }

  @Override
  @Nullable
  public ModelAndView resolveException(
      HttpServletRequest request,
      HttpServletResponse response,
      @Nullable Object handler,
      Exception exception) {
    if (!(exception instanceof ResponseStatusException statusException)) return null;
    ProblemDetail problem = problems.handleStatus(statusException, request, response);
    response.setStatus(statusException.getStatusCode().value());
    response.setContentType("application/problem+json");
    try {
      objectMapper.writeValue(response.getWriter(), problem);
    } catch (IOException writeFailure) {
      return new ModelAndView();
    }
    return new ModelAndView();
  }
}
