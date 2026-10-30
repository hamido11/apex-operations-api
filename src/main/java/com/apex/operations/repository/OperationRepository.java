package com.apex.operations.repository;

import com.apex.operations.domain.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link Operation} records. */
public interface OperationRepository extends JpaRepository<Operation, Long> {}
