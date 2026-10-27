import type { AdminTopic, Question, QuestionType } from "./types";

/** Must match `Topic.CATEGORIES` on the server. */
export const TOPIC_CATEGORIES = ["자기소개", "설문조사", "돌발질문"] as const;
export type TopicCategory = (typeof TOPIC_CATEGORIES)[number];

/** Mock-test sets that live only as question topics, never as practice topics (`MockTestComposer`). */
export const ROLE_PLAY_PREFIX = "롤플레이:";
export const ADVANCED_PREFIX = "고난도:";
/** The topic the mock test's first question always comes from. */
export const SELF_INTRO_TOPIC = "자기소개";

export const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  DESCRIPTION: "묘사",
  PAST_EXPERIENCE: "과거 경험",
  COMPARE_CONTRAST: "비교",
  OPINION: "의견",
  ROLE_PLAY: "롤플레이",
  UNEXPECTED_SITUATION: "문제 해결",
};

export type TopicGroup = "topic" | "roleplay" | "advanced" | "unlinked";

/** Which kind of place a question's topic points to; "unlinked" questions are never asked anywhere. */
export function topicGroup(topic: string, topicTitles: Set<string>): TopicGroup {
  if (topic.startsWith(ROLE_PLAY_PREFIX)) return "roleplay";
  if (topic.startsWith(ADVANCED_PREFIX)) return "advanced";
  return topicTitles.has(topic) ? "topic" : "unlinked";
}

export function setName(topic: string, prefix: string): string {
  return topic.slice(prefix.length).trim();
}

/** Active question count per type for one topic title (or set name). */
export function typeCounts(questions: Question[], topic: string): Partial<Record<QuestionType, number>> {
  const counts: Partial<Record<QuestionType, number>> = {};
  for (const q of questions) {
    if (q.active && q.topic === topic) counts[q.type] = (counts[q.type] ?? 0) + 1;
  }
  return counts;
}

export type Readiness = { tone: "ok" | "warn" | "off"; text: string };

/**
 * How the mock test can use a topic, following `MockTestComposer`: a survey or unexpected topic needs two unused
 * questions for a combo and asks describe → experience → compare when it has them; the first question comes only
 * from the topic titled 자기소개.
 */
export function readiness(topic: AdminTopic, counts: Partial<Record<QuestionType, number>>): Readiness {
  const total = Object.values(counts).reduce((sum, n) => sum + (n ?? 0), 0);
  if (!topic.active) return { tone: "off", text: "비활성: 연습 목록과 실전에 나오지 않아요" };
  if (topic.category === "자기소개") {
    if (topic.title !== SELF_INTRO_TOPIC) return { tone: "warn", text: `실전 1번 문항은 '${SELF_INTRO_TOPIC}' 주제에서만 나와요` };
    return counts.DESCRIPTION ? { tone: "ok", text: "실전 1번 문항으로 쓰여요" } : { tone: "warn", text: "묘사 유형 문항이 있어야 실전 1번에 좋아요" };
  }
  if (total === 0) return { tone: "warn", text: "활성 문항이 없어 연습할 수 없어요" };
  if (total < 2) return { tone: "warn", text: "실전에 쓰려면 활성 문항이 2개 이상 필요해요" };
  if (counts.DESCRIPTION && counts.PAST_EXPERIENCE && counts.COMPARE_CONTRAST) {
    return { tone: "ok", text: "실전 콤보 준비 완료(묘사·과거 경험·비교)" };
  }
  return { tone: "ok", text: "실전에 쓰여요. 묘사·과거 경험·비교를 모두 갖추면 더 좋아요" };
}
