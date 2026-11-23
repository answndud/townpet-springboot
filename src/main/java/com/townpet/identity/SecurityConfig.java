package com.townpet.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  /** Reachable without signing in, for every HTTP method. */
  private static final String[] PUBLIC_ANY_METHOD = {
    "/actuator/health",
    "/actuator/health/**",
    "/actuator/info",
    "/api/health",
    "/api/viewer-shell",
    "/api/v1/auth/csrf",
    "/api/v1/auth/sessions",
    "/api/v1/auth/password-resets/**",
    "/api/v1/auth/email-verifications/**",
    "/api/search/log",
    "/api/security/csp-report",
    "/api/acquisition/events",
    "/api/guest/authors",
    "/api/guest/step-up",
    "/api/guest/step-up/consume",
    "/api/guest/posts/**",
    "/api/v1/catalog/**"
  };

  /** Public read-only pages and APIs. */
  private static final String[] PUBLIC_GET = {
    "/api/users/*/profile-summary",
    "/api/v1/adoptions",
    "/api/v1/adoptions/*",
    "/api/v1/hospital-reviews",
    "/api/v1/hospital-reviews/*",
    "/api/v1/volunteer",
    "/api/v1/volunteer/*",
    "/api/v1/local-resources/**",
    "/api/v1/gatherings/**",
    "/api/v1/care/requests/**",
    "/api/v1/publications/**",
    "/api/v1/lost-found/alerts/**",
    "/api/v1/lost-found/sightings/**",
    "/api/v1/marketplace/listings/**",
    "/api/v1/discovery",
    "/api/v1/discovery/popular",
    "/api/v1/communities/*/feed",
    "/api/v1/boards/*/feed",
    "/api/posts/**",
    "/api/boards/*/posts",
    "/api/lounges/breeds/*/groupbuys",
    "/api/lounges/breeds/*/posts",
    "/posts/*",
    "/adoptions/*",
    "/marketplace/*",
    "/lost-found/*",
    "/gatherings/*",
    "/care/*",
    "/guides/*"
  };

  /** Anonymous visitors may count views and shares and report web vitals. */
  private static final String[] PUBLIC_POST = {
    "/api/posts/*/view",
    "/api/posts/*/share",
    "/api/v1/operations/web-vitals",
    "/api/metrics/web-vitals"
  };

  /** Staff-only areas, for every HTTP method. */
  private static final String[] MODERATOR_ANY_METHOD = {
    "/actuator/metrics/**",
    "/api/v1/auth/mfa/**",
    "/api/v1/operations/**",
    "/api/admin/**",
    "/api/ops/web-vitals/summary",
    "/api/reports/bulk/**"
  };

  private static final String[] MODERATOR_GET = {"/api/v1/trust-reports/**", "/api/reports"};

  private static final String[] MODERATOR_PATCH = {"/api/v1/trust-reports/**", "/api/reports/**"};

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  DaoAuthenticationProvider authenticationProvider(MemberUserDetailsService users) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      @Value("${townpet.e2e-support.enabled:false}") boolean e2eSupportEnabled,
      StableSecurityProblemHandlers securityProblems,
      ModeratorMfaFilter moderatorMfaFilter)
      throws Exception {
    CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
    CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
    csrfHandler.setCsrfRequestAttributeName(null);

    http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokens).csrfTokenRequestHandler(csrfHandler))
        .authorizeHttpRequests(
            requests -> {
              if (e2eSupportEnabled) {
                requests.requestMatchers("/api/_test/**").permitAll();
              }
              // Rules are evaluated top to bottom and the first match wins, so the public POST
              // endpoints under /api/v1/operations must stay ahead of the moderator rule.
              requests
                  .requestMatchers(PUBLIC_ANY_METHOD)
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, PUBLIC_GET)
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, PUBLIC_POST)
                  .permitAll()
                  .requestMatchers(MODERATOR_ANY_METHOD)
                  .hasRole("MODERATOR")
                  .requestMatchers(HttpMethod.GET, MODERATOR_GET)
                  .hasRole("MODERATOR")
                  .requestMatchers(HttpMethod.PATCH, MODERATOR_PATCH)
                  .hasRole("MODERATOR")
                  .anyRequest()
                  .authenticated();
            })
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .headers(headers -> headers.frameOptions(frame -> frame.deny()))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(securityProblems)
                    .accessDeniedHandler(securityProblems))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    http.addFilterAfter(moderatorMfaFilter, AuthorizationFilter.class);
    return http.build();
  }
}
