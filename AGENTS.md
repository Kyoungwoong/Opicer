# AGENTS.md (Opicer / VibeCoding Protocol)

> Version: 1.0  
> Status: Stable

This document defines how the AI agent MUST behave while building **Opicer**.
The objective is to deliver **correct, testable, and maintainable backend features**
with fast VibeCoding feedback loops—not just code generation.

The agent is **not autonomous**. All work is **approval-driven**, **phase-based**, and follows
a VibeCoding workflow: fast feedback loops, visible reasoning, and correctness-first delivery.

---

## 1. Core Principle (Non-Negotiable)

- MUST: Optimize for **correctness, reproducibility, and clarity**.
- MUST: Treat any calculation logic as **high-risk**: no guesswork, no silent assumptions.
- MUST: Maintain explicit **policies/definitions** (e.g., rounding, minSpend base, apply order).
- MUST: Provide **evidence** for correctness (tests, fixed scenarios, expected outputs).

---

## 2. Opicer Delivery Alignment (Why this protocol exists)

This protocol is designed to keep delivery:
- **Problem-specific**: objectives and acceptance criteria are explicit
- **Scope-controlled**: MVP vs out-of-scope is enforced
- **Testable**: scenarios and edge cases are deterministic
- **AI-collaborative**: AI outputs are validated and documented
- **Iterative**: small increments with visible progress

---

## 3. Global Rules (Always Applied)

### 3.1 Approval-Driven Work
- MUST: Get the user's explicit **OK on the plan** (Delivery Cycle step 3, §3.5) before creating an issue, a branch, or any code.
- After that OK, run the Delivery Cycle through to a merge-ready PR **without** asking again at each phase. Still MUST stop and ask when:
    - scope would expand beyond the approved `plan.md`
    - requirements are unclear or missing
    - an action is destructive or otherwise not covered by the plan
- MUST NOT: Write production code or refactor without explicit approval.

### 3.2 Source of Truth & Context Hygiene
Priority order:
1) **Opicer.md** and the user’s final chosen requirements
2) `plan.md` (durable working memory: scope, policies, decisions, progress)
3) Repository evidence (code, tests, configs, command outputs)
4) Conversation (lowest priority)

- MUST: Capture all decisions in `plan.md` so work survives context compaction.
- MUST NOT: Rely on conversation history as the only memory.

### 3.3 Correctness Guardrails (Opicer Backend)
- MUST: Define and freeze API contracts before implementation:
    - request/response DTOs
    - validation rules
    - error response shape
- MUST: Implement rules in code **and** tests consistently.
- MUST: Flag any ambiguity as an open question in `plan.md` and request user choice.

### 3.4 Git Workflow and Prompt Logging
- MUST: Follow the `git-workflow` skill for Issue → Branch → Commit → Push → PR.
- MUST: Create an Issue first, then create a branch named `opicer-ai/<type>/<issue-number>-<short-description>` from `main`.
- MUST: Make **small, feature-scoped commits**. If a change can be split, it must be split.
- MUST: Use commit message format `<type>(<scope>): <subject>` as defined in `git-workflow`.
- MUST: Use relevant skills before working, and follow their instructions.
- MUST: **Log every user request in `docs/worklog/`** (format: `docs/worklog/README.md`).
    - One file per piece of work: `docs/worklog/YYYY-MM-DD-<short-slug>.md`. Append to the day's file when the work continues.
    - Record the user's prompts **verbatim** (original language), then the analysis, the decisions and plan (link `plan.md` / plan files), what was built (PRs, commits), verification evidence, and open items.
    - Write the entry when the request arrives, and update it at Close Out (§3.5 step 10).
    - Never paste secrets, API keys, credentials, or personal data. Redact them as `[REDACTED]`.
    - `plan.md` stays the working memory and is git-ignored. `docs/worklog/` is the committed history.

### 3.5 Delivery Cycle (Every Unit of Work)
Every unit of work, whether feature, fix, refactor, docs or chore, goes through this exact loop.
Each step is listed with the FSM state from §4 that it belongs to.

1. **Analyze** (Problem Interpretation)
    - Do a *fresh* analysis of the current state: code, running app, docs, and the last merged result.
    - Do not reuse assumptions from the previous cycle without re-checking them.
    - This step is read-only.
2. **Plan** (Plan Mode)
    - Write the plan in `plan.md` (template §6). It must include scope in/out, open questions, tasks, and acceptance criteria.
    - Present the plan to the user.
3. **User OK** (gate)
    - STOP and wait for an explicit OK on the plan.
    - Nothing below may start without it.
4. **Issue**
    - Create a GitHub issue that follows `git-workflow` §3: title `[Feature|Bug|Refactor|Docs|Chore] ...`, plus the body template.
5. **Branch**
    - Update `main` (`git switch main && git pull --ff-only`).
    - Create and switch to `opicer-ai/<type>/<issue-number>-<short-description>`.
    - The issue comes before the branch because the branch name needs the issue number.
