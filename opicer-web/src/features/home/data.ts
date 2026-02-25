import type {
  OpicCalendar,
  RecentActivity,
  UniversalSentence,
} from "@/features/home/types";

export const UNIVERSAL_SENTENCES: UniversalSentence[] = [
  {
    id: "u1",
    title: "의견 시작 템플릿",
    sentence: "From my perspective, this topic is quite important because...",
    tags: ["opinion", "starter"],
    situation: "Opinion",
  },
  {
    id: "u2",
    title: "경험 공유 템플릿",
    sentence: "I still remember the time when I had to deal with...",
    tags: ["experience", "past"],
    situation: "Past Experience",
  },
  {
    id: "u3",
    title: "비교/대조 템플릿",
    sentence: "Compared to the past, things have become much more...",
    tags: ["compare", "contrast"],
    situation: "Compare/Contrast",
  },
  {
    id: "u4",
    title: "문제 해결 템플릿",
    sentence: "In that situation, the first thing I did was to...",
    tags: ["solution", "structure"],
    situation: "Unexpected Situation",
  },
];

export const RECENT_ACTIVITIES: RecentActivity[] = [
  {
    id: "a1",
    title: "Mock Test #12",
    summary: "IM 레벨 타깃 답변 구조 점검",
    dateLabel: "오늘",
    status: "complete",
  },
  {
    id: "a2",
    title: "주제별 연습: Travel",
    summary: "스토리텔링 흐름 개선 필요",
    dateLabel: "어제",
    status: "in_progress",
  },
  {
    id: "a3",
    title: "추천 연습: Role-play",
    summary: "대화형 답변 템플릿 복습",
    dateLabel: "추천",
    status: "recommended",
  },
];

export const OPIC_CALENDAR: OpicCalendar = {
  monthLabel: "2026년 3월",
  startWeekday: 0,
  daysInMonth: 31,
  events: [
    { id: "e1", date: 7, label: "시험 접수 시작", type: "registration" },
    { id: "e2", date: 21, label: "정기 시험", type: "exam" },
  ],
};
