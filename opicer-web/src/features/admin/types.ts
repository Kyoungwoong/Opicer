export type OpicLevel = "NL" | "IL" | "IM" | "IH" | "AL";
export type QuestionType =
  | "DESCRIPTION"
  | "PAST_EXPERIENCE"
  | "OPINION"
  | "COMPARE_CONTRAST"
  | "ROLE_PLAY"
  | "UNEXPECTED_SITUATION";
export type PromptUseCase =
  | "TRANSCRIPT_CLEANING"
  | "FEEDBACK"
  | "SCRIPT_IMPROVEMENT";
export type UniversalSentenceType =
  | "OPINION"
  | "PAST_EXPERIENCE"
  | "COMPARE_CONTRAST"
  | "UNEXPECTED_SITUATION";

/** A practice topic; questions belong to it by `title`. */
export type AdminTopic = {
  id: string;
  title: string;
  englishTitle: string;
  category: string;
  categoryOrder: number;
  topicOrder: number;
  badges: { label: string; count: number | null }[];
  active: boolean;
  /** The mock test finds this topic by title (survey item or self-introduction), so it cannot be renamed. */
  mockLinked: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AdminTopicInput = Omit<AdminTopic, "id" | "mockLinked" | "createdAt" | "updatedAt">;

export type Question = {
  id: string;
  topic: string;
  type: QuestionType;
  promptText: string;
  promptAudioUrl: string | null;
  structuralHint: string | null;
  targetLevels: OpicLevel[];
  keyExpressions: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type DailySentence = {
  id: string;
  date: string;
  text: string;
  level: OpicLevel;
  audioUrl: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type PromptVersion = {
  id: string;
  useCase: PromptUseCase;
  version: number;
  name: string;
  template: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type EvaluationGuideKind = "SCORING_NOTES" | "TIPS" | "EXPRESSIONS" | "REPORT";

export type EvaluationGuide = {
  id: string;
  kind: EvaluationGuideKind;
  kindLabel: string;
  version: number;
  title: string;
  contentMd: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type UniversalSentence = {
  id: string;
  type: UniversalSentenceType;
  title: string;
  sentence: string;
  tags: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type GoodAnswerSample = {
  id: string;
  topicId: string;
  level: OpicLevel;
  sampleText: string;
  sampleAudioUrl: string | null;
  summary: string | null;
  tags: string[];
  keyExpressions: string[];
  createdAt: string;
};

export type GoodAnswerUploadItem = {
  id: string;
  createdAt: string;
  updatedAt: string;
  audioUrl: string;
  sampleText: string;
};

export type AdminOpicCenter = {
  id: string;
  name: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  phone: string | null;
  subwayLines: string | null;
  directions: string | null;
  regionName: string | null;
  active: boolean;
  updatedAt: string;
  /** Set when the center was saved without coordinates. */
  geocodeWarning: string | null;
};

/** Leave both coordinates empty to look them up from the address. */
export type AdminOpicCenterInput = {
  name: string;
  address: string;
  phone?: string;
  subwayLines?: string;
  directions?: string;
  regionName?: string;
  latitude?: number;
  longitude?: number;
};

export type AdminOpicCenterReview = {
  id: string;
  centerId: string;
  rating: number;
  content: string;
  sourceLabel: string | null;
  visible: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AdminOpicCenterReviewInput = {
  rating: number;
  content: string;
  sourceLabel?: string;
  visible: boolean;
};
