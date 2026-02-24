# CLAUDE.md — Opicer Project Context for Claude Code

> This file is the entry point for Claude Code. Read it fully before taking any action.
> For the full development protocol, also read `AGENTS.md`.

---

## 1. Project Identity

**Opicer** is an AI-powered OPIC speaking coach.

- Simulates real OPIC exam flows (NL → AL level progression)
- Records and analyzes user speech via STT (OpenAI Whisper)
- Remembers user-specific speaking habits (RAG-based, no fine-tuning)
- Provides personalized feedback per OPIC level

**Key differentiator:** "The system remembers *how* you speak, not just what you said."

---

## 2. Source of Truth (Priority Order)

1. **`Opicer.md`** — Product spec and architecture. The highest authority.
2. **`AGENTS.md`** — Development protocol (VibeCoding). Non-negotiable.
3. **`plan.md`** — Current feature's scope, decisions, progress (may not be in git).
4. **Repository** — Code, tests, configs.
5. **Conversation** — Lowest priority. Never rely on it as sole memory.

When in doubt, re-read `Opicer.md` and `plan.md`.

---

## 3. Tech Stack

| Layer       | Technology                                      |
|-------------|-------------------------------------------------|
| Backend     | Kotlin + Spring Boot 3.x                        |
| Build       | Gradle (Kotlin DSL preferred)                   |
| Database    | PostgreSQL (production), H2 (test fallback)     |
| Migrations  | Flyway                                          |
| ORM         | Spring Data JPA + Hibernate                     |
| Auth        | Spring Security — HTTP Basic (MVP), OAuth2 later |
| Storage     | AWS S3 (prod) / MinIO (local dev)               |
| STT         | OpenAI Whisper API                              |
| Async       | DB Job table + scheduler (Kafka-extensible)     |
| Frontend    | Next.js + TypeScript (`opicer-web/`)            |
| Package Mgr | pnpm (frontend)                                 |

**Important:** Backend source is Kotlin (`src/main/kotlin`). Java source files are legacy — migrate on touch.

---

## 4. Repository Structure

```
Opicer/
├── CLAUDE.md              ← This file
├── AGENTS.md              ← Development protocol (read this)
├── Opicer.md              ← Product spec (source of truth)
├── opicer-api/            ← Spring Boot backend
│   └── src/main/kotlin/com/opicer/api/
├── opicer-web/            ← Next.js frontend
│   └── src/app/
└── .agents/skills/        ← Reference skill files (read when needed)
```

---

## 5. Core Domain Models

Entities use UUID PKs. All have `createdAt`, `updatedAt` via JPA Auditing.

| Entity             | Key Fields                                                            |
|--------------------|-----------------------------------------------------------------------|
| `Question`         | topic, type(enum), promptText, promptAudioUrl?, structuralHint?, targetLevels[], keyExpressions[], active |
| `DailySentence`    | date(unique), text, level(enum), audioUrl?, active                    |
| `PromptVersion`    | useCase(enum), version, name, template, active                        |
| `MockSession`      | Aggregate root for a test session                                     |
| `Recording`        | storageKey, status state machine (INITIATED→UPLOADED→TRANSCRIBING→DONE/FAILED) |
| `Transcript`       | text, segments, confidence, provider, model, cost                     |
| `Feedback`         | per-question analysis results                                         |
| `UserSpeechProfile`| quantitative signals + confidence-tagged pattern observations         |
| `CreditLedger`     | credit balance tracking                                               |

**Enums:**
- `OpicLevel`: NL, IL, IM, IH, AL
- `QuestionType`: DESCRIPTION, PAST_EXPERIENCE, OPINION, COMPARE_CONTRAST, ROLE_PLAY, UNEXPECTED_SITUATION
- `PromptUseCase`: TRANSCRIPT_CLEANING, FEEDBACK, SCRIPT_IMPROVEMENT

---

## 6. API Contracts (Current MVP)

### Public (no auth required)
```
GET  /api/questions                    # filter: topic, type, level, page, size, sort
GET  /api/questions/{id}
GET  /api/daily-sentences/today
GET  /api/health
```

### Admin (ROLE_ADMIN required)
```
POST/PUT/DELETE /api/admin/questions
GET             /api/admin/questions

POST/PUT/DELETE /api/admin/daily-sentences
GET             /api/admin/daily-sentences

POST/PUT/DELETE /api/admin/prompts
POST            /api/admin/prompts/{id}/activate   # ensures single active per useCase
GET             /api/admin/prompts
```

**Security invariants:**
- `/api/admin/**` → 401 without credentials, 403 with wrong role
- Admin credentials injected via env vars: `OPICER_ADMIN_USERNAME`, `OPICER_ADMIN_PASSWORD`
- NEVER claim official OPIC scores — always "estimated level"

---

