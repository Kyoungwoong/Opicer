import type {
  CompetencyDimension,
  CorrectionCategory,
  OpicLevel,
  SessionReport,
  TargetLevel,
} from "@/features/practice/types";

/** Shown next to every estimated level; mirrors the server's SessionReport.DISCLAIMER. */
export const LEVEL_DISCLAIMER = "예상 레벨(참고용) · 공식 OPIc 점수가 아닙니다";

export const LEVEL_ORDER: OpicLevel[] = ["NL", "IL", "IM", "IH", "AL"];

export const TARGET_LEVELS: { level: TargetLevel; description: string }[] = [
  { level: "IM", description: "익숙한 주제를 문장으로 이어 말하기" },
  { level: "IH", description: "경험을 시간 순서·비교로 풀어 말하기" },
  { level: "AL", description: "어떤 주제든 문단으로 자연스럽게" },
];

export const DEFAULT_TARGET_LEVEL: TargetLevel = "IM";

export function isTargetLevel(value: unknown): value is TargetLevel {
  return TARGET_LEVELS.some((option) => option.level === value);
}

/** 1 (NL) to 5 (AL), for bar widths. */
export function levelRank(level: OpicLevel): number {
  return LEVEL_ORDER.indexOf(level) + 1;
}

export const DIMENSION_LABELS: Record<CompetencyDimension, string> = {
  TASK: "과제 수행",
  DISCOURSE: "담화 구성",
  ACCURACY: "문법·시제",
  VOCABULARY: "어휘·표현",
};

export const DIMENSION_ORDER: CompetencyDimension[] = ["TASK", "DISCOURSE", "ACCURACY", "VOCABULARY"];

export const CATEGORY_LABELS: Record<CorrectionCategory, string> = {
  TENSE: "시제",
  AGREEMENT: "수 일치",
  ARTICLE: "관사",
  PREPOSITION: "전치사",
  OTHER_GRAMMAR: "기타 문법",
  WORD_CHOICE: "단어 선택",
  NATURALNESS: "자연스러움",
  CONNECTOR: "연결어",
  ORGANIZATION: "구성",
};

export const CONFIDENCE_LABELS: Record<SessionReport["confidence"], string> = {
  HIGH: "신뢰도 높음",
  MEDIUM: "신뢰도 보통",
  LOW: "신뢰도 낮음",
};

export function gapLabel(gapToTarget: number, targetLevel: OpicLevel): string {
  if (gapToTarget <= 0) return `목표 ${targetLevel} 도달`;
  return `목표 ${targetLevel}까지 ${gapToTarget}단계`;
}
