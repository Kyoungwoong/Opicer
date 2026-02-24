---
name: assessment-checklists
description: Time-limited coding test guardrails and checklists. Use this skill when working under strict deadlines (coding tests, take-home assessments, timed interviews) to enforce plan/TDD/dev loops, timeboxing, and final submission discipline.
allowed-tools: filesystem shell
---

# Assessment Checklists

## Core Rules (Must Follow)
- Always create or update `plan.md` from `PROBLEM.md` before writing tests or code.
- In TDD: write a failing test first, then minimal implementation, then refactor.
- Keep changes small and localized; one logical unit per iteration.
- Update `plan.md` whenever requirements or assumptions change.

## Timeboxing (Recommended Cadence)
- T-60: stabilize core requirements and algorithm correctness.
- T-30: harden edge cases, run full tests, remove debug noise.
- T-10: final commit and push, stop all changes after cutoff.

## Pre-Submit Checklist
- All MUST requirements in `plan.md` are satisfied.
- All tests pass.
- Edge cases reviewed.
- Final commit + push completed before deadline.

## Non-Goals
- This skill does not change business logic.
- This skill does not replace stack-specific testing rules.
