package com.townpet.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MaintenanceModeFilter extends OncePerRequestFilter {
  private final MaintenanceMode maintenanceMode;

  public MaintenanceModeFilter(MaintenanceMode maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!isWrite(request)) {
      filterChain.doFilter(request, response);
      return;
    }
    if (!maintenanceMode.tryEnterWrite()) {
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"title\":\"Service Unavailable\",\"detail\":\"TownPet is in maintenance mode\"}");
      return;
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      maintenanceMode.leaveWrite();
    }
  }

  private static boolean isWrite(HttpServletRequest request) {
    return switch (request.getMethod().toUpperCase(Locale.ROOT)) {
      case "POST", "PUT", "PATCH", "DELETE" -> true;
      default -> false;
    };
  }
}
