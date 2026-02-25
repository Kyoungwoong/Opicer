# Plan: MyPage UI

## Context
- Problem statement: 마이페이지 UI/흐름 추가 (드롭다운 → 마이페이지, 히스토리 섹션 분리, 상세 패널 전환)
- References (Opicer.md, issues, decisions): 사용자 지시사항(2026-02-25)

## Objective
- `/mypage` 화면에서 좌측 카테고리(주제별/실전)와 히스토리 리스트를 분리하고, 아이템 클릭 시 같은 페이지 내 연습 상세 화면으로 전환되도록 한다.

## Scope
- In (MVP):
  - TopNav 사용자 이름 클릭 시 드롭다운 → 마이페이지 이동
  - `/mypage` 페이지 생성
  - 더미 히스토리 데이터로 UI 구성
  - 좌측 카테고리 리스트(주제별/실전) 및 기본 선택(주제별)
  - 히스토리 클릭 시 같은 페이지 내 연습 상세 화면 표시(오디오/스크립트/수정은 placeholder)
  - 상세 화면에서 히스토리 리스트로 돌아가는 동선 제공
- Out (v2+):
  - 실제 데이터 연동(백엔드)
  - 오디오 재생, 스크립트 보기/수정 기능
  - 히스토리 필터/검색

## API Contracts (Frozen for MVP)
- 없음 (UI-only, 더미 데이터)

## Requirements (Acceptance Criteria)
- [ ] TopNav 사용자 이름 클릭 → 드롭다운에 마이페이지 링크 표시
- [ ] `/mypage` 좌측 카테고리(주제별/실전) 리스트 표시, 기본은 주제별 선택
- [ ] 선택된 카테고리에 해당하는 히스토리 리스트 표시
- [ ] 히스토리 아이템 클릭 시 같은 페이지 내 연습 상세 화면 표시
- [ ] 상세 화면에서 리스트로 돌아가는 버튼 제공
- [ ] 분석 패널은 UI 구조만 제공(오디오/스크립트/수정 기능은 비활성 또는 placeholder)

## Scenarios (Fixed with Expected Outputs)
- S1: 로그인 상태에서 사용자 이름 클릭
  - Input: TopNav 사용자 이름 클릭
  - Expected: 드롭다운에 “마이페이지” 항목 표시, 클릭 시 `/mypage` 이동
- S2: `/mypage` 진입
  - Input: `/mypage` 접속
  - Expected: 좌측 카테고리 리스트 + 선택된 카테고리 히스토리 리스트 표시
- S3: 카테고리 전환
  - Input: 실전 연습하기 클릭
  - Expected: 실전 히스토리 리스트로 변경
- S4: 히스토리 선택
  - Input: 히스토리 아이템 클릭
  - Expected: 동일 페이지 내 연습 상세 화면 표시, 리스트는 유지

## Edge Cases (Deterministic Handling)
- E1: 히스토리 없음
  - Rule: “기록이 없습니다” 메시지 표시
- E2: 상세 화면 복귀
  - Rule: 뒤로가기/닫기 버튼으로 리스트 화면 복귀

## Design
- UI
  - TopNav 드롭다운 (사용자 이름 클릭)
  - `/mypage` 화면: 2~3단 레이아웃
    - 좌측: 카테고리 리스트(주제별/실전)
    - 중앙: 선택된 카테고리 히스토리 리스트
    - 우측: 선택된 히스토리 상세(연습 화면 스타일)
- Data model (UI only)
  - HistoryItem: id, type(topic|mock), title, date, duration, topic, summary

## Tasks
- [ ] 1. TopNav 드롭다운에 “마이페이지” 링크 추가
- [ ] 2. `/mypage` 페이지 및 기본 레이아웃 추가
- [ ] 3. 더미 히스토리 데이터 및 리스트/패널 전환 로직 구현
- [ ] 4. 상세 화면 및 복귀 버튼 placeholder 추가

## Verification (Definition of Done)
- Automated: N/A (UI-only)
- Manual: 시나리오 S1~S4 확인
- Documentation: `plan-mypage.md` 유지