## 7. Development Workflow

Follow the **VibeCoding 6-phase FSM** defined in `AGENTS.md`. No skipping phases.

```
Problem Interpretation → Plan → Build → Verify → Package → Read-Only
```

- **Each phase transition requires explicit user approval.**
- Keep `plan.md` updated as durable working memory.
- Build in small, reviewable increments. One logical unit per commit.
- If scope expands, stop and get `plan.md` update approval first.

**Architecture default:** Clean Layered (not Hexagonal) unless multiple adapters are needed.

---

## 8. Skills Reference

Read the relevant skill file **before** starting work in that area. Skills are located in `.agents/skills/`.

| Trigger                              | Read This File                                              |
|--------------------------------------|-------------------------------------------------------------|
| Any git action (branch, commit, PR)  | `.agents/skills/git-conventions/scripts/commit_message_guide.md` |
| Starting a new feature               | `.agents/skills/tdd-workflow/references/BEST_PRACTICES.md`  |
| Writing any test                     | `.agents/skills/tdd-loop/references/BEST_PRACTICES.md`      |
| Spring Boot controller/service tests | `.agents/skills/springboot-tdd/references/BEST_PRACTICES.md`|
| JPA entity / repository design       | `.agents/skills/jpa-patterns/SKILL.md`                      |
| Architecture decisions               | `.agents/skills/backend-patterns/references/BEST_PRACTICES.md` |
| Risk / timebox assessment            | `.agents/skills/assessment-checklists/SKILL.md`             |

**Skill precedence (highest → lowest):**
1. git-conventions
2. tdd-workflow → tdd-loop
3. assessment-checklists
4. springboot-tdd
5. java-coding-standards (applies to Kotlin code too)
6. jpa-patterns
7. backend-patterns

---

## 9. Git Conventions

**Branch naming:**
```
opicer-ai/<type>/<issue-number>-<short-description>
# e.g. opicer-ai/feat/5-question-bank-api
```

**Commit format:**
```
<type>(<scope>): <subject>
# e.g. feat(question): add public listing endpoint with filters
```

Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`

**Workflow:** Issue → Branch (from `main`) → Small commits → PR → Review → Merge

---

## 10. Code Standards (Always Apply)

### Kotlin / Java
- Business logic lives in **service/domain layer** — testable without HTTP
- Controllers handle HTTP only: parse request, call service, return response
- Use `@Transactional(readOnly = true)` for read paths
- No `EAGER` fetching on collections — use `JOIN FETCH` in queries
- Validate at system boundaries only (request DTOs); trust internal invariants

### Testing
- **Testcontainers + PostgreSQL** for integration tests (not H2 in integration layer)
- `@DataJpaTest` for repository tests
- `@WebMvcTest` for controller tests (mock service layer)
- Security tests must cover: 401 (no auth), 403 (wrong role), 200 (valid admin)
- JaCoCo coverage target: ≥ 80%

### Database
- Flyway for all schema changes — never `ddl-auto: create` in production profile
- Migrations: `src/main/resources/db/migration/V{n}__{description}.sql`
- `DailySentence.date` has a unique constraint
- Only one `PromptVersion` can be active per `useCase` — enforce in service + DB

### Error Responses
- Consistent shape across all endpoints: `{ "code": "...", "message": "..." }`
- 404 for missing resources, 409 for uniqueness conflicts, 400 for validation errors

---

## 11. Non-Negotiable Rules

1. **Never claim official OPIC scores.** All AI outputs are "estimates."
2. **Never fine-tune models on user data.** RAG + prompt engineering only.
3. **User audio requires explicit consent.** Users can delete all derived data (physical delete).
4. **Admin endpoints must always require ROLE_ADMIN** — never relax this.
5. **No scope expansion without plan.md update and user approval.**
6. **All business logic must be covered by deterministic tests** before marking done.
7. **Do not touch files outside the current feature's scope** without a separate plan.
8. **STT results are "estimated text," not ground truth** — store with confidence metadata.

---

## 12. Audio Pipeline (Phase 2+ — do not implement in MVP)

Presigned URL flow: `POST /api/recordings:init` → client uploads to S3 → `POST /api/recordings:complete` → async Job created → polling `GET /api/recordings/{id}`.

Recording state machine: `INITIATED → UPLOADING → UPLOADED → TRANSCRIBING → ANALYZING → DONE / FAILED / DELETED`

Async: DB Job table with `dedupeKey` (recordingId + jobType), idempotent processing. Kafka-extensible by design.

---

## 13. What Is Out of Scope (MVP)

- Mock Test, STT pipeline, TTS, LLM analysis
- pgvector / RAG / UserSpeechProfile / CreditLedger
- OAuth2 (structure prepared, not wired)
- Mobile native apps
- Official OPIC scoring claims
- Fine-tuned speech models
