package com.townpet.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

class BuildInfoContributorTest {
  @Test
  void exposesApplicationNameAndInjectedVersion() {
    Info.Builder builder = new Info.Builder();

    new BuildInfoContributor("abc123").contribute(builder);

    Map<?, ?> app = (Map<?, ?>) builder.build().getDetails().get("app");
    assertThat(app).isNotNull();
    Map<?, ?> nonNullApp = Objects.requireNonNull(app);
    assertThat(nonNullApp.get("name")).isEqualTo("townpet-springboot");
    assertThat(nonNullApp.get("version")).isEqualTo("abc123");
  }
}
