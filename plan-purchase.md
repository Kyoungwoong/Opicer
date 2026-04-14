# 중복 주문/결제 방지 실험 설계 (크레딧 구매 도메인)

## Summary
- 크레딧 구매 도메인에 취약 버전을 먼저 구현해 중복 주문/결제를 재현한 뒤, **전략별(Unique, Idempotency, Redis, MQ)**로 단계적으로 비교 실험한다.
- 현재 범위는 DB만 먼저, 전략 전환은 프로파일/환경설정으로 결정한다.
- 결제는 모의 결제사 테이블로 승인/실패를 시뮬레이션하며, API는 주문 생성 → 결제 승인의 2단계로 고정한다.

## Key Changes / Interfaces
- 도메인: credit (크레딧 구매)
- API 흐름 (고정)
  1. POST /api/credits/orders → 주문 생성
  2. POST /api/credits/payments/confirm → 결제 승인(모의 결제사 응답 포함)
- 전략 전환 (프로파일)
  - app.dup-strategy = none | unique | idempotency | redis | mq
- 핵심 테이블(최소)
  - credit_orders: id, user_id, package_id, amount, status, created_at
  - credit_payments: id, order_id, provider_tx_id, status, created_at
  - idempotency_keys: id, user_id, key, request_hash, status, response_snapshot, created_at, expires_at
  - mock_payment_provider: id, provider_tx_id, decision, created_at (모의 결제 승인/실패 기록)

## Scenario / Experiment Design
- 취약 시나리오(재현): 동일 order 또는 동일 결제 요청을 연속 클릭/재시도/동시 요청으로 여러 번 보내 중복 생성 확인.
- 전략별 적용 위치
  1. DB Unique: credit_payments(order_id) 또는 provider_tx_id 유니크로 차단
  2. Idempotency: confirm 진입 시 idempotency_keys 저장 후 동일 키 재요청은 기존 응답 반환
  3. Redis Lock: lock:credit:{orderId} (DB 외부 인프라 단계)
  4. MQ 직렬화: 결제 승인 요청을 큐에 적재 (DB 외부 인프라 단계)
- 비교 항목
  - 중복 발생률
  - 평균 처리 시간/지연
  - 실패 응답률
  - 재시도 시 일관성
  - 운영 복잡도/추가 인프라 비용

## Test Plan (실험)
- 동시성 부하 테스트: k6 스크립트로 동일 요청을 다중 동시 실행
- 재현 케이스
  - 연속 클릭(초당 n회)
  - 네트워크 재시도(같은 idempotency key)
  - 멀티 인스턴스 시뮬레이션(동일 요청 병렬)

## Assumptions
- 도메인: 크레딧 구매
- 인프라: DB만 먼저 (Redis/MQ는 단계적으로 추가)
- 전략 전환: 프로파일/환경설정
- 결제사 연동: 모의 결제사 테이블 사용
- API 흐름: 주문 생성 → 결제 승인 2단계 고정
