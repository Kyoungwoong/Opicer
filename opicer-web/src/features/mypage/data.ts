import type { PracticeHistory } from "@/features/mypage/types";

export const PRACTICE_HISTORY: PracticeHistory[] = [
  {
    id: "topic-1",
    type: "topic",
    title: "자기소개 & 일상 루틴",
    topicLabel: "일상/자기소개",
    date: "2026-02-24 21:40",
    durationMinutes: 18,
    summary: "가벼운 워밍업 질문 중심으로 답변 구조 점검",
    answers: [
      { id: "a1", question: "Tell me about yourself." },
      { id: "a2", question: "Describe a typical weekday for you." },
      { id: "a3", question: "What do you do after work or school?" },
    ],
  },
  {
    id: "topic-2",
    type: "topic",
    title: "여행 경험",
    topicLabel: "여행",
    date: "2026-02-23 22:10",
    durationMinutes: 22,
    summary: "경험 묘사, 감정 표현, 비교 표현 연습",
    answers: [
      { id: "b1", question: "Talk about your most memorable trip." },
      { id: "b2", question: "Describe the place you visited." },
      { id: "b3", question: "Would you visit again? Why?" },
    ],
  },
  {
    id: "topic-3",
    type: "topic",
    title: "취미/여가 활동",
    topicLabel: "취미",
    date: "2026-02-21 20:35",
    durationMinutes: 20,
    summary: "선호도 비교와 이유 설명 흐름 점검",
    answers: [
      { id: "c1", question: "What do you like to do in your free time?" },
      { id: "c2", question: "How did you start this hobby?" },
      { id: "c3", question: "Tell me about a recent hobby experience." },
    ],
  },
  {
    id: "mock-1",
    type: "mock",
    title: "실전 연습 #01",
    topicLabel: "혼합 주제",
    date: "2026-02-24 23:05",
    durationMinutes: 40,
    summary: "총 12문항, 시간 압박 구간 대응 점검",
    answers: [
      { id: "m1", question: "Tell me about your neighborhood." },
      { id: "m2", question: "Describe a memorable restaurant visit." },
      { id: "m3", question: "Explain a recent problem you solved." },
    ],
  },
  {
    id: "mock-2",
    type: "mock",
    title: "실전 연습 #02",
    topicLabel: "혼합 주제",
    date: "2026-02-19 21:15",
    durationMinutes: 38,
    summary: "스토리텔링 연결성 개선 목표",
    answers: [
      { id: "m4", question: "Describe a hobby you tried recently." },
      { id: "m5", question: "Talk about a trip you took with friends." },
      { id: "m6", question: "Tell me about a daily routine you enjoy." },
    ],
  },
];
