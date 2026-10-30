package com.apex.operations.web.dto;

import com.apex.operations.domain.OperationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request payload for creating an operation. Validated at the controller boundary. */
public record CreateOperationRequest(
    @NotBlank @Size(max = 120) String name,
    @NotNull OperationStatus status,
    @Size(max = 500) String description,
    @Min(1) @Max(5) int priority) {}
