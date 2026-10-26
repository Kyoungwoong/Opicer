"use client";

import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, ExternalLink } from "lucide-react";
import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchOpicSchedules } from "@/features/opic/api";
import type { OpicSchedule } from "@/features/opic/types";
import {
  WEEKDAY_LABELS,
  longDateLabel,
  monthGrid,
  monthLabel,
  monthOf,
  seoulToday,
  shiftMonth,
  shortDateLabel,
} from "@/features/opic/calendar";

export const OPIC_REGISTER_URL = "https://www.opic.or.kr/opics/jsp/view/index.jsp";

/** Synced data covers the current month plus two ahead (opicer.opic-sync.months-ahead). */
const MONTHS_AHEAD = 2;

type Props = {
  /** "compact" for the home card, "comfortable" for the guide page. */
  size?: "compact" | "comfortable";
  className?: string;
};

type MonthState =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; schedules: OpicSchedule[] };

/** "261101 정기 평가" → "정기 평가" */
function roundLabel(roundName: string): string {
  return roundName.replace(/^\d{6}\s*/, "") || roundName;
}

function registrationStatus(schedule: OpicSchedule, today: string): { label: string; open: boolean } {
  const { registrationStart: start, registrationEnd: end } = schedule;
  if (start && today < start) return { label: `접수 예정 · ${shortDateLabel(start)}부터`, open: false };
  if (end && today <= end) return { label: `접수 중 · ${shortDateLabel(end)} 마감`, open: true };
  return { label: "접수 마감", open: false };
}

function defaultSelection(schedules: OpicSchedule[], today: string): string | null {
  const dates = schedules.map((s) => s.examDate).sort();
  return dates.find((d) => d >= today) ?? dates[0] ?? null;
}

