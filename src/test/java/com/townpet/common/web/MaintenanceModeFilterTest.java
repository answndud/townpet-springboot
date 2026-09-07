package com.townpet.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MaintenanceModeFilterTest {
  @TempDir Path tempDir;

  @Test
  void rejectsWritesWhileMarkerExistsAndAllowsReads() throws Exception {
    Path marker = tempDir.resolve("maintenance");
    MaintenanceMode mode = new MaintenanceMode(marker.toString());
    MaintenanceModeFilter filter = new MaintenanceModeFilter(mode);
    Files.createFile(marker);
    FilterChain chain = mock(FilterChain.class);

    MockHttpServletRequest write = new MockHttpServletRequest("POST", "/api/v1/publications");
    MockHttpServletResponse writeResponse = new MockHttpServletResponse();
    filter.doFilter(write, writeResponse, chain);

    assertThat(writeResponse.getStatus()).isEqualTo(503);
    assertThat(writeResponse.getContentAsString()).contains("maintenance mode");

    MockHttpServletRequest read = new MockHttpServletRequest("GET", "/api/v1/publications");
    MockHttpServletResponse readResponse = new MockHttpServletResponse();
    filter.doFilter(read, readResponse, chain);
    verify(chain).doFilter(read, readResponse);
  }

  @Test
  void drainsWriteCounterAfterRequestCompletes() throws Exception {
    MaintenanceMode mode = new MaintenanceMode(tempDir.resolve("maintenance").toString());
    MaintenanceModeFilter filter = new MaintenanceModeFilter(mode);
    FilterChain chain = (request, response) -> assertThat(mode.activeWrites()).isEqualTo(1);

    filter.doFilter(
        new MockHttpServletRequest("POST", "/api/v1/publications"),
        new MockHttpServletResponse(),
        chain);

    assertThat(mode.activeWrites()).isZero();
  }
}
