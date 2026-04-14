# Credit Purchase Duplicate — Vulnerable Version (TDD Script)

## Scope
- 크레딧 주문 생성 API
- 결제 승인 API (취약 버전)
- 동시 요청 시 중복 결제가 발생하는 테스트
- k6 부하 테스트 스크립트

Non-goals:
- idempotency/unique/lock/queue 적용

## Domain Rules (Acceptance Criteria)
1. 결제 승인 요청이 동시에 들어오면 중복 결제가 발생할 수 있어야 한다.
2. 중복 발생 원인이 코드 주석과 테스트로 드러나야 한다.
3. 동시성 테스트로 중복을 재현할 수 있어야 한다.

## Test Strategy
- Integration test: SpringBootTest + ExecutorService
- Load test: k6 스크립트

## Test Cases (RED → GREEN)

### A. Concurrency
**A1 (RED): concurrent confirm creates duplicates**
- Given: 동일 orderId
- When: 동시 confirm 호출
- Then: payment count > 1

Implementation notes (GREEN):
- check-then-act 패턴 유지
- optional unsafe delay to widen race window

## How to Run (Local)
```bash
./gradlew test
```

## Coverage Map
- Rule 1 → A1
- Rule 2 → A1 (code comment + test)
- Rule 3 → A1 + k6 script

## Next Extensions
- unique constraint 적용
- idempotency key 적용
- redis lock 적용
- mq 직렬화 적용