export function OpicCalendar({ size = "compact", className }: Props) {
  const today = useMemo(() => seoulToday(), []);
  const firstMonth = monthOf(today);
  const lastMonth = shiftMonth(firstMonth, MONTHS_AHEAD);

  const [month, setMonth] = useState(firstMonth);
  // Results are tagged with their month, so switching months reads as "loading" without resetting state in the effect.
  const [result, setResult] = useState<{ month: string; state: MonthState } | null>(null);
  const [picked, setPicked] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    fetchOpicSchedules(month)
      .then((data) => alive && setResult({ month, state: { status: "ready", schedules: data.schedules } }))
      .catch(() => alive && setResult({ month, state: { status: "error" } }));
    return () => {
      alive = false;
    };
  }, [month]);

  const state: MonthState = result?.month === month ? result.state : { status: "loading" };
  const selected =
    picked && monthOf(picked) === month
      ? picked
      : state.status === "ready"
        ? defaultSelection(state.schedules, today)
        : null;

  const schedules = state.status === "ready" ? state.schedules : null;
  const examsByDate = useMemo(() => {
    const map = new Map<string, OpicSchedule[]>();
    for (const schedule of schedules ?? []) {
      map.set(schedule.examDate, [...(map.get(schedule.examDate) ?? []), schedule]);
    }
    return map;
  }, [schedules]);

  const grid = monthGrid(month);
  const cellHeight = size === "compact" ? "h-9" : "h-11";
  const selectedExams = selected ? examsByDate.get(selected) ?? [] : [];

  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <div className="flex items-center justify-between gap-2">
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="rounded-full"
          aria-label="이전 달"
          disabled={month <= firstMonth}
          onClick={() => setMonth((m) => shiftMonth(m, -1))}
        >
          <ChevronLeft />
        </Button>
        <p className="text-base font-semibold text-primary-strong" aria-live="polite">
          {monthLabel(month)}
        </p>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="rounded-full"
          aria-label="다음 달"
          disabled={month >= lastMonth}
          onClick={() => setMonth((m) => shiftMonth(m, 1))}
        >
          <ChevronRight />
        </Button>
      </div>

      <div>
        <div className="grid grid-cols-7 gap-1.5 text-center text-xs font-medium text-muted-foreground">
          {WEEKDAY_LABELS.map((label) => (
            <span key={label}>{label}</span>
          ))}
        </div>
        {state.status === "loading" ? (
          <Skeleton className={cn("mt-2 rounded-xl", size === "compact" ? "h-56" : "h-64")} />
        ) : (
          <div className="mt-2 grid grid-cols-7 gap-1.5">
            {Array.from({ length: grid.startWeekday }, (_, i) => (
              <div key={`empty-${i}`} className={cellHeight} />
            ))}
            {grid.days.map((date) => {
              const exams = examsByDate.get(date);
              const day = Number(date.slice(8));
              const isToday = date === today;
              const isPast = date < today;
              if (!exams) {
                return (
                  <div
                    key={date}
                    className={cn(
                      "flex items-center justify-center rounded-lg text-sm",
                      cellHeight,
                      isPast ? "text-muted-foreground/50" : "text-muted-foreground",
                      isToday && "ring-1 ring-primary/40"
                    )}
                  >
                    {day}
                  </div>
                );
              }
              const isSelected = date === selected;
              return (
                <button
                  key={date}
                  type="button"
                  aria-pressed={isSelected}
                  aria-label={`${longDateLabel(date)}, 시험 ${exams.length}회`}
                  onClick={() => setPicked(date)}
                  className={cn(
                    "relative flex items-center justify-center rounded-lg text-sm font-semibold transition-colors",
                    cellHeight,
                    isSelected
                      ? "bg-primary text-primary-foreground"
                      : "bg-accent text-primary-strong hover:bg-primary/15",
                    isPast && !isSelected && "opacity-60",
                    isToday && !isSelected && "ring-1 ring-primary/40"
                  )}
                >
                  {day}
                  {exams.length > 1 && (
                    <span
                      aria-hidden
                      className={cn(
                        "absolute top-0.5 right-1 text-[10px] leading-none",
                        isSelected ? "text-primary-foreground/80" : "text-primary/70"
                      )}
                    >
                      {exams.length}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        )}
      </div>

      <div className="rounded-2xl bg-secondary p-4" aria-live="polite">
        {state.status === "error" ? (
          <p className="text-sm text-muted-foreground">일정을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>
        ) : state.status === "loading" ? (
          <Skeleton className="h-12 rounded-lg" />
        ) : selected && selectedExams.length > 0 ? (
          <div className="flex flex-col gap-3">
            <p className="text-sm font-semibold">{longDateLabel(selected)} 시험</p>
            <ul className="flex flex-col gap-2">
              {selectedExams.map((exam, index) => {
                const status = registrationStatus(exam, today);
                return (
                  <li key={`${exam.roundName}-${index}`} className="flex flex-col gap-1 rounded-xl bg-card px-3 py-2.5 text-sm">
                    <span className="flex flex-wrap items-center gap-1.5">
                      <span className="font-semibold">{roundLabel(exam.roundName)}</span>
                      <Badge variant={status.open ? "default" : "secondary"}>{status.label}</Badge>
                      {exam.earlyRelease && <Badge variant="outline">성적 조기 발표</Badge>}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {exam.registrationStart && exam.registrationEnd
                        ? `접수 ${shortDateLabel(exam.registrationStart)} ~ ${shortDateLabel(exam.registrationEnd)}`
                        : "접수 기간 미정"}
                      {exam.scoreReleaseDate ? ` · 성적 발표 ${shortDateLabel(exam.scoreReleaseDate)}` : ""}
                    </span>
                  </li>
                );
              })}
            </ul>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">이 달에 등록된 시험 일정이 없어요.</p>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-muted-foreground">
        <span>출처: opic.or.kr · 일정은 변경될 수 있으니 접수 전 공식 사이트를 확인하세요.</span>
        <Button asChild variant="outline" size="sm" className="rounded-full">
          <a href={OPIC_REGISTER_URL} target="_blank" rel="noreferrer">
            접수 바로가기
            <ExternalLink />
          </a>
        </Button>
      </div>
    </div>
  );
}
