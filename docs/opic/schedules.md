# 오픽 시험 일정 (캘린더 데이터)

홈과 가이드의 "오픽 일정" 캘린더는 `opic_schedule` 테이블을 `GET /api/opic/schedules?month=YYYY-MM`로 보여 준다.

- 이번 달 기준 지난달부터 3개월 뒤까지 볼 수 있다.
- 일정이 없는 달이나 불러오기에 실패한 달에는 공식 일정 페이지로 가는 안내 카드를 보여 준다.

## 데이터 출처 원칙

- opic.or.kr은 robots.txt로 자동 수집을 거부한다. 그래서 그 사이트를 긁는 수집기는 만들지 않는다(#137).
- 지금은 공식 일정을 **사람이 확인해서 직접 넣는다**. 행마다 `source = 'manual'`을 기록한다.
- 허가된 출처(공식 API, 주관사 제휴, 공개 데이터 파일)가 생기면 같은 테이블에 배치로 채운다.
  - 이때 `source`에 그 출처 이름을 기록한다.
  - 같은 회차는 `(exam_date, round_name)` 고유 제약으로 덮어쓴다.
- 화면에는 "참고용 일정이에요. 바뀔 수 있으니 접수 전에 공식 사이트에서 확인하세요"를 항상 함께 보여 준다.

## 넣는 방법 (지금)

1. 템플릿 `scripts/opic-schedules.sql`(로컬 전용, git 미추적)에 회차를 한 줄씩 적는다.
   - 값: 시험일, 접수 시작·마감, 성적 발표일, 회차 이름, 조기 발표 여부
   - 다시 실행해도 같은 회차는 갱신만 된다.
2. 실행한다. 맥은 `docker compose exec -T postgres psql -U opicer -d opicer -v ON_ERROR_STOP=1 < scripts/opic-schedules.sql`로 실행한다.

   ```bash
   wsl -e docker exec -i opicer-postgres psql -U opicer -d opicer -v ON_ERROR_STOP=1 < scripts/opic-schedules.sql
   ```

3. 또는 DBeaver로 `opic_schedule` 테이블에 직접 넣는다. `id`는 `gen_random_uuid()`, `source`는 `manual`, `created_at`·`updated_at`은 `now()`다. 이 두 칸은 DB 기본값이 없어서 비우면 저장이 실패한다.

관리자 화면의 입력 탭은 다음 단계에서 만든다.
