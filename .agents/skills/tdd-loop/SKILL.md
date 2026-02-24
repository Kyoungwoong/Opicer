---
name: tdd-loop
description: Generic TDD workflow (RED→GREEN→REFACTOR) with Java-first examples. Use this skill to drive incremental development regardless of framework.
version: 1.1
license: MIT
compatibility: Java 17+; JUnit 5; Gradle/Maven
metadata:
  category: workflow
  priority: high
  agent-safe: true
allowed-tools: filesystem shell
---

# Test-Driven Development Loop (Generic)

This skill defines **how to do TDD** independent of any specific stack.
Use `springboot-tdd` for Spring Boot–specific test patterns after this skill defines the loop.

## When to Activate

- Writing a new feature
- Fixing a bug
- Refactoring code (behavior preserved)
- Adding or changing an API contract

## Core Principles

### 1) Tests before code
- ALWAYS write a failing test first (RED).
- Implement the minimum to pass (GREEN).
- Refactor only while tests are green (REFACTOR).
- No production code without a failing test that requires it.

### 2) Small increments
- Each iteration should change one thing.
- Prefer the smallest test that proves the behavior.
- Keep test scope narrow; avoid multi-feature tests.

### 3) Tidy First
- Separate **structural** changes (rename/move/refactor) from **behavioral** changes (new behavior).
- Do not mix both in the same commit.

## The Loop

1) **RED**: Write a failing test with a clear failure message.
2) **GREEN**: Write the smallest code to pass.
3) **REFACTOR**: Improve design without changing behavior.
4) Repeat.

## Entry Checklist (Before Writing Tests)
- Requirement is clear and testable.
- Scope is small enough for 1-3 tests.
- Decide the first test name and expected failure.

## Test Types (Java-first)

### Unit tests (JUnit 5)
- Domain rules
- Pure computations
- Validation and edge cases

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DurationCalculatorTest {
  @Test
  void shouldComputeMinutesBetweenTimes() {
    long minutes = DurationCalculator.minutesBetween("09:00", "10:30");
    assertThat(minutes).isEqualTo(90);
  }
}
```

### Integration tests (API/service + persistence)
- API contracts
- DB interactions
- End-to-end service flows (without UI)

Example command:
```bash
./gradlew test
```

## Failure-First Rules

- If a test fails for the wrong reason (e.g., NPE, configuration), fix the test setup first.
- Avoid writing tests that require large scaffolding before proving a single behavior.

## Coverage Guidance (Pragmatic)

- Cover the main happy path and at least one edge case per rule.
- Prefer meaningful tests over chasing a percentage.
- Enforce coverage in CI (JaCoCo in Java) with **minimum 80% line coverage**.

## Commit Discipline (Recommended)

- `test(<scope>): add failing test for <behavior>`
- `feat(<scope>): implement <behavior>`
- `refactor(<scope>): tidy up <structure>` (tests stay green)

## Non-Goals

- This skill does not dictate frameworks (MockMvc/Testcontainers/etc.).
- This skill does not replace stack-specific conventions (use `springboot-tdd` for that).
