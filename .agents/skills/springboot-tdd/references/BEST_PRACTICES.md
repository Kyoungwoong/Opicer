# Best Practices (springboot-tdd)

## Example Script
Use `scripts/controller_test_template.java` as a ready-to-use WebMvcTest template.

## How To Apply
- Copy into `src/test/java/...` and adjust controller/service names.
- Add `@MockBean` for dependencies.
- Verify status + payload for each endpoint.

## Notes
- Keep controller tests focused on HTTP behavior.
- Use service unit tests for business logic.
