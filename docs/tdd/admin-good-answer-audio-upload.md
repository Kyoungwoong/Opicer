# Admin Good Answer Audio Upload — TDD Script

## Scope
- Admin 음성 업로드 → Whisper STT → 텍스트/임베딩 저장
- Admin UI 탭에서 등록/조회/삭제

Non-goals:
- S3 연동
- 발음/어조 점수 계산

## Domain Rules (Acceptance Criteria)
1. 음성 업로드 요청은 audio, topicId, level이 필수다.
2. 음성 업로드 성공 시 sampleText, audioUrl, id가 저장된다.
3. Whisper 실패 시 AI_TRANSCRIPTION_FAILED를 반환한다.
4. 임베딩 실패 시 AI_EMBEDDING_FAILED를 반환한다.

## Test Strategy
- Unit tests:
  - GoodAnswerSampleService: STT/Embedding 흐름 단위 테스트
- Integration tests:
  - AdminGoodAnswerAudioUploadController WebMvcTest

## Test Cases (RED → GREEN)

### A. Upload API
**A1 (RED): POST /api/admin/good-answers/audio 성공**
- Given: audio 파일 + topicId + level
- When: POST multipart
- Then: 200, response에 id/sampleText/audioUrl 포함

Implementation notes (GREEN):
- Multipart 처리 + Whisper STT 호출 + 임베딩 생성 + 저장

**A2 (RED): audio 누락 시 400**
- Given: audio 없음
- When: POST
- Then: 400 VALIDATION_ERROR

### B. Failure Handling
**B1 (RED): Whisper 실패 시 AI_TRANSCRIPTION_FAILED**
- Given: Whisper client throws
- When: POST
- Then: 에러코드 반환

**B2 (RED): 임베딩 실패 시 AI_EMBEDDING_FAILED**
- Given: Embedding client throws
- When: POST
- Then: 에러코드 반환

## How to Run (Local)
```bash
./gradlew test
```

## Coverage Map (Requirements → Tests)
- Rule 1 → A2
- Rule 2 → A1
- Rule 3 → B1
- Rule 4 → B2

## Next Extensions (Future TDD Items)
- S3 업로드 경로
- 발음/어조 점수 계산 API
