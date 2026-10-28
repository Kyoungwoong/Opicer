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

## 넣는 방법

### 관리자 화면 (기본)
1. 관리자로 로그인해 관리자 화면의 **시험 일정** 탭을 연다.
2. 시험일과 회차명(예: 정기 평가)을 적는다. 접수 시작·마감, 성적 발표일, 조기 발표는 아는 것만 넣는다. 빈 날짜는 사용자 화면에 "미정"으로 보인다.
   - 날짜 순서는 접수 시작 ≤ 접수 마감 ≤ 시험일 ≤ 성적 발표일이어야 한다.
   - 같은 시험일에 같은 회차명은 하나만 둘 수 있다. 같은 날 회차가 여럿이면 회차명을 다르게 적는다.
3. 비슷한 회차가 이어지면 목록의 **복사**로 값을 채운 뒤 날짜만 바꿔 추가한다.
4. 목록은 다가오는 일정이 기본이다. 다가오는 일정이 없으면 탭에 경고가 나온다.

API: `/api/admin/opic-schedules`(ROLE_ADMIN) — 목록(GET), 추가(POST), 수정(PUT `/{id}`), 삭제(DELETE `/{id}`). 수정해도 `source`는 바뀌지 않는다.

### SQL로 한꺼번에 넣기 (선택)
1. 템플릿 `scripts/opic-schedules.sql`(로컬 전용, git 미추적)에 회차를 한 줄씩 적는다.
   - 다시 실행해도 같은 회차는 갱신만 된다.
2. 실행한다. 맥은 `docker compose exec -T postgres psql -U opicer -d opicer -v ON_ERROR_STOP=1 < scripts/opic-schedules.sql`로 실행한다.

   ```bash
   wsl -e docker exec -i opicer-postgres psql -U opicer -d opicer -v ON_ERROR_STOP=1 < scripts/opic-schedules.sql
   ```

3. DBeaver로 직접 넣을 때는 `id`에 `gen_random_uuid()`, `source`에 `manual`, `created_at`·`updated_at`에 `now()`를 넣는다. 이 두 칸은 DB 기본값이 없어서 비우면 저장이 실패한다.
