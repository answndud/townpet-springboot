package com.townpet.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Blocks moderator operational surfaces until the current session completes MFA. */
@Component
final class ModeratorMfaFilter extends OncePerRequestFilter {
  private final StableSecurityProblemHandlers securityProblems;

  ModeratorMfaFilter(StableSecurityProblemHandlers securityProblems) {
    this.securityProblems = securityProblems;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var session = request.getSession(false);
    if (isModerator(authentication)
        && isProtectedSurface(request)
        && !Boolean.TRUE.equals(
            session == null ? null : session.getAttribute(MfaService.SESSION_VERIFIED_ATTRIBUTE))) {
      securityProblems.handle(request, response, new AccessDeniedException("MFA is required"));
      return;
    }
    filterChain.doFilter(request, response);
  }

  private static boolean isModerator(@Nullable Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority()));
  }

  private static boolean isProtectedSurface(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.startsWith("/api/v1/auth/mfa/")) return false;
    if (path.startsWith("/api/admin/") || "/api/admin".equals(path)) return true;
    if (path.startsWith("/api/reports/") || "/api/reports".equals(path)) return true;
    if (path.startsWith("/api/v1/trust-reports/")) return true;
    if (path.startsWith("/api/v1/operations/")) {
      return !("POST".equalsIgnoreCase(request.getMethod())
          && ("/api/v1/operations/web-vitals".equals(path)
              || "/api/metrics/web-vitals".equals(path)));
    }
    return "/api/ops/web-vitals/summary".equals(path);
  }
}
