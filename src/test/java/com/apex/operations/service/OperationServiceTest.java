package com.apex.operations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apex.operations.domain.Operation;
import com.apex.operations.domain.OperationStatus;
import com.apex.operations.repository.OperationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test for {@link OperationService} with a mocked repository (no Spring context, no DB). */
@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

  @Mock private OperationRepository repository;

  @InjectMocks private OperationService service;

  @Test
  void create_persistsAndReturnsSavedEntity() {
    Operation saved = new Operation("Deploy", OperationStatus.PENDING, "desc", 2);
    when(repository.save(any(Operation.class))).thenReturn(saved);

    Operation result = service.create("Deploy", OperationStatus.PENDING, "desc", 2);

    assertThat(result).isSameAs(saved);
    verify(repository).save(any(Operation.class));
  }

  @Test
  void findById_whenMissing_throwsNotFound() {
    when(repository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(42L))
        .isInstanceOf(OperationNotFoundException.class)
        .hasMessageContaining("42");
  }

  @Test
  void findById_whenPresent_returnsEntity() {
    Operation op = new Operation("Scan", OperationStatus.IN_PROGRESS, "desc", 1);
    when(repository.findById(1L)).thenReturn(Optional.of(op));

    assertThat(service.findById(1L)).isSameAs(op);
  }
}
