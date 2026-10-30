package com.apex.operations;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INTENTIONAL failure used only to demonstrate the CI release gate.
 *
 * <p>This is NOT an application defect. It is tagged {@code failure-demo} and is EXCLUDED from
 * every normal build (see the surefire/failsafe {@code excludedGroups} config in pom.xml). It runs
 * only when the {@code failure-gate} Maven profile is active, which the {@code failure-gate} GitHub
 * Actions workflow triggers on demand to prove that a failed required check blocks the dependent
 * image-release job. The normal pipeline stays green.
 */
@Tag("failure-demo")
class FailureGateDemoTest {

  @Test
  void intentionalFailureToProveTheReleaseGate() {
    fail("Intentional failure-gate demonstration — expected to fail; not a real defect.");
  }
}
