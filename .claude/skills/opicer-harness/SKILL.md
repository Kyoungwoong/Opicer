---
name: opicer-harness
description: Run an Opicer unit of work through the multi-agent harness (opicer-planner, opicer-builder, opicer-evaluator) sized to the task. Use for M or L feature work, or when the user asks for the harness, multi-agent or /opicer-harness. S tasks (docs, config, small fixes) skip it.
---

# Opicer harness (orchestrator)

You, the main session, orchestrate. The agents work in fresh contexts and talk through files in `.harness/<id>/` (gitignored). The harness sits inside AGENTS.md §3.5: the plan gate, issue, branch, PR, GitHub review loop and merge stay exactly as they are. Background: `docs/harness.md`.

## 0. Size the task
| Size | When | Flow |
|---|---|---|
| **S** | docs, config, copy, a fix in one place | no harness: normal §3.5 |
| **M** | a feature in one layer (API only or web only), clear scope | contract → builder → **one** evaluator QA |
| **L** | backend + web together, a new user flow, or anything the plan calls risky | planner → contract → builder ⇄ evaluator, up to 3 QA rounds |

- Each component exists because the model can't reliably do that part alone. Use the smallest flow that covers the risk.
- Tell the user the size you picked and why.
- For L, tell them it is a long, expensive run before starting.

`<id>` is `<YYYYMMDD>-<short-slug>` until the issue exists. Then keep the folder and note the issue number in it.

## 1. Plan (M uses your own plan; L uses the planner)
- L: spawn `opicer-planner` with the request and the spec path `.harness/<id>/spec.md`.
- Copy the agreed scope into `plan.md` and present it.
- **Stop for the user's OK** (AGENTS.md §3.5 step 3).
- Then create the issue and branch.

## 2. Sprint contract (before any code)
- Write `.harness/<id>/contract.md` from the approved plan. Number each item and give it two parts:
  - **done when**: observable behaviour
  - **verified by**: a test name, an API call, a DB query, or browser steps
- Spawn `opicer-evaluator` in contract-review mode.
- Revise until it records **AGREED**, at most 2 rounds; otherwise bring the disagreement to the user.
- A scope change found here goes back to the user (§3.1, §12).

## 3. Build
Spawn `opicer-builder` with the contract path, the branch, and the commit attribution trailer. It commits on the branch and writes `build-notes.md`.

## 4. QA loop
- Spawn `opicer-evaluator` in QA mode for round N. It writes `qa-rN.md`.
- **FAIL**: spawn `opicer-builder` with that report to fix every failed item, then run QA round N+1.
- After **3 failed rounds**, stop and report to the user with the latest report. Do not keep looping.
- **PASS**: continue.
- Read each report yourself. If the evaluator passed something you can see is broken, the evaluator is miscalibrated: fail the round, and note it in `.harness/<id>/calibration.md` so the prompt can be tuned (§6).

## 5. PR and review (unchanged §3.5 steps 7–10)
- Open the PR.
- Post the final QA report's findings and evidence as GitHub review comments: inline where a line applies, otherwise a summary review.
- Then run the normal review loop, CI (`ci-pass`), merge and close-out.

## 6. Keep the harness honest
- When a review or the user finds a bug the evaluator passed, add it to the evaluator's calibration list. Keep it short: the pattern, and the PR number.
- When a new model arrives, try removing one component at a time on a real task (e.g. M without the contract review) and keep only what still changes the outcome.
