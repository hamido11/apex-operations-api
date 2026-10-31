package com.apex.operations.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apex.operations.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** REST API integration test exercising the controller through the full Spring MVC stack. */
class OperationControllerIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void listOperations_returnsSeededData() throws Exception {
    mockMvc
        .perform(get("/api/v1/operations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)));
  }

  @Test
  void createThenGet_roundTrips() throws Exception {
    String body =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "name",
                "Smoke test run",
                "status",
                "PENDING",
                "description",
                "Created via API integration test",
                "priority",
                2));

    String location =
        mockMvc
            .perform(
                post("/api/v1/operations").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", notNullValue()))
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.name").value("Smoke test run"))
            .andExpect(jsonPath("$.priority").value(2))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mockMvc
        .perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void createWithInvalidPayload_returns400ProblemDetail() throws Exception {
    // Blank name and out-of-range priority should both fail bean validation.
    String invalid = "{\"name\":\"\",\"status\":\"PENDING\",\"priority\":0}";

    mockMvc
        .perform(
            post("/api/v1/operations").contentType(MediaType.APPLICATION_JSON).content(invalid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation error"))
        .andExpect(jsonPath("$.errors").isArray());
  }

  @Test
  void getMissingOperation_returns404() throws Exception {
    mockMvc
        .perform(get("/api/v1/operations/999999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Operation not found"));
  }

  @Test
  void getWithNonNumericId_returns400NotServerError() throws Exception {
    // A path-variable type mismatch must be a client error, not a 500.
    mockMvc.perform(get("/api/v1/operations/not-a-number")).andExpect(status().isBadRequest());
  }

  @Test
  void requestToUnknownPath_returns404NotServerError() throws Exception {
    mockMvc.perform(get("/api/v1/does-not-exist")).andExpect(status().isNotFound());
  }

  @Test
  void unsupportedMethod_returns405NotServerError() throws Exception {
    mockMvc.perform(delete("/api/v1/operations")).andExpect(status().isMethodNotAllowed());
  }

  @Test
  void unsupportedContentType_returns415NotServerError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/operations").contentType(MediaType.TEXT_PLAIN).content("not json at all"))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void malformedJsonBody_returns400NotServerError() throws Exception {
    mockMvc
        .perform(post("/api/v1/operations").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest());
  }
}
