# 🗣️ OPICER — OPIC Speaking AI Coach

> Vibe Coding–Ready Product & Architecture Spec (MVP)

## 0. Document Purpose

---
This document defines the product scope, system architecture, AI learning pipeline <br/>
and development contracts for building an OPIC-speaking practice service (Opicer, working title).

The document is intentionally designed to:

- Be directly consumable by AI coding agents
- Serve as a single source of truth during vibe coding
- Allow incremental expansion after MVP without refactoring core concepts

---
## 1. Product Vision

Opicer is an AI-powered OPIC speaking coach that:

- Simulates real OPIC exam flows
- Records and analyzes user speech
- Remembers user-specific speaking habits
- Improves answers based on OPIC level targets (NL → AL)
- Focuses on learning effectiveness, not rote memorization

### Key Differentiator
> “The system remembers how you speak, not just what you said.”

---
## 2. Role Definition

### Primary AI Role
You are an AI Pair Programmer & System Architect working with the product owner to build `Opicer`.

You act as:
- A Vibe Coding expert
- An **11-year full-stack engineer**
- An **OOP-** and **architecture**-oriented designer
- An AI system designer (speech, RAG, orchestration)

### Responsibilities
- Translate this document into production-ready code
- Respect architectural boundaries and contracts
- Prefer clarity over over-engineering
- Optimize for iteration speed and correctness

### Additional Role Constraints
- You must not hallucinate OPIC official scoring
- You must treat AI analysis as estimation, not authority
- You must optimize for explainability over black-box scoring

---
## 3. Target Users

- OPIC test takers (NL ~ AL)
- Korean native speakers learning English
- Users who need spoken feedback, not grammar drills

---
## 4. MVP Scope (Strict)

### Included (MVP)
- Home (daily sentence + quick practice)
- Question bank by OPIC type
- Mock test (survey → questions → recording)
- Speech transcription + basic analysis
- Script improvement by OPIC level
- User dashboard (basic)
- OAuth login (Google / Kakao)

### Explicitly Excluded (MVP)
- Official OPIC score guarantees
- Advanced pronunciation scoring (phoneme-level)
- Fine-tuned speech models
- Mobile native apps

---
## 5. User Features (Detailed)

### 5.1 Home
- Daily OPIC-style sentence
- Level tag (IL / IM / etc.)
- “Repeat & Record” CTA
- Quick entry to Mock Test

### 5.2 Question Bank
- Filter by:
    - Topic
    - Question type
    - Target OPIC level
- Question details:
    - Prompt (text + audio)
    - Structural hint (optional)
    - Key expressions (cards)

### 5.3 Mock Test Flow
1. Survey
2. Question delivery (Ava TTS)
3. Recording
4. Submission
5. Analysis & Report
6. Script Improvement

**Important UX Rules**
- Question replay limited
- Time pressure configurable
- No mid-answer editing

### 5.4 Analysis & Report

Per session:
- Estimated OPIC level (with uncertainty)
- Per-question feedback
- Highlighted issues:
    - Grammar
    - Structure
    - Expression variety
- Personalized improvement tips

### 5.5 My Page

- Credit balance
- Practice statistics
- Progress trend
- Historical reports

---

## 6. Admin Features (MVP Minimal)
- Question CRUD
- Daily sentence management
- Prompt version management
- Usage & cost monitoring

---

## 7. Restrictions
- The system must not claim official OPIC scoring.
- User audio data must be handled with explicit consent.
- Users must be able to delete recordings and transcripts.
- The system must avoid exam cheating or leaked content.
- Abuse prevention and rate limiting are mandatory.

---
## 8. OPIC Learning Model
### Question Types
1. Description
2. Past Experience
3. Opinion
4. Compare / Contrast
5. Role-play
6. Unexpected Situation

### Level Expectations
| Level |	Key Traits |
|-------|---------------|
| NL/IL |Simple sentences, basic tense|
| IM    |Logical flow, examples|
| IH    |Complex structure, coherence|
| AL	|Natural, persuasive, flexible|

