package com.apex.operations.service;

/** Thrown when an operation cannot be found by id. Mapped to HTTP 404. */
public class OperationNotFoundException extends RuntimeException {

  public OperationNotFoundException(Long id) {
    super("Operation not found: " + id);
  }
}
