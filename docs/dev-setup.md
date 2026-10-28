# 로컬 개발 환경 (macOS · Windows 공통)

맥과 윈도우(WSL 포함)에서 같은 저장소를 번갈아 써도 `git status`가 깨끗하고 같은 명령으로 실행되도록 맞춰 두었다.

## 준비물

| 도구 | 버전 | 비고 |
|---|---|---|
| Docker | Docker Desktop(맥·윈도우) 또는 WSL 안의 Docker | 로컬 Postgres + pgvector |
| JDK | 아무 JDK 17 이상 | Gradle을 띄우는 데 필요하다. 빌드용 JDK 17이 없으면 Gradle이 자동으로 받는다(foojay). 맥: `brew install --cask temurin@17` |
| Node.js | 24 (`opicer-web/.nvmrc`) | nvm 사용 시 `nvm use` |
| pnpm | 12.14.0 (`opicer-web/package.json`의 `packageManager`) | `corepack enable` 후 자동으로 같은 버전을 쓴다 |

## 처음 한 번

1. 로컬 DB를 띄운다(저장소 루트에서). 처음 띄울 때 `opicer`와 테스트용 `opicer_test` DB, `vector` 확장이 만들어진다.

   ```bash
   docker compose up -d
   ```

2. 설정 파일을 만든다. 값은 각자 채우고, `.env`는 커밋하지 않는다. 빈 값은 `application.yaml` 기본값을 쓴다.

   ```bash
   cp opicer-api/.env.example opicer-api/.env
   cp opicer-web/.env.example opicer-web/.env
   ```

3. 웹 의존성을 설치한다.

   ```bash
   cd opicer-web && pnpm install
   ```

DB 데이터(문항 은행 등)는 저장소에 없다. 각 PC의 로컬 DB는 따로이며, 문항 은행 적재는 `docs/opic/question-bank.md`를 따른다.

## 실행

| 대상 | macOS · Linux · WSL | Windows (PowerShell) |
|---|---|---|
| DB | `docker compose up -d` | `docker compose up -d` |
| API (`opicer-api/.env`를 읽고 `bootRun`) | `./opicer-api/run-local.sh` | `.\opicer-api\run-local.ps1` |
| 웹 | `cd opicer-web && pnpm dev` | `cd opicer-web; pnpm dev` |
| 백엔드 테스트 (DB 필요) | `cd opicer-api && ./gradlew test` | `cd opicer-api; .\gradlew.bat test` |
| 웹 검사 | `pnpm lint && pnpm exec tsc --noEmit && pnpm build` | 같음 |
| 세 개 모두 컨테이너로(선택) | `docker compose --profile app up -d --build` | 같음 |

- **평소에는 DB만 컨테이너로** 띄우고 API와 웹은 직접 실행한다. 코드 변경이 바로 반영되고 디버깅이 쉽다.
- `--profile app`은 CI가 빌드하고 Docker Hub에 올리는 이미지와 같은 이미지로 API(8080)와 웹(3000)을 띄운다.
  - 배포 형태로 확인할 때 쓴다.
  - API는 `opicer-api/.env`를 읽고 DB 주소만 컨테이너용으로 바꾼다.
  - 내릴 때: `docker compose --profile app stop api web`
- 이미지·CI·배포는 [ci-cd.md](ci-cd.md)를 본다.

## 설정 프로필

| 파일 | 언제 | 내용 |
|---|---|---|
| `application.yaml` | 항상 | 공통 설정. 아무 프로필도 주지 않으면 `local`이 켜진다 |
| `application-local.yaml` | 기본(내 PC) | localhost DB, `ddl-auto: update`, STT mock, 개발용 기본값. 실제 키는 `opicer-api/.env`에 둔다 |
| `application-dev.yaml` | `SPRING_PROFILES_ACTIVE=dev`(배포 서버) | 접속 정보와 비밀값은 환경변수에서만 받는다. 하나라도 없으면 없는 변수 이름을 알려 주고 시작하지 않는다 |

Claude Code의 미리보기 설정(`.claude/launch.json`)은 PC마다 경로가 달라 저장소에 두지 않는다. 맥에서는 예를 들어 아래처럼 만든다.

```json
{
  "version": "0.0.1",
  "configurations": [
    { "name": "opicer-api", "runtimeExecutable": "bash", "runtimeArgs": ["opicer-api/run-local.sh"], "port": 8080 },
    { "name": "opicer-web", "runtimeExecutable": "pnpm", "runtimeArgs": ["--dir", "opicer-web", "dev"], "port": 3000 }
  ]
}
```

## 줄바꿈과 git

- 줄바꿈은 각 PC의 `core.autocrlf`가 아니라 저장소의 `.gitattributes`가 정한다.
  - 텍스트 파일은 어느 OS에서든 LF로 받는다.
  - `*.bat`, `*.cmd`, `*.ps1`만 CRLF다.
  - jar와 이미지는 바이너리로 둔다.
- `.editorconfig`가 편집기의 저장 형식(UTF-8, LF, 마지막 줄바꿈, 들여쓰기)을 맞춘다.
  - VS Code는 EditorConfig 확장을 설치한다. IntelliJ는 기본으로 지원한다.
- **이 설정 전에 받아 둔 작업 폴더**에는 CRLF 파일이 남아 있을 수 있다. git은 이미 깨끗하게 보지만, 파일까지 LF로 맞추려면 아래 두 줄을 한 번 실행한다.
  - 먼저 변경을 커밋하거나 stash한다.
  - 이 명령은 추적 중인 파일을 저장소 내용으로 다시 써서, 커밋하지 않은 변경이 사라진다.
  - 추적하지 않는 파일(`.env`, `plan.md`, `scripts/` 등)은 건드리지 않는다.

  ```bash
  git rm -r --cached -q .
  git reset -q --hard
  ```

- 실행 비트: `gradlew`와 `run-local.sh`는 저장소에 실행 권한(755)으로 들어 있다. 맥에서 권한 오류가 나면 `git checkout -- opicer-api/gradlew opicer-api/run-local.sh`로 다시 받는다.
