# opicer-api 테스트 실행 가이드

## 기본 원칙
- 기본 개발 사이클은 빠른 피드백을 우선한다.
- 동시성 검증은 Executor 기반 멀티스레드 테스트로 수행한다.
- 고부하 재현 테스트는 필요 시 수동 실행한다.

## 실행 명령
- 기본 회귀 테스트
  - `./gradlew test`
  - `@Tag("stress")` 테스트는 제외된다.
- 동시성 전용 테스트
  - `./gradlew concurrencyTest`
  - `@Tag("concurrency")` 대상만 실행한다.
- 고부하 재현 테스트
  - `./gradlew stressTest`
  - `@Tag("stress")` 대상만 실행한다.

## 파라미터 오버라이드
- 스레드 수
  - `-Dopicer.test.threads=10`
- 스레드당 요청 수
  - `-Dopicer.test.requestsPerThread=10000`
- 레이스 윈도우 지연(ms)
  - `-Dopicer.credit.unsafe-delay-ms=50`

예시:
```bash
./gradlew concurrencyTest \
  -Dopicer.test.threads=20 \
  -Dopicer.test.requestsPerThread=1000
```
