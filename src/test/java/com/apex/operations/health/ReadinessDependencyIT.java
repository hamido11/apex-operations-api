package com.apex.operations.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apex.operations.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Readiness/dependency test: when a component in the readiness group reports DOWN (here via the
 * controlled {@code app.simulate-failure} toggle), the readiness probe must return 503 DOWN while
 * liveness stays UP. This proves readiness is gated on dependency health — the same mechanism that
 * takes the instance out of rotation when PostgreSQL is unreachable.
 */
@TestPropertySource(properties = "app.simulate-failure=true")
class ReadinessDependencyIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void readiness_isDown_whenDependencyFails() throws Exception {
    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }

  @Test
  void liveness_staysUp_whenReadinessDown() throws Exception {
    mockMvc
        .perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
