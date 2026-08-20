package com.townpet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:townpet;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR(320)",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:security-rate-limit-h2.sql",
      "spring.session.jdbc.initialize-schema=always",
      "spring.modulith.events.jdbc.schema-initialization.enabled=true"
    })
class TownPetApplicationTests {

  @Test
  void contextLoads() {}
}
