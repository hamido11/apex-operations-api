package com.apex.operations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application-specific configuration.
 *
 * @param simulateFailure when {@code true}, the readiness probe reports DOWN. Used only to
 *     demonstrate a controlled, reversible release failure during the rollback exercise. Must
 *     remain {@code false} in normal operation.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(@DefaultValue("false") boolean simulateFailure) {}
