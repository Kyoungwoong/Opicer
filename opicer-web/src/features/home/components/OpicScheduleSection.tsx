import { cn } from "@/lib/utils";
import { OPIC_CALENDAR } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";
import { Badge } from "@/components/ui/badge";
import { ko } from "@/locales/ko";

const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

const EVENT_DOT = {
  exam: "bg-primary",
  registration: "bg-highlight-foreground",
} as const;

export function OpicScheduleSection() {
  const days = Array.from({ length: OPIC_CALENDAR.daysInMonth }, (_, i) => i + 1);
  const eventsByDate = new Map(OPIC_CALENDAR.events.map((event) => [event.date, event]));

  return (
    <SectionShell
      title={ko.home.opicSchedule.description}
      action={<Badge variant="secondary">{ko.home.opicSchedule.actionLabel}</Badge>}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-base font-semibold text-primary-strong">{OPIC_CALENDAR.monthLabel}</h3>
        <span className="text-xs text-muted-foreground">{ko.home.opicSchedule.officialNotice}</span>
      </div>

      <div className="mt-4 grid grid-cols-7 gap-1.5 text-center text-xs font-medium text-muted-foreground">
        {WEEKDAY_LABELS.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>

      <div className="mt-2 grid grid-cols-7 gap-1.5">
        {Array.from({ length: OPIC_CALENDAR.startWeekday }).map((_, index) => (
          <div key={`empty-${index}`} className="h-9" />
        ))}
        {days.map((day) => {
          const event = eventsByDate.get(day);
          return (
            <div
              key={day}
              className={cn(
                "relative flex h-9 items-center justify-center rounded-lg border bg-card text-sm",
                event?.type === "exam" && "border-primary/40 font-semibold text-primary-strong"
              )}
            >
              {day}
              {event ? (
                <span
                  aria-hidden
                  className={cn("absolute bottom-1 left-1/2 size-1.5 -translate-x-1/2 rounded-full", EVENT_DOT[event.type])}
                />
              ) : null}
            </div>
          );
        })}
      </div>

      <ul className="mt-4 flex flex-col gap-1.5 text-sm text-muted-foreground">
        {OPIC_CALENDAR.events.map((event) => (
          <li key={event.id} className="flex items-center gap-2">
            <span aria-hidden className={cn("size-2 rounded-full", EVENT_DOT[event.type])} />
            <span>
              {event.date}
              {ko.home.opicSchedule.dateUnit} {ko.home.opicSchedule.eventSeparator} {event.label}
            </span>
          </li>
        ))}
      </ul>
    </SectionShell>
  );
}
