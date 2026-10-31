package com.apex.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Application entry point for the Apex Operations API. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ApexOperationsApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApexOperationsApiApplication.class, args);
  }
}
