# Plan: Topic Practice (Backend-driven)

## Context
- Problem statement: Topics must be served from backend DB and user selections saved server-side.
- References: Issue #42

## Objective
- Provide backend-managed topics and allow single-topic selection to start practice.

## Scope
- In (MVP):
  - Topic entity + admin CRUD
  - Public endpoint for active topics
  - Selection endpoint for user choice
  - Frontend fetch + selection submit
  - Seed script (admin API)
- Out (v2+):
  - Practice session content generation
  - Progress tracking dashboards

## API Contracts (Frozen for MVP)
- GET `/api/topics`
  - Response: list of active topics
- POST `/api/practice/topic-selections`
  - Request: `{ "topicId": "uuid" }`
  - Response: selection summary
- Admin: `/api/admin/topics` CRUD

## Requirements (Acceptance Criteria)
- [ ] Topics are retrieved from DB only
- [ ] User can select one topic and selection is stored
- [ ] Inactive topics are not returned
- [ ] Admin API can create/update/deactivate topics

## Scenarios
- S1: Fetch topics returns ordered active list
- S2: Selecting topic stores selection with userId
- S3: Inactive topic selection returns error

## Verification
- `./gradlew test`
- UI: topics load and selection submits successfully
