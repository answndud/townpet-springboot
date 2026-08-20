package com.townpet.common;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/** Exposes the immutable application version used to correlate an image with a running process. */
@Component
public final class BuildInfoContributor implements InfoContributor {
  private final String version;

  public BuildInfoContributor(@Value("${townpet.build.version:0.1.0-SNAPSHOT}") String version) {
    this.version = version;
  }

  @Override
  public void contribute(Info.Builder builder) {
    builder.withDetail("app", Map.of("name", "townpet-springboot", "version", version));
  }
}
