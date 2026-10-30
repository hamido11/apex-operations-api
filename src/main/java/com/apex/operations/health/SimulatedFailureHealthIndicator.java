package com.apex.operations.health;

import com.apex.operations.config.AppProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator wired into the readiness group. Reports DOWN when {@code app.simulate-failure}
 * is enabled, which lets us demonstrate a genuine, reversible "bad release" and roll back from it
 * without introducing a real defect. Contributes key {@code simulatedFailure} to the health report.
 */
@Component("simulatedFailure")
public class SimulatedFailureHealthIndicator implements HealthIndicator {

  private final AppProperties properties;

  public SimulatedFailureHealthIndicator(AppProperties properties) {
    this.properties = properties;
  }

  @Override
  public Health health() {
    if (properties.simulateFailure()) {
      return Health.down()
          .withDetail("reason", "app.simulate-failure is enabled (controlled failure injection)")
          .build();
    }
    return Health.up().build();
  }
}
