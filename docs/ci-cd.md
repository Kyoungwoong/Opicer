# CI/CD

GitHub Actions로 검사하고, Docker Hub에 이미지를 올린다. 배포(AWS)는 아직 없다. 나중에 붙일 위치는 맨 아래에 적어 둔다.

## 흐름

```
브랜치 → PR ──CI(ci.yml)──▶ ci-pass 녹색 + 리뷰 대화 해결 ──▶ main 머지 ──CD(docker-publish.yml)──▶ Docker Hub
                                                               └─ v1.2.3 태그 ──CD──▶ Docker Hub(버전 태그)
```

## CI — `.github/workflows/ci.yml`

PR(main 대상), main push, 수동 실행에서 돈다. 같은 브랜치에 새 커밋이 오면 이전 실행은 취소된다.

| job | 언제 | 하는 일 |
|---|---|---|
| Detect changes | 항상 | PR에서 바뀐 쪽(api / web / docker)을 판단한다. main push는 전부 돈다 |
| Backend tests | api가 바뀌었을 때 | pgvector Postgres 서비스(`opicer_test`)를 띄우고 `./gradlew test` 실행. 실패하면 리포트를 artifact로 올린다 |
| Web lint, types and build | web이 바뀌었을 때 | `pnpm install --frozen-lockfile` → lint → `tsc --noEmit` → build |
| Docker image builds | api·web·compose·workflow가 바뀌었을 때 | 두 이미지를 빌드만 한다(push 안 함) |
| PR title and branch name | PR일 때 | 아래 규칙을 검사한다 |
| **ci-pass** | 항상 | 위 job이 하나라도 실패하거나 취소되면 실패한다(건너뛴 것은 통과). **main의 필수 체크는 이것 하나다** |

## PR 규칙 (CI가 검사)

- PR 제목: `<type>(<scope>): <subject>`
  - type은 `feat`, `fix`, `refactor`, `docs`, `chore`, `test` 중 하나다. scope는 선택이다.
  - 예: `feat(mocktest): pair questions 14-15`
- 브랜치: `<prefix>/<type>/<이슈 번호>-<설명>`
  - 예: `opicer-ai/feat/12-topic-tab`, `hiro/fix/30-login-redirect`

## main 브랜치 규칙 (GitHub ruleset "main protection")

- 직접 push, 삭제, force push가 막혀 있다. 모든 변경은 PR로만 들어온다.
- 머지 조건
  - `ci-pass` 통과(최신 main 기준)
  - 리뷰 대화가 모두 해결됨
  - 승인 인원은 0명이다. 혼자 작업하므로 1명으로 두면 본인 PR을 머지할 수 없다.
- 머지 방식은 merge commit만 쓴다.
- 관리자도 예외가 없다.
- 머지한 브랜치는 자동으로 지워진다.

## CD — `.github/workflows/docker-publish.yml`

main push, `v*` 태그, 수동 실행에서 돈다. API와 웹 이미지를 **한 저장소 `youngwoong/opicer`** 에 태그 접두어로 나눠 올린다.

| 이벤트 | API 태그 | 웹 태그 |
|---|---|---|
| main 머지 | `api-<커밋 7자리>`, `api-main` | `web-<커밋 7자리>`, `web-main` |
| 태그 `v1.2.3` | `api-1.2.3` | `web-1.2.3` |

- 배포에는 바뀌지 않는 `api-<sha>` 또는 `api-1.2.3`을 쓴다. `*-main`은 계속 바뀌는 태그이고 `latest`는 만들지 않는다.
- 버전 배포: `git tag v1.0.0 && git push origin v1.0.0`
  - 태그는 main에 있는 커밋에만 단다.
  - main에 없는 커밋(머지 안 한 브랜치 등)에 단 태그는 CI를 거치지 않았으므로 workflow가 실패시킨다.
- 웹 이미지에는 빌드할 때 `NEXT_PUBLIC_NAVER_MAP_CLIENT_ID`가 들어간다. 공개되는 값이므로 GitHub **Variables**에 둔다.

### 처음 한 번 (사용자가 직접)

1. Docker Hub → Account settings → Personal access tokens에서 **Read & Write** 토큰을 만든다. 비밀번호 대신 쓴다.
2. Docker Hub에 저장소 `youngwoong/opicer`를 만든다.
   - 공개 범위를 고른다. 무료 플랜은 비공개 저장소 수가 적어, 한 저장소에 태그로 나누는 지금 구조가 맞다.
   - 만들지 않고 push하면 공개 저장소로 생길 수 있다.
3. GitHub 저장소 → Settings → Secrets and variables → Actions
   - Secrets: `DOCKERHUB_USERNAME`(= `youngwoong`), `DOCKERHUB_TOKEN`(1의 토큰)
   - Variables(선택): `NEXT_PUBLIC_NAVER_MAP_CLIENT_ID`, `DOCKERHUB_REPOSITORY`(기본 `youngwoong/opicer`)
4. Actions → Docker publish → Run workflow로 한 번 돌려 Docker Hub에 태그가 생기는지 본다.

secret이 없으면 이미지를 빌드만 하고 push는 건너뛴다. 실행은 성공으로 끝나고 안내만 남는다.

## 이미지 실행

```bash
# API: local 프로필이 기본. 서버에서는 dev 프로필과 필수 환경변수를 준다.
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --env-file api.env youngwoong/opicer:api-<sha>
# 웹: API 주소는 실행할 때 준다.
docker run -p 3000:3000 -e OPICER_API_BASE_URL=http://<api-host>:8080 youngwoong/opicer:web-<sha>
```

dev 프로필은 아래 변수 중 하나라도 없으면 시작하지 않는다. 없는 변수 이름을 모두 알려 준다(`RequiredDevSettings`).

`OPICER_DB_URL`, `OPICER_DB_USER`, `OPICER_DB_PASSWORD`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`, `JWT_SECRET`, `FRONTEND_BASE_URL`, `OPENAI_API_KEY`

## 나중에 AWS를 붙일 때

- 레지스트리를 ECR로 옮길 때: `docker-publish.yml`의 `IMAGE`와 로그인 단계를 바꾼다. AWS 인증은 OIDC(`aws-actions/configure-aws-credentials`)로 하고 장기 키는 쓰지 않는다.
- 배포 job: `publish` 뒤에 `needs: publish`로 붙인다.
  - 예: ECS 태스크 정의의 이미지를 `api-<sha>`로 바꿔 배포한다.
  - GitHub Environments(`dev`)로 secret과 승인 단계를 분리한다.
- DB 스키마: 운영 전에 Flyway를 도입하고, dev 프로필의 `ddl-auto`를 `validate`로 바꾼다(로드맵 9번).