---

## 9. AI Learning Architecture (CORE)
### 9.1 AI Orchestrator (Central Brain)
The AI Orchestrator coordinates all AI-related processing.

**Responsibilities**
- Audio ingestion
- Speech-to-text
- Text normalization
- Metric extraction
- Feedback generation
- User memory update

**Design Principle**
> The orchestrator is idempotent and asynchronous.

### 9.2 Speech → Learning Pipeline (CRITICAL)
**Step 1: Audio Ingestion**
- Format normalization (mono / 16kHz)
- Length validation
- Noise threshold check

**Step 2: Speech-to-Text (STT)**
- Sentence-level segmentation
- Timestamped transcript
- Confidence metadata (if available)

**Step 3: Text Cleaning & Annotation**
- Fillers tagging (um, uh, you know)
- Repetition detection
- Sentence boundary inference

**Step 4: Quantitative Signals**
- Words per minute
- Average sentence length
- Unique word ratio
- Estimated grammar error density
- Pause frequency

**Step 5: Qualitative Analysis (LLM)**
- Coherence
- Structure adherence
- Expression variety
- Level alignment

**Step 6: Script Improvement**
For selected OPIC level:
- Corrected version (minimal change)
- Expanded version (level-appropriate)
- 2–3 paraphrased variants

**Step 7: User Memory Update**
Persist:
- Frequent mistakes
- Repeated expressions
- Weak topics
- Historical level estimates

---

## 10. Personalization Strategy (NO Fine-tuning)
**We do NOT fine-tune speech models in MVP.**

Instead:
- Structured user speech profile
- Vector-based retrieval of past answers
- RAG-based feedback enrichment

This ensures:
- Lower cost
- Faster iteration
- Explainability

---

## 11. Data Architecture
### Tech Stack (Fixed)
- Frontend: React.js + TypeScript
- Backend: Kotlin + Spring Boot
- Database: PostgreSQL
- Vector DB: PostgreSQL (pgvector)
- Async: Queue-based processing
- (if you neccessary) kafka, redis, Spring Security, Spring Data JPA, Spring AI, etc
    - You can actively use it if you need it.
### Core Domain Models
- User
- SurveyResponse
- Question
- MockSession (Aggregate Root)
- Recording
- Transcript
- Feedback
- UserSpeechProfile
- CreditLedger

---
## 12. AI-Oriented Vector Usage

Stored as embeddings:
- User transcripts
- Feedback summaries
- Question intent

Used for:
- Similar answer retrieval
- Personalized feedback
- Weakness detection

## ## Summary

- Currently, repo is TimeWeave backend, and Opicer.md is OPIC Speaking AI
  It is a Coach product/architecture specification.
- This task is as selected by the user: (1) completely transition to Opicer, (2) large changes, (3)
  1차 산출물=Question Bank + Admin CRUD, (4) Kotlin + PostgreSQL + Spring
  Proceed to Security (role-based).
- However, for repo governance, PROBLEM.md remains unchanged (historical document).
  The de facto problem definition of this work is **User Instructions + Opicer.md **.

## Context

