package com.townpet.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
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
      HttpSecurity http, @Value("${townpet.e2e-support.enabled:false}") boolean e2eSupportEnabled)
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
                  .requestMatchers("/actuator/health", "/api/v1/auth/csrf", "/api/v1/auth/sessions")
                  .permitAll()
                  .requestMatchers("/api/health")
                  .permitAll()
                  .requestMatchers("/api/v1/auth/password-resets/**")
                  .permitAll()
                  .requestMatchers("/api/v1/auth/email-verifications/**")
                  .permitAll()
                  .requestMatchers("/api/v1/catalog/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/local-resources/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/gatherings/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/members/*")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/publications/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/posts/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, "/api/posts/*/view", "/api/posts/*/share")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/posts/*/stats")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/lost-found/alerts/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/lost-found/sightings/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/marketplace/listings/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/feed")
                  .permitAll()
                  .requestMatchers("/api/v1/operations/**")
                  .hasRole("MODERATOR")
                  .requestMatchers(HttpMethod.GET, "/api/v1/trust-reports/**")
                  .hasRole("MODERATOR")
                  .requestMatchers(HttpMethod.PATCH, "/api/v1/trust-reports/**")
                  .hasRole("MODERATOR")
                  .requestMatchers("/api/admin/reports/**")
                  .hasRole("MODERATOR")
                  .anyRequest()
                  .authenticated();
            })
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    return http.build();
  }
}
