export type TopicBadge = {
  label: string;
  count?: number | null;
};

export type TopicItem = {
  id: string;
  title: string;
  englishTitle: string;
  category: string;
  categoryOrder: number;
  topicOrder: number;
  badges: TopicBadge[];
  active: boolean;
};

export type TopicCategory = {
  id: string;
  label: string;
  description: string;
  topics: TopicItem[];
};

export type TopicSelection = {
  id: string;
  topicId: string;
  selectedAt: string;
};

export type PracticeSubmitResult = {
  chargeId: string;
  alreadyCharged: boolean;
  deductedCredits: number;
  sessionId: string;
};

/** Sent with submit so the session's answers are stored server-side (no audio). */
export type PracticeAnswerInput = {
  questionId: string;
  questionText: string;
  transcript?: string | null;
};

export type PracticeSessionSummary = {
  id: string;
  topicId: string;
  topicTitle: string;
  startedAt: string;
  completedAt: string;
  answerCount: number;
  analyzedCount: number;
};

export type PracticeSessionAnswer = {
  questionId: string;
  questionOrder: number;
  questionText: string;
  /** STT output: an estimate, not ground truth. */
  transcript: string | null;
  analysis: string | null;
  improvedScript: string | null;
};

export type PracticeSessionDetail = Omit<PracticeSessionSummary, "answerCount" | "analyzedCount"> & {
  answers: PracticeSessionAnswer[];
};

export type PracticeQuestion = {
  id: string;
  topic: string;
  type: string;
  promptText: string;
  promptAudioUrl?: string | null;
  structuralHint?: string | null;
  targetLevels?: string[];
  keyExpressions?: string[];
};

export type PracticeAnswer = {
  questionId: string;
  questionText: string;
  audioUrl?: string;
  recordedAt?: string;
  transcript?: string;
  transcribeError?: string;
};

export type AnalysisResult = {
  analysis: string;
};

export type ImprovementResult = {
  improved: string;
};
