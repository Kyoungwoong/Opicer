# Universal Sentences — TDD Script

## Scope
- Universal sentence CRUD (admin)
- Random 4 sentences API for home slider
- Error handling via ErrorCode + GlobalExceptionHandler

Non-goals:
- AI generation
- Public unauthenticated access

## Domain Rules (Acceptance Criteria)
1. 타입 4종(의견/경험/비교/문제해결) 데이터를 관리한다.
2. 랜덤 API는 요청마다 active 문장 중 최대 4개를 반환한다.
3. Admin CRUD는 ROLE_ADMIN만 가능하고, not found 시 전용 에러코드를 반환한다.
4. Validation 실패 시 field errors를 포함한다.

## Test Strategy
- Integration tests (MockMvc + SpringBootTest)
- Repository를 이용해 랜덤 데이터 셋 구성

## Test Cases (RED → GREEN)

### A. Random API
**A1 (RED): 랜덤 API는 4개를 반환한다**
- Given: active 문장 6개 + inactive 1개 저장
- When: GET /api/universal-sentences/random?size=4
- Then: 200, data length == 4, code/status fields 존재

Implementation notes (GREEN):
- Service에서 active 목록 shuffle 후 size clamp

### B. Admin Not Found
**B1 (RED): admin update not found 에러코드 반환**
- Given: 존재하지 않는 id
- When: PUT /api/admin/universal-sentences/{id}
- Then: 404 + code = UNIVERSAL_SENTENCE_NOT_FOUND

Implementation notes (GREEN):
- ApiException + ErrorCode로 처리

## How to Run (Local)
```bash
cd opicer-api
./gradlew test
```

## Coverage Map (Requirements → Tests)
- 랜덤 4개 반환 → A1
- Not found 에러코드 → B1

## Next Extensions (Future TDD Items)
- Admin CRUD happy-path 테스트 추가
- Validation 상세 항목 테스트
