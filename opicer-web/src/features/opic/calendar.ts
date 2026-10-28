/** Calendar math on YYYY-MM-DD / YYYY-MM strings, anchored to Asia/Seoul so SSR and browsers agree. */

const SEOUL_YMD = new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

/** Today in Asia/Seoul as YYYY-MM-DD. */
export function seoulToday(now: Date = new Date()): string {
  return SEOUL_YMD.format(now);
}

export function monthOf(isoDate: string): string {
  return isoDate.slice(0, 7);
}

export function shiftMonth(month: string, delta: number): string {
  const [y, m] = month.split("-").map(Number);
  const d = new Date(Date.UTC(y, m - 1 + delta, 1));
  return `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}`;
}

export type MonthGrid = {
  /** 0 = Sunday */
  startWeekday: number;
  /** YYYY-MM-DD for every day of the month */
  days: string[];
};

export function monthGrid(month: string): MonthGrid {
  const [y, m] = month.split("-").map(Number);
  const startWeekday = new Date(Date.UTC(y, m - 1, 1)).getUTCDay();
  const daysInMonth = new Date(Date.UTC(y, m, 0)).getUTCDate();
  const days = Array.from({ length: daysInMonth }, (_, i) => `${month}-${String(i + 1).padStart(2, "0")}`);
  return { startWeekday, days };
}

/** "2026년 11월" */
export function monthLabel(month: string): string {
  const [y, m] = month.split("-").map(Number);
  return `${y}년 ${m}월`;
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

/** "11.3(화)" */
export function shortDateLabel(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  return `${m}.${d}(${WEEKDAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]})`;
}

/** "11월 3일 화요일" */
export function longDateLabel(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  return `${m}월 ${d}일 ${WEEKDAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]}요일`;
}

export const WEEKDAY_LABELS = WEEKDAYS;
