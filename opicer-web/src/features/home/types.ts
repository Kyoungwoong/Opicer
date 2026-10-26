export type UniversalSentence = {
  id: string;
  type: "OPINION" | "PAST_EXPERIENCE" | "COMPARE_CONTRAST" | "UNEXPECTED_SITUATION";
  title: string;
  sentence: string;
  tags: string[];
};
