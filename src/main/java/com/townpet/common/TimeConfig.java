package com.townpet.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The application clock. Time-dependent code takes a {@link Clock} so tests can fix the time. */
@Configuration
class TimeConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
