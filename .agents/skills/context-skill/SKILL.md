---
name: assessment-context-skill
description: Normalize and record problem context from repository files and user-provided instructions before planning or coding.
version: 1.1
license: MIT
compatibility: GitHub-based repositories; file-driven product specs
metadata:
  category: context
  priority: high
  agent-safe: true
allowed-tools: filesystem shell
---

# Context Normalization Skill

## Goal

Before any planning or coding begins, this skill ensures that the problem context
is clearly understood, normalized, and explicitly recorded.

It exists to prevent misunderstandings, missed constraints, and hidden assumptions
for consistent, low-risk execution.

This skill produces **shared understanding**, not a solution.

---

## When to Use

- Immediately after receiving a problem description
- Before entering **Plan Mode**
- Before creating or updating `plan.md`

---

## Inputs

This skill operates **only** on the following inputs:
- Repository files (e.g., `Opicer.md`, `README.md`, `AGENTS.md`, `plan.md`)
- GitHub Issues provided for the task
- User-provided instructions or clarifications

No external systems or hidden sources are assumed.

---

## Workflow

1. Read all provided problem-related files.
   Summarize the problem objective, inputs, outputs, and constraints
   into a short, clear paragraph.

2. Identify and explicitly list any ambiguous, missing, or unclear points
   under a **Questions / Clarifications Needed** section.

3. Extract and record constraints that affect solution design, such as:
    - Time or memory limits
    - Input size or data volume
    - Ordering guarantees or lack thereof
    - Error handling expectations
    - MVP scope boundaries (what is explicitly excluded)
    - Required tech stack or architectural constraints

4. If examples are provided, extract only the essential patterns
   and edge cases for quick reference.
   Raw examples may be stored separately if needed.

5. Write down all assumptions explicitly
   (e.g., input bounds, uniqueness, sorting guarantees).

6. Organize the above into a concise context summary
   that can be referenced during planning and development.

---

## Outputs

- A concise, human-readable context summary
- A clear list of assumptions
- A clear list of open questions or uncertainties

This output may be written into:
- A working note
- `docs/context.md` (preferred if it doesn't exist)
- Or another appropriate location, as determined by the agent

---

## Handoff Rules

- This skill does NOT define implementation strategy.
- This skill does NOT create `plan.md`.
- This skill does NOT authorize test writing or coding.

Its sole purpose is to ensure that the agent and the user
share the same understanding of the problem **before** planning begins.

Any new information discovered later MUST be reflected
back into the context summary.

---

## Non-Goals

- Designing architecture
- Defining execution steps
- Writing tests
- Writing production code

Those activities belong to subsequent phases.

---

## Final Principle

If the problem is not clearly understood,
**planning and coding MUST NOT begin**.

Clarity precedes correctness.
