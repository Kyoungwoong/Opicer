# 🗣️ OPICER — OPIC Speaking AI Coach
> “The system remembers **how** you speak, not just **what** you said.”

**Opicer**는 OPIC 시험 대비를 위한 **AI 스피킹 코치**입니다.
실제 시험 흐름을 시뮬레이션하고, 사용자의 말하기 습관을 기억해
레벨(NL → AL)에 맞는 피드백과 개선된 스크립트를 제공합니다.

  ---

## ✨ Why Opicer?

- **실전 흐름 시뮬레이션**: OPIC 질문 유형 & 시험 구조 기반
- **개인화된 피드백**: 사용자의 말하기 습관을 기억
- **레벨별 개선 스크립트**: NL → AL 단계별 답변 개선
- **반복 훈련에 최적화**: 빠른 피드백 → 반복 훈련

  ---

## 🧭 Core Experience (MVP)

- **Home**: Daily sentence + 빠른 연습
- **Question Bank**: 주제/유형/레벨 기반 질문 탐색
- **Mock Test**: 실제 시험 흐름으로 진행
- **Analysis & Report**: 말하기 습관/구조/표현 개선 제안
- **Script Improvement**: 레벨별 답변 개선 버전 제공

  ---

## 🧩 Product Philosophy

**Opicer는 ‘암기’가 아닌 ‘말하기 습관 개선’을 목표로 합니다.**

- AI는 점수를 대신하지 않으며
- 사용자의 말하기 습관을 기억하고
- **반복 가능한 개선 루프**를 제공합니다.

  ---

## 🛠 Tech Stack (Target)

- **Frontend**: React + TypeScript
- **Backend**: Java + Spring Boot
- **Database**: PostgreSQL
- **Vector Search**: PostgreSQL (pgvector)
- **Async**: Queue-based processing
- **Optional**: Kafka, Redis, Spring Security, Spring Data JPA, Spring AI

  ---

## 💻 로컬 개발 (macOS · Windows)

```bash
docker compose up -d                    # 로컬 Postgres + pgvector
./opicer-api/run-local.sh               # API (Windows: .\opicer-api\run-local.ps1)
cd opicer-web && pnpm install && pnpm dev   # 웹
```

세 개를 모두 컨테이너로 띄우려면 `docker compose --profile app up -d --build`를 씁니다.

준비물, `.env` 설정, 프로필(local/dev), 테스트, 줄바꿈 규칙은 [docs/dev-setup.md](docs/dev-setup.md)에 있습니다. CI, Docker Hub 이미지, 브랜치 규칙은 [docs/ci-cd.md](docs/ci-cd.md)에 있습니다.

  ---

## 📌 Notes

- Opicer는 **공식 OPIC 점수를 보장하지 않습니다.**
- 사용자 음성 데이터는 **동의 하에 처리**되며 삭제 가능해야 합니다.

  ---

## 📮 Contact

- Project Owner: Opicer Team