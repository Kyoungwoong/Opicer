import { ExternalLink } from "lucide-react";
import { cn } from "@/lib/utils";
import { GuideSection } from "@/features/guide/components/GuideSection";
import { Button } from "@/components/ui/button";
import { OPIC_CALENDAR, OPIC_SCHEDULE } from "@/features/guide/data";

const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

const EVENT_STYLE = {
  exam: { dot: "bg-primary", label: "시험일" },
  registration: { dot: "bg-highlight-foreground", label: "접수" },
} as const;

export function OpicScheduleSection() {
  const days = Array.from({ length: OPIC_CALENDAR.daysInMonth }, (_, i) => i + 1);
  const eventsByDate = new Map(OPIC_CALENDAR.events.map((event) => [event.date, event]));

  return (
    <GuideSection id="schedule" title="오픽 일정" subtitle="연동 준비중 일정">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border bg-secondary px-4 py-3">
        <div>
          <p className="text-xs text-muted-foreground">공식 일정 연동 준비중</p>
          <p className="text-base font-semibold text-primary-strong">{OPIC_CALENDAR.monthLabel}</p>
        </div>
        <Button asChild variant="outline" className="h-9 rounded-full px-4">
          <a href="https://www.opic.or.kr/opics/jsp/view/index.jsp" target="_blank" rel="noreferrer">
            접수 바로가기
            <ExternalLink />
          </a>
        </Button>
      </div>

      <div className="mt-4 rounded-2xl border bg-secondary p-3 sm:p-4">
        <div className="grid grid-cols-7 gap-1.5 text-center text-xs font-medium text-muted-foreground sm:gap-2">
          {WEEKDAY_LABELS.map((label) => (
            <span key={label}>{label}</span>
          ))}
        </div>
        <div className="mt-2 grid grid-cols-7 gap-1.5 sm:gap-2">
          {Array.from({ length: OPIC_CALENDAR.startWeekday }).map((_, index) => (
            <div key={`empty-${index}`} className="h-10" />
          ))}
          {days.map((day) => {
            const event = eventsByDate.get(day);
            return (
              <div
                key={day}
                className={cn(
                  "relative flex h-10 items-center justify-center rounded-lg border bg-card text-sm",
                  event?.type === "exam" && "border-primary/40 font-semibold text-primary-strong"
                )}
              >
                {day}
                {event ? (
                  <span
                    className={cn("absolute bottom-1 left-1/2 size-1.5 -translate-x-1/2 rounded-full", EVENT_STYLE[event.type].dot)}
                  >
                    <span className="sr-only">{EVENT_STYLE[event.type].label}</span>
                  </span>
                ) : null}
              </div>
            );
          })}
        </div>
        <div className="mt-3 flex gap-4 text-xs text-muted-foreground">
          {Object.values(EVENT_STYLE).map((style) => (
            <span key={style.label} className="inline-flex items-center gap-1.5">
              <span aria-hidden className={cn("size-2 rounded-full", style.dot)} />
              {style.label}
            </span>
          ))}
        </div>
      </div>

      <ul className="mt-4 flex flex-col gap-2">
        {OPIC_SCHEDULE.map((item) => (
          <li
            key={item.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border bg-secondary px-4 py-3"
          >
            <div>
              <p className="text-sm font-semibold">{item.title}</p>
              <p className="text-sm text-muted-foreground">{item.note}</p>
            </div>
            <span className="text-sm font-semibold text-primary-strong">{item.dateLabel}</span>
          </li>
        ))}
      </ul>
    </GuideSection>
  );
}
