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
              requests
                  .requestMatchers(
                      "/actuator/health",
                      "/actuator/info",
                      "/api/v1/auth/csrf",
                      "/api/v1/auth/sessions")
                  .permitAll()
                  .requestMatchers("/actuator/metrics/**")
                  .hasRole("MODERATOR")
                  .requestMatchers("/api/health")
                  .permitAll()
                  .requestMatchers("/api/viewer-shell")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/users/*/profile-summary")
                  .permitAll()
                  .requestMatchers("/api/search/log")
                  .permitAll()
                  .requestMatchers("/api/security/csp-report")
                  .permitAll()
                  .requestMatchers("/api/acquisition/events")
                  .permitAll()
                  .requestMatchers(
                      "/api/guest/authors",
                      "/api/guest/step-up",
                      "/api/guest/step-up/consume",
                      "/api/guest/posts/**")
                  .permitAll()
                  .requestMatchers("/api/v1/auth/password-resets/**")
                  .permitAll()
                  .requestMatchers("/api/v1/auth/email-verifications/**")
                  .permitAll()
                  .requestMatchers("/api/v1/auth/mfa/**")
                  .hasRole("MODERATOR")
                  .requestMatchers("/api/v1/catalog/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/local-resources/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/gatherings/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/care/requests/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/publications/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/posts/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, "/api/posts/*/view", "/api/posts/*/share")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/posts/*/stats")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/posts/*/lost-found-share.svg")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/lost-found/alerts/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/lost-found/sightings/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/marketplace/listings/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/boards/*/posts")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/lounges/breeds/*/groupbuys")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/lounges/breeds/*/posts")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/discovery")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/discovery/popular")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/communities/*/feed")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/boards/*/feed")
                  .permitAll()
                  .requestMatchers(
                      HttpMethod.POST, "/api/v1/operations/web-vitals", "/api/metrics/web-vitals")
                  .permitAll()
                  .requestMatchers("/api/v1/operations/**")
                  .hasRole("MODERATOR")
                  .requestMatchers(HttpMethod.GET, "/api/v1/trust-reports/**")
                  .hasRole("MODERATOR")
                  .requestMatchers(HttpMethod.PATCH, "/api/v1/trust-reports/**")
                  .hasRole("MODERATOR")
                  .requestMatchers("/api/admin/**")
                  .hasRole("MODERATOR")
                  .requestMatchers(
                      "/api/ops/web-vitals/summary", "/api/v1/operations/web-vitals/summary")
                  .hasRole("MODERATOR")
                  .requestMatchers(HttpMethod.GET, "/api/v1/trust-reports/**", "/api/reports")
                  .hasRole("MODERATOR")
                  .requestMatchers(HttpMethod.PATCH, "/api/v1/trust-reports/**", "/api/reports/**")
                  .hasRole("MODERATOR")
                  .requestMatchers("/api/reports/bulk/**")
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
