import type { DifficultyAdjustment, SelfAssessment } from "@/features/mock/types";

/** The whole exam, both parts and the difficulty choice between them. */
export const EXAM_SECONDS = 40 * 60;
export const TOTAL_QUESTIONS = 15;
/** Each question may be heard at most twice: once on arrival and one replay. */
export const MAX_PLAYS = 2;
/** Answers being turned into text at the same time while the exam goes on. */
export const TRANSCRIBE_CONCURRENCY = 3;

export const PRACTICE_NOTICE = "연습용 모의 시험이에요. 결과는 예상 레벨(참고용)이며 공식 OPIc 점수가 아닙니다.";

/** Opicer's own wording for the six self-assessment steps. */
export const SELF_ASSESSMENT_OPTIONS: { value: SelfAssessment; description: string }[] = [
  { value: 1, description: "아는 단어나 짧은 표현 몇 개로 대답할 수 있어요." },
  { value: 2, description: "자기소개처럼 익숙한 내용을 짧은 문장으로 말할 수 있어요." },
  { value: 3, description: "일상적인 주제를 간단한 문장 몇 개로 이어서 말할 수 있어요." },
  { value: 4, description: "익숙한 주제라면 이유와 예시를 붙여 1~2분 동안 말할 수 있어요." },
  { value: 5, description: "경험을 자세히 풀고, 예전과 지금을 비교하며 자연스럽게 말할 수 있어요." },
  { value: 6, description: "낯선 주제나 사회 문제도 내 의견을 논리적으로 길게 말할 수 있어요." },
];

/** Same wording as the server's insights. */
export const QUESTION_TYPE_LABELS: Record<string, string> = {
  DESCRIPTION: "묘사·일상",
  PAST_EXPERIENCE: "과거 경험",
  COMPARE_CONTRAST: "비교·변화",
  OPINION: "의견",
  ROLE_PLAY: "롤플레이",
  UNEXPECTED_SITUATION: "문제 해결",
};

export const DIFFICULTY_OPTIONS: { value: DifficultyAdjustment; label: string; description: string }[] = [
  { value: "EASIER", label: "더 쉽게", description: "남은 질문을 한 단계 쉽게 받아요." },
  { value: "SAME", label: "비슷하게", description: "지금과 비슷한 난이도로 이어 가요." },
  { value: "HARDER", label: "더 어렵게", description: "남은 질문을 한 단계 어렵게 받아요." },
];

export function formatClock(seconds: number): string {
  const safe = Math.max(0, seconds);
  return `${Math.floor(safe / 60).toString().padStart(2, "0")}:${(safe % 60).toString().padStart(2, "0")}`;
}

/** Runs at most `limit` tasks at once, in the order they were queued. */
export function createLimiter(limit: number) {
  let running = 0;
  const waiting: Array<() => void> = [];
  const release = () => {
    running -= 1;
    waiting.shift()?.();
  };
  return function run<T>(task: () => Promise<T>): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      const start = () => {
        running += 1;
        task().then(resolve, reject).finally(release);
      };
      if (running < limit) start();
      else waiting.push(start);
    });
  };
}
