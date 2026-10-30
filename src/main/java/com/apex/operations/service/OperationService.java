package com.apex.operations.service;

import com.apex.operations.domain.Operation;
import com.apex.operations.domain.OperationStatus;
import com.apex.operations.repository.OperationRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service encapsulating operation use-cases. */
@Service
@Transactional(readOnly = true)
public class OperationService {

  private static final Logger log = LoggerFactory.getLogger(OperationService.class);

  private final OperationRepository repository;

  public OperationService(OperationRepository repository) {
    this.repository = repository;
  }

  public List<Operation> findAll() {
    return repository.findAll();
  }

  public Operation findById(Long id) {
    return repository.findById(id).orElseThrow(() -> new OperationNotFoundException(id));
  }

  @Transactional
  public Operation create(String name, OperationStatus status, String description, int priority) {
    Operation saved = repository.save(new Operation(name, status, description, priority));
    log.info("Created operation id={} name={} status={}", saved.getId(), saved.getName(), status);
    return saved;
  }
}
