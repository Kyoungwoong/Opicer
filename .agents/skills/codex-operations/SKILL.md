---
name: codex-operations
description: >
  Operational rules for using Codex effectively and safely in a repository.
  Use this skill whenever exploring the codebase, reading files, searching,
  applying edits, running commands, or using Git/GitHub tools.
license: MIT
compatibility: >
  Intended for Codex CLI usage in a Git repository. Assumes `rg` (ripgrep) is
  available; if not, fall back to alternatives (grep/find).
metadata:
  category: core
  priority: high
  always-on: true
allowed-tools: filesystem shell git github-mcp
---

# Codex Operations Skill

This skill defines **how** the agent must operate tools and repository edits.
It complements `AGENTS.md` (governance) and MUST be followed for all work.

---

## 1. Tooling Priority

- MUST: Prefer dedicated tools when available; otherwise use terminal commands.
- MUST: Use terminal commands only when no dedicated tool can perform the action.
- SHOULD: Prefer `rg` for searching text and file discovery (`rg --files`).
    - If `rg` is not available, MAY fall back to `grep` / `find`.

---

## 2. Exploration and Reading Files

- MUST: Think first and decide all likely files/resources needed before reading.
- MUST: Batch reads/searches whenever possible (avoid one-file-at-a-time thrashing).
- SHOULD: When uncertain, read slightly more context rather than repeatedly re-reading the same file.
- MUST: If repeated reads/edits are not yielding progress, stop and ask the user for direction.

---

## 3. Editing Rules (Patch Discipline)

- MUST: Make edits in small, reviewable increments aligned to the current phase (Plan/TDD/Dev).
- MUST: Prefer patch-based edits (e.g., `apply_patch`) for precise changes.
- MUST NOT: Perform broad search-and-replace across the codebase unless explicitly required by the plan and approved by the user.
- MUST: Preserve existing codebase conventions (naming, formatting, patterns).
- SHOULD: Use ASCII by default; only introduce non-ASCII characters when justified or already used in the file.

---

## 4. Commands and Execution Safety

- MUST: Request user approval before running any command that can modify repository state or system state.
- MUST: Set a working directory explicitly when running commands (avoid `cd`-driven state when possible).
- MUST: Prefer short, targeted commands.
- MUST: After running commands, report relevant output succinctly (errors first).

---

## 5. Git Safety Rules

- MUST: Treat the working tree as potentially dirty.
- MUST NOT: Revert or discard changes you did not create unless the user explicitly requests it.
- MUST: If unexpected changes appear, stop and ask the user how to proceed.
- MUST NOT: Use destructive commands such as `git reset --hard`, `git checkout --`, or rewriting history unless explicitly requested and approved by the user.
- MUST NOT: Amend commits unless explicitly requested by the user.

---

## 5.5 Read-Only Review Guard

When the user asks for review/explanation (or when operating in Read-Only Mode per `AGENTS.md`):
- MUST NOT: edit files (no `apply_patch`)
- MUST NOT: run repo-modifying commands
- MUST: restrict to reading files and reporting findings

If an edit is needed during review, MUST ask the user to confirm exiting Read-Only Mode first.

---

## 6. Commit and Change Hygiene

- MUST: Keep changes minimal per iteration (align with the Change Budget Rule in `AGENTS.md`).
- MUST: Ensure tests pass before committing (unless the user explicitly requests an intermediate commit).
- SHOULD: Use small, frequent commits rather than large, infrequent ones.
- MUST: Separate structural vs behavioral changes into different commits (Tidy First).

---

## 7. GitHub MCP Usage

- MUST: Use GitHub MCP for:
    - Issue creation
    - Pull Request creation
    - PR comments/reviews and other GitHub UI actions
- SHOULD: Use local git for:
    - branch, add, commit, push
- MAY: Use GitHub MCP for push/branch only if:
    - local push is blocked (permissions/sandbox/network), or
    - the user explicitly requests it.

---

## 8. Stop Conditions

The agent MUST stop and ask the user if:
- Requirements are ambiguous or conflict with `plan.md`
- A change would exceed small-scope edits (large refactor, many files) without explicit approval
- A command needs elevated permissions or could have broad side effects
- The agent is unsure whether a change is structural or behavioral (Tidy First boundary)
