package com.apex.operations.web.dto;

import com.apex.operations.domain.Operation;
import com.apex.operations.domain.OperationStatus;
import java.time.Instant;

/** Response view of an {@link Operation}. */
public record OperationResponse(
    Long id,
    String name,
    OperationStatus status,
    String description,
    int priority,
    Instant createdAt,
    Instant updatedAt) {

  public static OperationResponse from(Operation op) {
    return new OperationResponse(
        op.getId(),
        op.getName(),
        op.getStatus(),
        op.getDescription(),
        op.getPriority(),
        op.getCreatedAt(),
        op.getUpdatedAt());
  }
}
