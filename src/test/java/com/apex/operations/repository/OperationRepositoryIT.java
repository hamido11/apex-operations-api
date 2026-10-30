package com.apex.operations.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.apex.operations.domain.Operation;
import com.apex.operations.domain.OperationStatus;
import com.apex.operations.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Database persistence integration test against a real PostgreSQL (Testcontainers + Flyway). */
class OperationRepositoryIT extends AbstractIntegrationTest {

  @Autowired private OperationRepository repository;

  @Test
  void save_thenFindById_persistsAllFieldsAndTimestamps() {
    Operation toSave =
        new Operation("Persistence check", OperationStatus.IN_PROGRESS, "stored in postgres", 4);

    Operation saved = repository.save(toSave);
    assertThat(saved.getId()).isNotNull();

    Operation found = repository.findById(saved.getId()).orElseThrow();
    assertThat(found.getName()).isEqualTo("Persistence check");
    assertThat(found.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(found.getPriority()).isEqualTo(4);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
  void findAll_includesFlywaySeedData() {
    assertThat(repository.findAll()).extracting(Operation::getName).contains("Nightly backup");
  }
}
