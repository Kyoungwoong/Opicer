---
name: tdd-workflow
description: Write a feature-specific TDD script (RED→GREEN plan) and keep it in docs/tdd/ before starting implementation.
version: 1.2
license: MIT
compatibility: Java 17+; Gradle projects; docs-driven workflow
metadata:
  category: workflow
  priority: high
  agent-safe: true
allowed-tools: filesystem shell
---

# TDD Script Skill

## Goal
Create a **feature-specific** TDD script that drives implementation through a clear RED → GREEN loop.

This skill produces a document under:
- `docs/tdd/<feature-slug>.md`

The script should be written **before** implementation work begins (test-first).

## When to use
- Any time the developer asks to "develop a feature" or "implement" something.
- Especially when the workflow requires: Plan → Issue → TDD → Implement → Test → Push.

## Output format (docs/tdd/<feature>.md)
Use this structure (copy/paste and fill in):

```markdown
# <Feature Name> — TDD Script

## Scope
- ...

Non-goals:
- ...

## Domain Rules (Acceptance Criteria)
1. ...

## Test Strategy
- Unit tests:
- Integration tests:

## Test Cases (RED → GREEN)

### A. <Area 1>
**A1 (RED): <test title>**
- Given:
- When:
- Then:

Implementation notes (GREEN):
- ...

### B. <Area 2>
...

## How to Run (Local)
```bash
./gradlew test
```

## Coverage Map (Requirements → Tests)
- <requirement> → <tests>

## Next Extensions (Future TDD Items)
- ...
```

## Rules
- Write tests first (RED) and make them fail for the correct reason.
- Only implement enough code to make the next test pass (GREEN).
- Keep tests small and explicit (happy path + at least one edge case per rule).
- Add a coverage map so missing requirements are visible.
- If a requirement is unclear, stop and clarify before writing tests.
- For protected endpoints, include 401/403/200 tests in the script.
- Enforce **JaCoCo 80% line coverage** in CI (tests must meet this threshold).

## Commit discipline (recommended)
- Prefer separate commits:
  - `test(<scope>): add failing tests for <feature>`
  - `feat(<scope>): implement <feature> to satisfy tests`
- Do not mix unrelated refactors into the GREEN commit.

## Notes
- Feature-specific TDD scripts live in `docs/tdd/` (project artifact).
- This skill documents *how* to write them (reusable process).
