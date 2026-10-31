package com.apex.operations.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code app_database_up} (1 = a validated DB connection can be opened, 0 = not) so
 * monitoring can alert on the exact failure mode readiness guards against.
 *
 * <p>Readiness reports DB health to orchestrators, but Micrometer exposes no gauge for it — and the
 * {@code up} metric stays 1 during a DB outage because the app itself remains scrapable. This gauge
 * fills that gap. The probe runs on a schedule (never inline during a scrape) so a hung database
 * cannot stall metrics collection.
 */
@Component
public class DatabaseConnectivityMetrics {

  private static final Logger log = LoggerFactory.getLogger(DatabaseConnectivityMetrics.class);

  private final DataSource dataSource;
  private final AtomicInteger databaseUp = new AtomicInteger(0);

  public DatabaseConnectivityMetrics(DataSource dataSource, MeterRegistry registry) {
    this.dataSource = dataSource;
    Gauge.builder("app_database_up", databaseUp, AtomicInteger::doubleValue)
        .description("1 when the application can open a validated database connection, 0 otherwise")
        .register(registry);
  }

  @Scheduled(
      fixedRateString = "${app.db-probe.interval-ms:10000}",
      initialDelayString = "${app.db-probe.initial-delay-ms:5000}")
  void probe() {
    try (Connection connection = dataSource.getConnection()) {
      databaseUp.set(connection.isValid(2) ? 1 : 0);
    } catch (Exception ex) {
      databaseUp.set(0);
      log.debug("Database connectivity probe failed", ex);
    }
  }

  int currentValue() {
    return databaseUp.get();
  }
}
