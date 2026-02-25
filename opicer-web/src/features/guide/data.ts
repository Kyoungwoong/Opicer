import type {
  GuideSection,
  OpicCalendar,
  OpicScheduleItem,
  ReviewItem,
  TipItem,
} from "@/features/guide/types";

export const GUIDE_SECTIONS: GuideSection[] = [
  {
    id: "intro",
    title: "오픽 설명",
    summary: "OPIC 시험의 구조와 레벨 기준을 빠르게 이해하세요.",
  },
  {
    id: "schedule",
    title: "오픽 일정",
    summary: "정기 시험과 접수 일정 요약 (연동 준비중).",
  },
  {
    id: "reviews",
    title: "오픽 강의실 별 지원자들 후기",
    summary: "시험장 분위기와 좌석 경험을 미리 확인하세요.",
  },
  {
    id: "tips",
    title: "오픽 보러 갈 때의 꿀팁",
    summary: "준비물, 시간 관리, 멘탈 케어까지 체크리스트.",
  },
];

export const OPIC_SCHEDULE: OpicScheduleItem[] = [
  {
    id: "s1",
    dateLabel: "3월 7일",
    title: "접수 시작",
    note: "오전 10시부터 접수 오픈",
  },
  {
    id: "s2",
    dateLabel: "3월 21일",
    title: "정기 시험",
    note: "오후 2시 시험 회차",
  },
];

export const OPIC_CALENDAR: OpicCalendar = {
  monthLabel: "2026년 3월",
  startWeekday: 0,
  daysInMonth: 31,
  events: [
    { id: "c1", date: 7, label: "접수 시작", type: "registration" },
    { id: "c2", date: 21, label: "정기 시험", type: "exam" },
  ],
};

export const REVIEW_ITEMS: ReviewItem[] = [
  {
    id: "r1",
    classroom: "강남센터 3층",
    quote: "조명이 밝고 마이크 음질이 깔끔했어요.",
    rating: 5,
  },
  {
    id: "r2",
    classroom: "종로센터 5층",
    quote: "좌석 간 간격이 넉넉해서 집중하기 좋았습니다.",
    rating: 4,
  },
  {
    id: "r3",
    classroom: "부산센터 2층",
    quote: "에어컨 소리가 조금 있었지만 전반적으로 조용했어요.",
    rating: 4,
  },
];

export const TIP_ITEMS: TipItem[] = [
  {
    id: "t1",
    title: "시험 전 10분 워밍업",
    detail: "간단한 주제에 대해 1분씩 말하며 입을 풀어보세요.",
  },
  {
    id: "t2",
    title: "답변 구조를 미리 정리",
    detail: "인트로–경험–결론 3단계를 의식하면 안정적입니다.",
  },
  {
    id: "t3",
    title: "시간 압박에 대비",
    detail: "질문을 듣자마자 첫 문장을 바로 말할 수 있게 준비하세요.",
  },
  {
    id: "t4",
    title: "컨디션 관리",
    detail: "시험 전날 수면을 충분히 확보하고 카페인은 과하게 마시지 마세요.",
  },
];
