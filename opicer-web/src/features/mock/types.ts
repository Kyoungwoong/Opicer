import type { SessionEvaluationStatus, TargetLevel } from "@/features/practice/types";

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
