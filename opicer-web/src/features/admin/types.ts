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
