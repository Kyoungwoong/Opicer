---
name: assessment-problem-solving-skill
description: Break problems down, validate correctness first, and avoid premature optimization during timed assessments.
allowed-tools: filesystem shell
---

# Problem Solving Skill

## Core Principles
- Keep solutions as simple as possible; favor clarity over cleverness.
- Validate correctness before refactoring—ensuring the basic path works is higher priority than optimizations.
- Avoid unnecessary abstractions and general-purpose frameworks unless the problem explicitly requires them.

## Workflow
1. Read the requirement, then restate it in a single sentence or paragraph to confirm understanding.
2. Sketch the simplest algorithm that meets the constraints, then walk through sample inputs manually (happy path + edge cases).
3. Implement that algorithm, keeping functions small and naming clear.
4. After a correct draft is available, consider only those structure changes that improve readability or maintainability without adding complexity.

## Notes
- Assume input sizes are moderate unless constraints say otherwise.
- Document any assumptions (e.g., sorting guarantees, unique IDs) in comments or README TODOs.
