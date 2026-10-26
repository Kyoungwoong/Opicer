import type { PracticeSessionSummary } from "@/features/practice/types";

const SEOUL_DATE = new Intl.DateTimeFormat("ko-KR", { timeZone: "Asia/Seoul", month: "numeric", day: "numeric" });
const SEOUL_DATETIME = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

/** "10.26" */
export function formatShortDate(iso: string): string {
  const parts = SEOUL_DATE.formatToParts(new Date(iso));
  const month = parts.find((p) => p.type === "month")?.value ?? "";
  const day = parts.find((p) => p.type === "day")?.value ?? "";
  return `${month}.${day}`;
}

/** "2026년 10월 26일 오후 03:12" */
export function formatDateTime(iso: string): string {
  return SEOUL_DATETIME.format(new Date(iso));
}

export type HistoryStatus = { label: string; tone: "done" | "partial" | "none" };

export function historyStatus(session: Pick<PracticeSessionSummary, "answerCount" | "analyzedCount">): HistoryStatus {
  if (session.answerCount > 0 && session.analyzedCount >= session.answerCount) {
    return { label: "분석 완료", tone: "done" };
  }
  if (session.analyzedCount > 0) {
    return { label: "일부 분석", tone: "partial" };
  }
  return { label: "분석 전", tone: "none" };
}

export const HISTORY_STATUS_DOT: Record<HistoryStatus["tone"], string> = {
  done: "bg-primary",
  partial: "bg-highlight",
  none: "bg-muted-foreground/40",
};

export function historyPath(sessionId: string): string {
  return `/mypage/history/${encodeURIComponent(sessionId)}`;
}
