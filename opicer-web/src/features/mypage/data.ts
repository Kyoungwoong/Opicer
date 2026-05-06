import type { PracticeHistory } from "@/features/mypage/types";

export const PRACTICE_HISTORY: PracticeHistory[] = [
  {
    id: "topic-1",
    type: "topic",
    title: "Self Introduction & text text",
    topicLabel: "text/Self Introduction",
    date: "2026-02-24 21:40",
    durationMinutes: 18,
    summary: "text text Question text text text text",
    answers: [
      { id: "a1", question: "Tell me about yourself." },
      { id: "a2", question: "Describe a typical weekday for you." },
      { id: "a3", question: "What do you do after work or school?" },
    ],
  },
  {
    id: "topic-2",
    type: "topic",
    title: "text text",
    topicLabel: "text",
    date: "2026-02-23 22:10",
    durationMinutes: 22,
    summary: "text text, text text, text text text",
    answers: [
      { id: "b1", question: "Talk about your most memorable trip." },
      { id: "b2", question: "Describe the place you visited." },
      { id: "b3", question: "Would you visit again? Why?" },
    ],
  },
  {
    id: "topic-3",
    type: "topic",
    title: "text/text text",
    topicLabel: "text",
    date: "2026-02-21 20:35",
    durationMinutes: 20,
    summary: "text text text text text text",
    answers: [
      { id: "c1", question: "What do you like to do in your free time?" },
      { id: "c2", question: "How did you start this hobby?" },
      { id: "c3", question: "Tell me about a recent hobby experience." },
    ],
  },
  {
    id: "mock-1",
    type: "mock",
    title: "text text #01",
    topicLabel: "text Topic",
    date: "2026-02-24 23:05",
    durationMinutes: 40,
    summary: "text 12text, text text text text text",
    answers: [
      { id: "m1", question: "Tell me about your neighborhood." },
      { id: "m2", question: "Describe a memorable restaurant visit." },
      { id: "m3", question: "Explain a recent problem you solved." },
    ],
  },
  {
    id: "mock-2",
    type: "mock",
    title: "text text #02",
    topicLabel: "text Topic",
    date: "2026-02-19 21:15",
    durationMinutes: 38,
    summary: "text text itemstext text",
    answers: [
      { id: "m4", question: "Describe a hobby you tried recently." },
      { id: "m5", question: "Talk about a trip you took with friends." },
      { id: "m6", question: "Tell me about a daily routine you enjoy." },
    ],
  },
];
