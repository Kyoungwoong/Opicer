# Topic Practice Selection — TDD Script

## Scope
- Topics are fetched from backend DB
- User selects one topic and selection is stored

Non-goals:
- Generating practice prompts

## Domain Rules
1. Only active topics are returned.
2. Selecting an inactive topic is rejected.
3. Selection is stored with userId and timestamp.

## Test Strategy
- Integration tests:
  - `TopicControllerTest`
  - `TopicSelectionControllerTest`

## Test Cases (RED → GREEN)

### A. Topics API
**A1 (RED): returns active topics only**
- Given: active + inactive topics
- When: GET `/api/topics`
- Then: only active in response

### B. Selection API
**B1 (RED): stores selection**
- Given: active topic + authenticated user
- When: POST `/api/practice/topic-selections`
- Then: 201 with selection info

**B2 (RED): inactive topic rejected**
- Given: inactive topic
- When: POST selection
- Then: 409 error code

## How to Run
```bash
./gradlew test
```

## Coverage Map
- Rule 1 → A1
- Rule 2 → B2
- Rule 3 → B1
