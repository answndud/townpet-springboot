package com.townpet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TownPetApplication {

  public static void main(String[] args) {
    SpringApplication.run(TownPetApplication.class, args);
  }
}
