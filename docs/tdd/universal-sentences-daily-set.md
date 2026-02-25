# Universal Sentences Daily Set — TDD Script

## Scope
- Daily set endpoint that returns 4 items (one per type).
- Deterministic selection per day.
- Error when any type has no active items.

Non-goals:
- Admin UI bulk upload
- Scheduler-based rotation

## Domain Rules (Acceptance Criteria)
1. Daily set returns exactly 4 sentences (one per type).
2. Same day → same set; different day → different set when enough data.
3. Missing type → 409 error with code `UNIVERSAL_SENTENCE_DAILY_NOT_READY`.

## Test Strategy
- Unit tests:
  - `UniversalSentenceServiceTest` (daily selection logic)
- Integration tests:
  - `UniversalSentenceControllerTest` (daily endpoint + error)

## Test Cases (RED → GREEN)

### A. Service daily selection
**A1 (RED): returns 4 items (one per type) for given day**
- Given: active sentences across 4 types (10 each, ordered)
- When: request daily set for fixed date
- Then: returns 4 items, each with unique type

Implementation notes (GREEN):
- Use `Clock` for determinism
- Use type order `UniversalSentenceType.values()`

**A2 (RED): same day returns same set**
- Given: same data
- When: call twice for same date
- Then: identical item IDs per type

**A3 (RED): missing type throws ApiException**
- Given: no items for one type
- When: request daily set
- Then: ApiException with code `UNIVERSAL_SENTENCE_DAILY_NOT_READY`

### B. Controller endpoint
**B1 (RED): `/api/universal-sentences/daily` returns 4 items**
- Given: active sentences in DB
- When: GET with auth
- Then: HTTP 200 and 4 items

**B2 (RED): missing type returns 409**
- Given: missing type
- When: GET with auth
- Then: HTTP 409 and error code

## How to Run (Local)
```bash
./gradlew test
```

## Coverage Map (Requirements → Tests)
- Rule 1 → A1, B1
- Rule 2 → A2
- Rule 3 → A3, B2

## Next Extensions (Future TDD Items)
- Admin bulk upload endpoint
