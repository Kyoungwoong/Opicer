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

export type OpicLevel = "NL" | "IL" | "IM" | "IH" | "AL";

/** Levels a learner can aim for; the server rejects NL and IL. */
export type TargetLevel = Extract<OpicLevel, "IM" | "IH" | "AL">;

/** Null on sessions recorded before automatic evaluation, or with nothing to evaluate. */
export type SessionEvaluationStatus = "PENDING" | "RUNNING" | "DONE" | "PARTIAL" | "FAILED";

export type AnswerEvaluationStatus = "PENDING" | "DONE" | "FAILED" | "SKIPPED";

export type CompetencyDimension = "TASK" | "DISCOURSE" | "ACCURACY" | "VOCABULARY";

export type CorrectionCategory =
  | "TENSE"
  | "AGREEMENT"
  | "ARTICLE"
  | "PREPOSITION"
  | "OTHER_GRAMMAR"
  | "WORD_CHOICE"
  | "NATURALNESS"
  | "CONNECTOR"
  | "ORGANIZATION";

/** Session report (schemaVersion 1). Every level is a practice estimate, never an official OPIc score. */
export type SessionReport = {
  estimatedLevel: OpicLevel;
  lowestLevel: OpicLevel;
  highestLevel: OpicLevel;
  confidence: "HIGH" | "MEDIUM" | "LOW";
  evaluatedAnswers: number;
  dimensionLevels: Partial<Record<CompetencyDimension, OpicLevel>>;
  targetLevel: OpicLevel;
  /** Target minus estimate in level steps; positive means below the target. */
  gapToTarget: number;
  topIssues: {
    category: CorrectionCategory;
    answerCount: number;
    totalCount: number;
    example: { category: CorrectionCategory; original: string; corrected: string } | null;
  }[];
  tips: string[];
  skippedAnswers: number;
  failedAnswers: number;
  disclaimer: string;
  schemaVersion: number;
};

/**
 * Per-answer evaluation (schemaVersion 1). COACH documents (topic practice) have no level, ratings or strengths;
 * documents written before modes existed have no `mode` and are FULL.
 */
export type AnswerEvaluation = {
  mode?: "FULL" | "COACH";
  overallLevel?: OpicLevel;
  /** Reference numbers measured from the STT transcript, not a score. */
  signals: {
    wordCount: number;
    sentenceCount: number;
    averageSentenceLength: number;
    wordsPerMinute: number | null;
    lexicalDiversity: number;
    fillerCount: number;
  };
  dimensions?: { code: CompetencyDimension; level: OpicLevel; evidence: string; commentKo: string }[];
  strengths?: string[];
  corrections: { original: string; corrected: string; reasonKo: string; category: CorrectionCategory }[];
  targetScript: string;
  keyExpressions: { expression: string; meaningKo: string }[];
};

export type PracticeSubmitResult = {
  chargeId: string;
  alreadyCharged: boolean;
  deductedCredits: number;
  sessionId: string;
  evaluationStatus: SessionEvaluationStatus | null;
};

/** Sent with submit so the session's answers are stored server-side (no audio). */
export type PracticeAnswerInput = {
  questionId: string;
  questionText: string;
  transcript?: string | null;
  /** Recording length, 1–600 seconds; omitted when unknown. */
  durationSeconds?: number | null;
};

export type PracticeSessionSummary = {
  id: string;
  topicId: string;
  topicTitle: string;
  startedAt: string;
  completedAt: string;
  answerCount: number;
  analyzedCount: number;
  evaluationStatus: SessionEvaluationStatus | null;
  estimatedLevel: OpicLevel | null;
};

export type PracticeSessionAnswer = {
  questionId: string;
  questionOrder: number;
  questionText: string;
  /** STT output: an estimate, not ground truth. */
  transcript: string | null;
  analysis: string | null;
  improvedScript: string | null;
  durationSeconds: number | null;
  evaluationStatus: AnswerEvaluationStatus | null;
  overallLevel: OpicLevel | null;
  evaluation: AnswerEvaluation | null;
};

export type PracticeSessionDetail = Omit<PracticeSessionSummary, "answerCount" | "analyzedCount"> & {
  targetLevel: OpicLevel | null;
  report: SessionReport | null;
  evaluatedAt: string | null;
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
  durationSeconds?: number;
};
