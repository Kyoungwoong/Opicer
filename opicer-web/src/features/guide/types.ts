export type GuideSection = {
  id: string;
  title: string;
  summary: string;
};

export type TipItem = {
  id: string;
  title: string;
  detail: string;
};

export type ExamStep = {
  id: string;
  /** Short label for the step button */
  label: string;
  title: string;
  duration: string;
  description: string;
  tip: string;
};

export type LevelGuide = {
  id: "AL" | "IH" | "IM" | "IL";
  label: string;
  summary: string;
  structure: string;
  tense: string;
  length: string;
  example: string;
  nextStep: string;
};
