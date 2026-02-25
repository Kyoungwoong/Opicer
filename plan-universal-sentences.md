# Plan: Universal Sentences Daily Set

## Context
- Problem statement: Universal sentences are a 4-item daily set (one per type) that changes once per day.
- References: Issue #38, Opicer.md

## Objective
- Provide a deterministic daily set of 4 universal sentences (one per type).
- Seed 4 types × 10 sentences via Admin API (not auto-seed).

## Scope
- In (MVP):
  - `/api/universal-sentences/daily` endpoint returning 4 items.
  - Deterministic daily selection per type.
  - Admin API seeding script (requires admin token).
  - Tests for daily selection logic + endpoint.
  - Frontend uses daily endpoint for home.
- Out (v2+):
  - Scheduling/cron-based rotation
  - Admin UI bulk upload

## API Contracts (Frozen for MVP)
- Endpoints:
  - `GET /api/universal-sentences/daily`
- Response DTOs:
  - `UniversalSentenceResponse` (existing)
- Validation rules:
  - If any type has no active sentence: error.
- Error responses:
  - `UNIVERSAL_SENTENCE_DAILY_NOT_READY` (HTTP 409)

## Requirements (Acceptance Criteria)
- [ ] Returns exactly 4 items (one per type).
- [ ] Same day returns same set.
- [ ] Next day returns a different set (if enough data).
- [ ] When any type is missing active items, returns error code.
- [ ] Home page calls daily endpoint.

## Scenarios (Fixed with Expected Outputs)
- S1: Given 10 active items per type, `GET /daily` returns 4 items.
  - Expected: one per type, deterministic for date.
- S2: Given missing type, `GET /daily` returns 409 + error code.

## Edge Cases (Deterministic Handling)
- E1: Empty list for any type → 409 error.
- E2: Only 1 per type → always same set per day.

## Design
- API resources:
  - `UniversalSentenceController#daily()`
- Data model:
  - `UniversalSentence` (existing)
- Transaction boundaries:
  - Read-only service for daily set
- Security rules:
  - `/api/universal-sentences/daily` requires auth (existing rule for `/api/**`)

## Tasks
- [ ] 1. Add daily selection logic (service + error code)
- [ ] 2. Add tests (service + controller)
- [ ] 3. Remove/disable auto-seeder
- [ ] 4. Add admin seeding script + data
- [ ] 5. Update frontend to call daily endpoint

## Verification (Definition of Done)
- Automated: `./gradlew test` (JaCoCo ≥ 80%)
- Manual: run seed script with admin token, verify 4 items per day
- Documentation: update `plan-universal-sentences.md` if changes
