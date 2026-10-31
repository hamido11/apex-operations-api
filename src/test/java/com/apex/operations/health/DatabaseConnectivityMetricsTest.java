package com.apex.operations.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies the {@code app_database_up} gauge reflects real DB reachability. */
@ExtendWith(MockitoExtension.class)
class DatabaseConnectivityMetricsTest {

  @Mock private DataSource dataSource;
  @Mock private Connection connection;

  @Test
  void reportsUp_whenConnectionIsValid() throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.isValid(2)).thenReturn(true);

    DatabaseConnectivityMetrics metrics = new DatabaseConnectivityMetrics(dataSource, registry);
    metrics.probe();

    assertThat(metrics.currentValue()).isEqualTo(1);
    assertThat(registry.get("app_database_up").gauge().value()).isEqualTo(1.0);
  }

  @Test
  void reportsDown_whenConnectionCannotBeOpened() throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    when(dataSource.getConnection()).thenThrow(new SQLException("no route to host: db"));

    DatabaseConnectivityMetrics metrics = new DatabaseConnectivityMetrics(dataSource, registry);
    metrics.probe();

    assertThat(metrics.currentValue()).isEqualTo(0);
    assertThat(registry.get("app_database_up").gauge().value()).isEqualTo(0.0);
  }
}
