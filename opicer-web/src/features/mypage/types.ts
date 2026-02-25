export type PracticeHistoryType = "topic" | "mock";

export type PracticeAnswer = {
  id: string;
  question: string;
  audioUrl?: string;
};

export type PracticeHistory = {
  id: string;
  type: PracticeHistoryType;
  title: string;
  topicLabel: string;
  date: string;
  durationMinutes: number;
  summary: string;
  answers: PracticeAnswer[];
};
