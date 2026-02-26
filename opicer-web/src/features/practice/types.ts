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