- Spec: Opicer.md
- Current codebase: src/main/java/com/example/demo/** (TimeWeave 도메인)
- Current build: build.gradle (Java 17, Spring Boot 4.0.2, H2)
- Governance: AGENTS.md , plan.md (Updated at the start of this operation because of existing work)
  Needed)

## Scope

- In:
    - Project Naming Organization: rootProject.name=opicer, base package
      com.opicer, spring.application.name=opicer
    - Reconfigure the backend based on Kotlin + Spring Boot
    - Transition to PostgreSQL + Migration Deployment (Flyway)
    - Question Bank (Public): Inquiry/Filter/Details
    - Admin (Protected): Question/Daily Sentence/Prompt Version CRUD(+활성
      Tue)
    - Default security: /api/admin/** access ADMIN only
- Out (excluding this primary output):
    - Mock Test, STT, TTS, LLM Analysis Pipeline
      (Orchestrator)
    - "Learning/personalizing" parts such as pgvector/RAG, UserSpeechProfile, CreditLedger, etc
    - OAuth (Google/Kakao) Real-world interworking (but security structure can be extended later to OAuth)
      Designed to be)

## Non-goals

- "Official OPIC Score" Claim/Guarantee Logic
- Voice data processing (including consent/deletion) does not even create API stubs in this range
  Um... (To the next sprint)

## Requirements (Acceptance Criteria)

- [ ] Remove the existing TimeWeave (timeblock/pattern) code/test/README-centric description
  and switch to Opicer backend.
- [ ] The schema is managed as Flyway based on PostgreSQL (at least one migration).
- [ ] You can filter/paging questions with the Public API.
- [ ] Create/modify/delete/inquire Question/DailySentence/PromptVersion with Admin API
  I can do it.
- [ ] /api/admin/** is 401 without authentication and 403 without permission.
- [ ] The test verifies the core CRUD + security based on Postgres (Testcontainers)
  The ./gradlew test goes through.

## Public APIs (v1)

### Question Bank

- GET /api/questions
    - Query: topic(optional), type(optional), level(optional), page, size,
      sort
    - Response: paged list (items[] + page metadata)
- GET /api/questions/{id}

### Home (Daily Sentence)

- GET /api/daily-sentences/today
- (옵션) GET /api/daily-sentences?

## Admin APIs (v1) — requires ROLE_ADMIN

- Questions
    - POST /api/admin/questions
    - PUT /api/admin/questions/{id}
    - DELETE /api/admin/questions/{id}
    - GET /api/admin/questions (including administrator full/inactive)
- Daily Sentences
    - POST /api/admin/daily-sentences
    - PUT /api/admin/daily-sentences/{id}
    - DELETE /api/admin/daily-sentences/{id}
    - GET /api/admin/daily-sentences
- Prompt Versions
    - POST /api/admin/prompts
    - PUT /api/admin/prompts/{id}
    - DELETE /api/admin/prompts/{id}
    - POST /api/admin/prompts/{id}/activate (동일 useCase 내 단일 active 보
      Chapter)
    - GET /api/admin/prompts

## Data Model (PostgreSQL, JPA)

### Enums

- OpicLevel: NL, IL, IM, IH, AL
- QuestionType: DESCRIPTION, PAST_EXPERIENCE, OPINION, COMPARE_CONTRAST,
  ROLE_PLAY, UNEXPECTED_SITUATION
- PromptUseCase: TRANSCRIPT_CLEANING, FEEDBACK, SCRIPT_IMPROVEMENT

### Entities (UUID PK)

- Question
    - id, topic(String), type(enum), promptText, promptAudioUrl?,
      structuralHint?
    - targetLevels(ElementCollection of OpicLevel)
    - keyExpressions(ElementCollection of String; ordered)
    - active(Boolean), createdAt, updatedAt
- DailySentence
    - id, date(unique), text, level(enum), audioUrl?, active, createdAt,
      updatedAt
- PromptVersion
    - id, useCase(enum), version(String), name(String), template(Text)
    - active(Boolean), createdAt, updatedAt

### Migrations (Flyway)

- V1: Above 3 tables + join tables for element-collection + unique
  constraints
- daily_sentence.date unique
- prompt_version: (use_case, active=true) service to maintain virtually only one
  Ensure at the level (+partial unique index is considered at the next stage)

## Security (Spring Security)

- /api/admin/**: HTTP Basic 기반 + ROLE_ADMIN 요구
- Public GET /api/**는 permit
- The Admin account is injected as an environment variable-based in-memory user in MVP:
    - OPICER_ADMIN_USERNAME, OPICER_ADMIN_PASSWORD
- Subsequent OAuth2 deployments maintain the same /api/admin/** privilege model and authenticate
  Configure only the method to be replaceable

## Implementation Tasks (ordered, decision-complete)

1. Repo Cleanup (Transition)
    - Remove existing timlock/**, related tests, TimeWeave description of README
    - rootProject.name, spring.application.name, base package를 Opicer로 정
      Lee
2. Kotlin Migration
    - Add Kotlin (Spring) Plug-in to Gradle
    - Start new code with src/main/kotlin, remove Java source (full transition)
3. PostgreSQL + Flyway 도입
    - runtime driver org.postgresql:postgresql
    - Configure Flyway dependencies and application.yml profiles (local, test)
    - Provide local postgres in docker-compose.yml (for development convenience)
4. Domain / Repository / Service Implementation
    - Question, DailySentence, PromptVersion + Repository + Service
    - Filter/Paging Inquiry Logic
    - Same useCase existing active inactivation (transaction)
5. Implementing API Layer
    - Public controllers: QuestionController, DailySentenceController
    - Admin controllers: AdminQuestionController,
      AdminDailySentenceController, AdminPromptController
    - DTO/Validation/Error Response Format Cleanup (whether to reuse existing exception response patterns)
      Clean up based on new packages)
6. 테스트 (Postgres Testcontainers)
    - Admin Security Test: 401/403/200
    - Question CRUD + Filter/Paging
    - DailySentence date unique 동작
    - Prompt activate behavior (keep single active)
7. Update a document
    - Update README.md based on Opicer backend (How to run: compose + app)
    - Update plan.md to work on this (the current plan is a different topic)

## Test Cases (must have)

- Admin endpoints:
    - no auth → 401
    - wrong creds → 401
    - admin creds → 200/201
- Question listing:
    - no filter
    - filter by type
    - filter by level (targetLevels 포함 여부)
    - pagination page/size
- Daily sentence:
    - Failed to create same date 2 (specified as 409 or validation error)
- Prompt activation:
    - Only one active remains after an activate call in the same useCase

## Definition of Done (Verification)

- Automated:
    - ./gradlew test
- Manual (local):
    - docker compose up -d (postgres)
    - After running the app:
        - GET /api/questions 200
        - POST /api/admin/questions (basic auth) 201
        - Confirm admin call 401 without authentication

## Assumptions & Defaults

- No audio files are saved, and promptAudioUrl is treated only as an "external URL string."
  All.
- The front end is out of this range, and we only provide the back end JSON API.
- OAuth remains a follow-up, but now confirms Admin protection with Basic + role.

## 0. 현재 합의된 방향

- 제품 전환: 기존 TimeWeave 백엔드 → **Opicer로 완전 전환**
- 우선 산출물:
    - **M0(운영/콘텐츠 기반)**: Question Bank + Admin CRUD + Prompt Version 관리 + 보안
    - 이후 **M2(핵심)**: 녹음 업로드 → STT → 분석/리포트(비동기 파이프라인)
- “편한 방법”이 아닌 “좋은 방법”:
    - 업로드: **Presigned URL**
    - 저장소: **S3** (로컬은 **MinIO**)
    - STT: **OpenAI Whisper API**
    - 비동기: **DB Job 테이블 + 스케줄러/워커**
    - 추후 확장: Kafka로 자연스럽게 이전 가능하도록 설계

  ---

## 1. 학생 데이터 품질/정확도 정책 (중요)

### 1.1 핵심 문제
- 학생(사용자) 음성/스크립트/전사 결과는 **부정확**할 수 있다.
- 부정확한 데이터를 “학습”에 사용하면, 시스템이 잘못된 패턴을 강화하거나(예: 틀린 표현을 자주 쓰는 것
  을 ‘개성’으로 저장), 피드백 품질을 떨어뜨릴 수 있다.

### 1.2 결론: “학습” 금지의 정의
MVP에서 다음은 **절대 하지 않는다**:
- 사용자 데이터를 이용한 **모델 파인튜닝 / 모델 업데이트**
- 사용자 데이터를 이용한 **전 사용자 공통 지식(글로벌 규칙) 생성**
  (예: “학생들이 많이 쓰니까 이 표현이 맞다” 같은 통계 기반 규칙화)

### 1.3 허용되는 개인화(단, 안전하게)
“학습” 대신 아래를 **개인화 메모리(사용자 전용)** 로만 저장할 수 있다.

- 저장 가능(추천):
    - **정량 지표**: WPM, 문장 길이, pause 빈도, filler 비율 등
    - **진단 결과(불확실성 포함)**: “시제 혼동 의심(중간 확신)”처럼 confidence 포함
    - **반복 패턴(단, 근거 링크 포함)**: “I think” 반복 빈도(원문 위치/세션ID 참조)

- 저장 시 안전장치(필수):
    - 모든 개인화 레코드는 `confidence`/`source`/`observedAt` 메타데이터를 가진다.
    - `confidence`가 낮은 항목은:
        - 프로필 반영을 **유예**하거나,
        - 반영하더라도 **가중치를 낮게** 두고 **자동 소멸(Decay)** 시킨다.
    - Retrieval로 과거 데이터를 가져올 때도 “정답”이 아니라 **참고 정보**로만 프롬프트에 포함한다.

### 1.4 전사(STT) 정확도 문제에 대한 운영 원칙
- Whisper 결과는 “정답 텍스트”가 아니라 **추정 텍스트**로 취급한다.
- STT 결과가 불확실할수록(낮은 confidence/잡음/길이/발음) 프로필 업데이트를 제한한다.
- 가능하면 다음을 고려(후속):
    - 사용자에게 transcript 확인/수정 UI 제공(수정본은 별도 필드로 저장, 원본과 분리)
    - “프로필 업데이트는 사용자 확인본만” 같은 옵션 제공

  ---

## 2. 오디오 업로드: Presigned URL 방식(권장)

### 2.1 전체 플로우(클라이언트 기준)
1) `POST /api/recordings:init`
    - 입력: `sessionId`, `questionId`, `contentType`, `expectedDurationSec?`
    - 출력: `recordingId`, `uploadUrl(presigned)`, `storageKey`
2) 클라이언트가 `uploadUrl`로 **S3/MinIO에 직접 PUT 업로드**
3) `POST /api/recordings:complete`
    - 입력: `recordingId`
    - 서버가 업로드 완료/메타 검증 후 **비동기 Job 생성**
4) `GET /api/recordings/{recordingId}` 또는 `GET /api/sessions/{id}`
    - 상태 조회: `UPLOADED → TRANSCRIBING → ANALYZING → DONE/FAILED`
    - 완료 시 transcript/리포트 조회 가능

### 2.2 오디오 표준 포맷(서버 내부 기준)
- 업로드 포맷은 다양해도 됨(webm/opus 등)
- 파이프라인 내부 기준은:
    - **WAV(PCM), mono, 16kHz, 16-bit** 로 정규화 권장
- 길이/용량 제한은 정책으로 고정 필요(예: 질문당 최대 120초, 25MB)

### 2.3 저장소 구성
- Production: AWS S3
- Local Dev: MinIO (S3-compatible)
- 장점: presign/업로드/권한 모델이 개발/운영에서 동일해짐

  ---

## 3. STT: Whisper API 사용

### 3.1 처리 방식
- 워커가 녹음 오브젝트를 가져와(또는 presigned GET) Whisper API 호출
- 결과물 저장:
    - `Transcript`(텍스트, 세그먼트/타임스탬프 가능하면 포함)
    - `confidence`/`provider`/`model`/`cost` 등의 메타

### 3.2 실패/재시도
- 네트워크/레이트리밋/일시 장애: 제한된 횟수 재시도
- 포맷 오류/길이 초과: 즉시 실패 처리(재시도 불가)

  ---

## 4. 비동기 오케스트레이션: DB Job 방식 (현재) + Kafka 확장 (추후)

### 4.1 DB Job 방식(현재 MVP에 최적)
#### 이유
- 인프라 최소화(외부 큐 불필요)
- 아이템포턴시/재시도/관찰성(메트릭) 설계가 단단함
- 작은 팀/초기 MVP에 운영 부담이 낮음

#### 권장 테이블(예시)
- `recording`
    - `id`, `storageKey`, `status`, `createdAt`, `updatedAt`
- `job`
    - `id`, `type`(TRANSCRIBE/ANALYZE), `dedupeKey`(recordingId+type)
    - `status`(READY/RUNNING/SUCCEEDED/FAILED)
    - `attempts`, `nextRunAt`, `lockedAt`, `lockedBy`
    - `lastError`, `createdAt`, `updatedAt`

#### 스케줄러/워커 동작
- 일정 주기로 `READY && nextRunAt<=now` Job을 가져와 lock 후 실행
- 성공/실패/재시도 정책을 상태로 반영
- 같은 `dedupeKey`는 중복 생성되지 않게(unique) 하여 **아이템포턴시** 확보

### 4.2 Kafka로 확장하기 위한 설계 가이드(문서화 요구 반영)

#### 목표
- 처리량/확장성 향상
- 워커를 독립적으로 수평 확장
- 이벤트 기반 파이프라인(RecordingUploaded → TranscribeRequested → …)

#### 확장 전략: “DB Job → Kafka” 무리 없이 가는 방법
1) **현재부터 이벤트 모델을 고정**
    - 내부적으로 “이벤트 타입/페이로드 스키마”를 정의해둔다.
    - DB Job은 사실상 “이벤트 큐” 역할을 한다고 생각한다.
2) 추후 Kafka 도입 시, 다음 중 하나로 전환:
    - (A) Job 생성 시 Kafka publish (이중화/점진적)
    - (B) Outbox 패턴: DB 트랜잭션 내 outbox 기록 → Kafka relay가 publish
3) 컨슈머는 `recordingId` 기준으로 **중복처리 방지(dedupe)** 를 반드시 수행
    - exactly-once를 믿지 않고, at-least-once에 맞춘 idempotent 처리

#### Kafka 토픽 예시
- `opicer.recording.events`
    - `RecordingUploadCompleted`
- `opicer.ai.jobs`
    - `TranscribeRequested`
    - `AnalyzeRequested`
- `opicer.ai.results`
    - `TranscriptCreated`
    - `ReportCreated`
- `opicer.ai.dlq`
    - 처리 불가/영구 실패 이벤트

#### 메시지 키/아이템포턴시
- 메시지 key: `recordingId` (같은 녹음은 같은 파티션으로 가게)
- 컨슈머는 처리 완료 테이블/캐시로 `recordingId+type` 처리 여부를 기록

#### 재시도/실패 정책
- 재시도는:
    - (1) Kafka retry topic(지연) 또는
    - (2) 컨슈머 내부 backoff + 재발행
- 영구 실패는 DLQ로 보내고 운영자가 재처리 가능하게 한다.

  ---

## 5. 데이터 보호/삭제(학습 정책과 연결)

- 사용자 동의 없이는 오디오를 수집/처리하지 않는다.
- 사용자는 언제든 “삭제”할 수 있어야 한다:
    - 원본 오디오 오브젝트
    - 정규화된 오디오
    - transcript
    - 파생 결과(리포트/임베딩/프로필 업데이트 항목)
- 삭제는 “표시 삭제”가 아니라 가능한 범위에서 **물리 삭제**를 기본으로 한다(정책으로 명시).

  ---

## 6. 다음 계획에 반영해야 할 체크리스트(Decision items)
- 업로드 제한: `maxDurationSec`, `maxBytes`, 허용 `contentType` 목록
- Recording 상태 머신 정의(최소):
    - `INITIATED / UPLOADING / UPLOADED / TRANSCRIBING / ANALYZING / DONE / FAILED / DELETED`
- confidence 정책:
    - STT confidence가 낮을 때 프로필 업데이트를 “안 함/약하게 함/유예” 중 무엇으로 고정할지
- Kafka 확장 시점과 전환 전략:
    - Outbox 패턴 채택 여부(권장)