6. **Build** (Build Mode)
    - Make small, scoped commits (`<type>(<scope>): <subject>`).
    - Run the local checks before pushing: backend tests, frontend `tsc --noEmit` / `pnpm lint` / build as relevant.
7. **Pull Request**
    - Push the branch and open a PR to `main`.
    - Use `.github/pull_request_template.md` and the `git-workflow` §6 title format, and include `Closes #<issue-number>`.
8. **Review Loop** (Verify Mode). Repeat until a pass finds nothing:
    1. **Review** the PR diff as a reviewer, not as its author. Check correctness, scope vs `plan.md`, conventions, tests, security, and UI on desktop and 375px when relevant.
    2. **Comment on GitHub.**
        - Post each finding as an inline review comment on the exact line.
        - Add a summary review (event `COMMENT`; GitHub does not allow approving your own PR).
        - Findings that exist only in chat or terminal output do not count.
    3. **Fix.**
        - Address each finding in new commits. Do not force-push over reviewed history.
        - Push, then reply on each thread with the fixing commit SHA and resolve it.
    4. **Re-review** the updated diff from step 1.
    - The loop ends when a review pass yields **no new findings** and the PR breaks **no check that passes on `main`**.
        - Run any check that already fails on `main` against both branches.
        - List those failures in the report as pre-existing, and track them in a follow-up issue. They do not block this PR.
    - Post a final summary comment that says the loop is done and lists the verification evidence (commands and results).
    - If a finding is out of scope for this PR, do not fix it here. Open a follow-up issue and link it in the thread.
9. **Merge** (Package Mode)
    - Merge only after the review loop has ended cleanly.
    - If the agent is permitted to merge, it merges the PR itself.
    - If it is not permitted, it hands the PR link to the user, asks them to merge, and waits.
10. **Close Out**
    - After the merge, switch to `main` and pull.
    - Delete the merged branch locally and on the remote.
    - Update `plan.md`: progress and Iteration Log.
    - Update the request's `docs/worklog/` entry: result, PR links, verification, open items (§3.4).
    - The next unit of work starts again at **step 1** with a new analysis and a new plan OK.

---

## 4. VibeCoding Workflow (Finite State Machine)

The agent operates in strict states. No skipping.

1) **Problem Interpretation Mode**
2) **Plan Mode**
3) **Build Mode**
4) **Verify Mode**
5) **Package Mode**
6) **Read-Only Mode**

The agent MUST NOT reorder or merge states.

---

## 5. Problem Interpretation Mode (Mandatory First Step)

### Purpose
Convert the user’s idea into a **crisp, testable** problem statement.

### Output (1 page max)
- Objective (1–2 sentences)
- Inputs / Outputs
- Policies/Definitions (API contracts, validation, error rules)
- Constraints (scope boundaries)
- Ambiguities (questions requiring user decision)

### Exit Condition
- MUST: Proceed to Plan Mode. The approval gate is the plan OK (§3.5 step 3).
- MUST: Carry any ambiguity into the plan's Open Questions so the user decides it at that gate.

---

## 5.1 Architecture Choice Rule (Clean vs Hexagonal)

Default to **Clean Layered**. Move to **Hexagonal** only when at least one is true:
- multiple input adapters are required (REST + batch + messaging)
- multiple output adapters are required (DB + external APIs)
- strong test isolation/replaceability is required
- cross-domain boundaries must be enforced

If Hexagonal is chosen:
- Define `ports.in` and `ports.out` interfaces first
- Controllers must call `ports.in`
- JPA repositories must implement `ports.out` via adapters
- Domain must not depend on Spring/JPA

---

## 6. Plan Mode

### Purpose
Design the MVP to maximize delivery speed and minimize risk.

### Rules
- MUST: Produce/update `plan.md` using the template below.
- MUST: Separate **MVP scope** and **Out of Scope** explicitly.
- MUST: List **fixed scenarios** with expected outputs (used later as acceptance tests).
- MUST: Include an **iteration log** plan (how changes will be recorded).

### `plan.md` Template (Required)
```markdown
# Plan: Opicer Backend (Feature/Module)

## Context
- Problem statement:
- References (Opicer.md, issues, decisions):

## Objective
- (1–2 sentences)

## Scope
- In (MVP):
- Out (v2+):

## API Contracts (Frozen for MVP)
- Endpoints:
- Request DTOs:
- Response DTOs:
- Validation rules:
- Error responses:

## Requirements (Acceptance Criteria)
- [ ] ...
- [ ] ...

## Scenarios (Fixed with Expected Outputs)
- S1: ...
  - Input:
  - Expected:
- S2: ...
- S3: ...

## Edge Cases (Deterministic Handling)
- E1: ... rule ...
- E2: ... rule ...

## Design
- API resources:
- Data model:
- Transaction boundaries:
- Security rules (if any):

## Tasks
- [ ] 1. ...
- [ ] 2. ...

## Verification (Definition of Done)
- Automated: unit/integration tests (JaCoCo ≥ 80%)
- Manual: run fixed scenarios and match expected outputs
- Documentation: README + AI collaboration notes (if required)
```
### Exit Condition
- MUST: Present `plan.md` to user.
- MUST: Wait for user approval before entering Build Mode.

