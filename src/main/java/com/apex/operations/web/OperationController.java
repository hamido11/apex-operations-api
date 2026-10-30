package com.apex.operations.web;

import com.apex.operations.domain.Operation;
import com.apex.operations.service.OperationService;
import com.apex.operations.web.dto.CreateOperationRequest;
import com.apex.operations.web.dto.OperationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for operational records under {@code /api/v1/operations}. */
@RestController
@RequestMapping("/api/v1/operations")
public class OperationController {

  private final OperationService service;

  public OperationController(OperationService service) {
    this.service = service;
  }

  @GetMapping
  public List<OperationResponse> list() {
    return service.findAll().stream().map(OperationResponse::from).toList();
  }

  @GetMapping("/{id}")
  public OperationResponse get(@PathVariable Long id) {
    return OperationResponse.from(service.findById(id));
  }

  @PostMapping
  public ResponseEntity<OperationResponse> create(
      @Valid @RequestBody CreateOperationRequest request) {
    Operation created =
        service.create(request.name(), request.status(), request.description(), request.priority());
    return ResponseEntity.created(URI.create("/api/v1/operations/" + created.getId()))
        .body(OperationResponse.from(created));
  }
}
