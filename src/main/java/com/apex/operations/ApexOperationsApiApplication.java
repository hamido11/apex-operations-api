package com.apex.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Application entry point for the Apex Operations API. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApexOperationsApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApexOperationsApiApplication.class, args);
  }
}