## 7. Build Mode

### Purpose
Implement the prototype and documentation as planned.

### Rules
- MUST: Implement in small increments.
- MUST: Keep business logic in **service/domain** (testable without HTTP).
- MUST: Use tests to lock correctness for each rule and scenario.
- MUST: Record material changes in an **Iteration Log** (file or `plan.md` section).
- MUST NOT: Expand scope beyond `plan.md` without approval.

### Implementation Guidance (Opicer)
- SHOULD: Start from service + tests → then controller.
- SHOULD: Build scenario-driven: implement S1, S2, S3 in order.
- SHOULD: Keep API contracts visible in README to avoid ambiguity claims.

### Exit Condition
- MUST: Open the PR and continue to Verify Mode (the Review Loop, §3.5 step 8). No separate approval is needed, unless scope changed.

---

## 8. Verify Mode

### Purpose
Demonstrate reliability and eliminate “instant fail” risk.

### Rules
- MUST: Run all tests and report results.
- MUST: Manually validate each fixed scenario:
    - Provide inputs
    - Confirm outputs match expected values exactly
- MUST: Check edge cases:
    - caps, minSpend failure, negative/zero inputs, shipping clamp, rounding
- MUST: If any mismatch occurs, return to Build Mode and fix.
- MUST: Record review findings as GitHub PR review comments, and loop through review → comment → fix → re-review (§3.5 step 8).

### Exit Condition
- MUST: Post the verification report as the final PR review comment:
    - commands run
    - pass/fail
    - scenario output confirmations
- MUST: Continue to merge (§3.5 step 9) only when the last review pass found nothing.

---

## 9. Package Mode (Release Readiness)

### Purpose
Prepare the deliverables Opicer expects.

### MUST Deliverables
- `README.md` with:
    - how to run
    - API contracts/assumptions
    - sample scenarios (inputs + expected outputs)
- Planning/Dev document (this can be the 기획서 + `plan.md`)

### Optional (High ROI for scoring)
- `TESTCASES.md` (table of scenario/edge case inputs & expected outputs)
- short demo GIF or screenshots

### Exit Condition
- MUST: Merge per §3.5 step 9, or hand the merge to the user if the agent may not merge.
- MUST: Then do Close Out (§3.5 step 10) and start the next cycle at step 1.

---

## 10. Read-Only Mode (Safety Guard)

### When triggered
- Reviewing code, explaining design, summarizing repo, or user requests “review/explain”.

### Rules
- MUST NOT: Modify files.
- MUST NOT: Run commands that change repository state.
- MUST: Only read and report findings.
- Exception: posting review comments to a GitHub PR is allowed. It does not change the repository.

---

## 11. AI Collaboration Rules (VibeCoding-Optimized)

- MUST: Use AI to accelerate drafting, but **never** trust outputs blindly.
- MUST: Validate AI outputs by at least one:
    - unit tests
    - fixed scenario expected-output matching
    - manual calculation cross-check
- MUST: Log key AI contributions and corrections in `AI_COLLABORATION.md` (or `plan.md`).
- SHOULD: Prefer “prompt → hypothesis → test → adjust” cycles over large one-shot generation.

---

## 12. Change Budget Rule

- MUST: Each increment should be small and reviewable.
- SHOULD: Avoid touching more than one logical unit per increment.
- MUST: If scope expands, stop and request a `plan.md` update approval.

---

## 13. Final Principle

The agent MUST behave as a disciplined engineering partner demonstrating Opicer competencies:
**clear problem definition, controlled scope, reproducible scenarios, deterministic edge handling, and verified correctness**.
If any uncertainty arises, the agent MUST stop and ask the user.

---

## 14. Frontend + Backend Scope

- This repository includes **both frontend and backend**.
- Backend: Java + Spring Boot + Spring Data JPA (REST API)
- Frontend: React + TypeScript (if present)
- Apply the same VibeCoding workflow to both stacks.

---

## 15. Skill Wiring (Required)

The agent MUST activate these skills when relevant:
- `codex-operations`: for any reading, searching, editing, or commands
- `backend-patterns`: for REST API and architecture decisions
- `java-coding-standards`: for Java/Spring code conventions
- `springboot-tdd` + `tdd-loop` + `tdd-workflow`: for all feature work
- `git-workflow`: for all git actions and naming
- `jpa-patterns`: when working with JPA entities or queries
If multiple skills apply, use the minimal set and follow their order.
