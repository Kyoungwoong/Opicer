---
name: git-workflow
description: Git Issue, Commit, Branch, Push, Pull Request 규칙을 정의하는 workflow skill
version: 1.1
license: MIT
compatibility: GitHub repositories using git CLI and GitHub MCP
metadata:
   category: workflow
   priority: high
   agent-safe: true
allowed-tools: git github-mcp
---

# Git Workflow Skill

## 1. Purpose

This document defines the **mandatory Git workflow** used in this project.  
It standardizes Issue management, Commit messages, Branch strategy,
and Push / Pull Request procedures.

This skill MUST be followed by **all humans and AI agents**.

### Always Check Rule
- Before any git action, re-check this skill for naming, branching, and commit rules.

---

## 2. Scope

- GitHub-based repositories
- Personal and team development
- All code changes, including tests and configuration

---

## 3. Issue Management Rules

### 3.1 Issue Title Convention

- MUST: Use the following format

```text
[Feature|Bug|Refactor|Docs|Chore] Brief description
```
Example:
```text
[Feature] Adding a user login feature
[Bug] Correct authentication token expiration error
```

---

### 3.2 Issue Description Template
Issue The body must include the following items.

- Background and Purpose
- Detailed requirements
- Estimated scope of work
- Related files or modules

---

## 4. Commit Convention

### 4.1 Commit Message Format
- MUST: Follow the following format
```text
<type>(<scope>): <subject>
```

---

### 4.2 Commit Types

| Type     | Description                     |
|----------|---------------------------------|
| feat | Add new features |
| fix | Fix bug |
| docs | document modification |
| style | Formatting, Semicolon, etc. Code style |
| refactor | Structural changes without behavior change |
| test | Add or modify a test |
| chore | Build, tooling, setup, or other tasks |
| build | Build system or dependency changes |
| ci | CI configuration or scripts |
| perf | Performance improvements |

### 4.3 Commit Discipline (Tidy First)
- MUST: Separate structural commits (refactor, style) from behavioral commits (feat, fix)
- MUST: Ensure all tests pass before committing behavioral changes 
- MUST NOT: Mix refactoring and feature changes in the same commit

---

### 4.4 Commit Message Example
```text
feat(auth): Implementing the user login function
```

- Add JWT token-based authentication
- Create login API endpoints

---

## 5. Branch Strategy (GitHub Flow)

This project follows **GitHub Flow**, a lightweight and production-friendly
branching strategy.

### Core Principles
- `main` is always deployable.
- All changes are made through short-lived feature branches.
- Every branch is created from `main`.
- All changes are merged back into `main` via Pull Requests.
- Direct commits to `main` are NOT allowed.

---

### 5.1 Branch Naming Convention

- MUST: Create a new branch from `main` for every Issue.
- MUST: Follow the format below

```bash
<type>/<issue-number>-<short-description>
```
Example:
```bash
feat/123-user-login
fix/456-auth-token-bug
refactor/789-cleanup-utils
```

### 5.2 AI Agent Branch Prefix Rule
- Branches created by an AI agent MUST be prefixed with `opicer-ai/`.

Format:
```bash
opicer-ai/<type>/<issue-number>-<short-description>
```

Example:
```bash
opicer-ai/feat/123-user-login
```
This rule exists to:
- Clearly distinguish AI-generated branches 
- Improve traceability during review 
- Align with evaluation and audit requirements

### 5.3 Branch Lifecycle (GitHub Flow)
1. Create a branch from main <br/> Example: git checkout -b feat/123-user-login
2. Commit incremental changes to the branch
   * Follow Commit Convention rules 
   * Keep commits small and logical 
3. Push the branch to the remote repository <br/> Example: git push origin feat/123-user-login
4. Open a Pull Request targeting main
   * Follow Pull Request rules 
   * Link the related Issue 
5. After review and verification, merge the Pull Request into main 
6. Delete the branch after merge (recommended)

### 5.4 Restrictions
- MUST NOT: Commit directly to `main`
- MUST NOT: Keep long-lived branches 
- MUST NOT: Reuse branches for multiple Issues 
- MUST: Use one branch per Issue

---

## 6. Push & Pull Request Workflow

### 6.1 Pre-Push Checklist
- MUST: All tests passed
- MUST:Commit conventions are followed
- MUST: associated Issue number connection
- MUST: Don't commit unnecessary files

---

### 6.2 Git Commands

```bash
# Create Branch
git checkout -b <branch-name>

# Commit
git add <path/to/file1> <path/to/file2>
# or stage everything explicitly (use with care):
# git add -A
git commit -m "<type>(<scope>): <subject>"

# Push
git push origin <branch-name>
```
### 6.3 Pull Request Rule

* MUST: Include Issue Connection Statement in PR Body
```
Closes #<issue-number>
```

### 6.4 Pull Request Title Convention
- MUST: Follow the same category prefix format as Issues
```
[Feature|Bug|Refactor|Docs|Chore] Brief description
```

### 6.5 Pull Request Body Template (Recommended)
PR body should include:
- Background and Purpose
- What changed (summary bullets)
- How to test (exact commands / steps)
- Risk / Rollback plan
- Non-goals (explicitly out of scope)
- Checklist
  - [ ] Tests passing (e.g., `./gradlew test`)
  - [ ] No unrelated files included
  - [ ] Docs updated (if needed)
- Issue connection: `Closes #<issue-number>`

### 6.6 Pull Request Template File
- Recommended: Add `.github/PULL_REQUEST_TEMPLATE.md` so GitHub auto-populates PR descriptions.
