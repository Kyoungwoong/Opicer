import type {
  AnswerEvaluation,
  AnswerEvaluationStatus,
  CompetencyDimension,
  CorrectionCategory,
  OpicLevel,
  SessionEvaluationStatus,
  SessionReport,
  TargetLevel,
} from "@/features/practice/types";

export type SurveyItem = { id: string; label: string; topics: string[] };

/** `min` to `max` items may be chosen; `max` 1 means a single choice. */
export type SurveyPart = { code: string; title: string; min: number; max: number; items: SurveyItem[] };

export type SurveyCatalog = { parts: SurveyPart[]; part4MinTotal: number; cost: number };

/** Chosen item ids per part code. */
export type SurveySelections = Record<string, string[]>;

export type SelfAssessment = 1 | 2 | 3 | 4 | 5 | 6;

export type DifficultyAdjustment = "EASIER" | "SAME" | "HARDER";

export type MockItem = {
  id: string;
  order: number;
  part: number;
  questionText: string;
  questionType: string | null;
  promptAudioUrl: string | null;
};

export type StartMockTestInput = {
  survey: SurveySelections;
  selfAssessment: SelfAssessment;
  targetLevel: TargetLevel;
};

export type StartedMockTest = { id: string; items: MockItem[] };

export type MockAnswerInput = { itemId: string; transcript: string | null; durationSeconds: number | null };

export type MockSubmitResult = {
  mockTestId: string;
  alreadyCharged: boolean;
  deductedCredits: number;
  evaluationStatus: SessionEvaluationStatus | null;
};

/** A submitted test in the history list. */
export type MockTestSummary = {
  id: string;
  startedAt: string;
  submittedAt: string;
  selfAssessment: SelfAssessment;
  adjustment: DifficultyAdjustment | null;
  targetLevel: OpicLevel | null;
  itemCount: number;
  evaluationStatus: SessionEvaluationStatus | null;
  estimatedLevel: OpicLevel | null;
};

export type MockTestItemDetail = {
  id: string;
  order: number;
  part: number;
  questionText: string;
  questionType: string | null;
  topicTitle: string | null;
  /** STT output: an estimate, not ground truth. */
  transcript: string | null;
  durationSeconds: number | null;
  evaluationStatus: AnswerEvaluationStatus | null;
  overallLevel: OpicLevel | null;
  evaluation: AnswerEvaluation | null;
};

export type WordCount = { word: string; count: number };

/** Feedback beyond the report (schemaVersion 1), computed by the server from transcripts, durations and evaluations. */
export type MockTestInsights = {
  schemaVersion: number;
  questionTypes: { type: string; label: string; answers: number; level: OpicLevel; topIssue: CorrectionCategory | null }[];
  strongestTypes: string[];
  weakestTypes: string[];
  habits: {
    repeatedWords: WordCount[];
    connectors: WordCount[];
    dominantConnector: string | null;
    dominantConnectorPercent: number | null;
    averageSentenceLength: number;
    lexicalDiversity: number;
    notes: string[];
  };
  time: {
    usedSeconds: number;
    limitSeconds: number;
    part1Seconds: number;
    part2Seconds: number;
    items: { order: number; part: number; seconds: number | null }[];
    shortAnswers: number[];
    longAnswers: number[];
    unanswered: number[];
    notes: string[];
  };
  recommendations: { type: string; label: string; topics: { id: string; title: string }[] }[];
};

/** One evaluated test in the level trend, oldest first. */
export type MockTrendPoint = {
  id: string;
  submittedAt: string;
  estimatedLevel: OpicLevel;
  dimensionLevels: Partial<Record<CompetencyDimension, OpicLevel>>;
};

export type MockTestDetail = Omit<MockTestSummary, "itemCount"> & {
  status: "IN_PROGRESS" | "SUBMITTED";
  survey: SurveySelections | null;
  examSeconds: number | null;
  report: SessionReport | null;
  insights: MockTestInsights | null;
  evaluatedAt: string | null;
  items: MockTestItemDetail[];
  trend: MockTrendPoint[];
};
