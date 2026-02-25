import { OPIC_CALENDAR } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";

const WEEKDAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

export function OpicScheduleSection() {
  const days = Array.from({ length: OPIC_CALENDAR.daysInMonth }, (_, i) => i + 1);
  const eventsByDate = new Map(
    OPIC_CALENDAR.events.map((event) => [event.date, event])
  );

  return (
    <SectionShell
      title="OPIC schedule"
      description="오픽 일정 캘린더"
      action={
        <span className="rounded-full border border-black/10 px-4 py-2 text-xs font-semibold text-[var(--muted)]">
          연동 준비중
        </span>
      }
    >
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-semibold text-[var(--accent-strong)]">
          {OPIC_CALENDAR.monthLabel}
        </h4>
        <span className="text-xs text-[var(--muted)]">
          공식 일정 연동 예정
        </span>
      </div>

      <div className="mt-4 grid grid-cols-7 gap-2 text-[11px] text-[var(--muted)]">
        {WEEKDAY_LABELS.map((label) => (
          <span key={label} className="text-center">
            {label}
          </span>
        ))}
      </div>

      <div className="mt-2 grid grid-cols-7 gap-2 text-sm">
        {Array.from({ length: OPIC_CALENDAR.startWeekday }).map((_, index) => (
          <div key={`empty-${index}`} className="h-10" />
        ))}
        {days.map((day) => {
          const event = eventsByDate.get(day);
          return (
            <div
              key={day}
              className="relative flex h-10 items-center justify-center rounded-xl border border-black/5 bg-white/70 text-xs"
            >
              {day}
              {event && (
                <span
                  className={`absolute bottom-1 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full ${
                    event.type === "exam" ? "bg-[var(--accent)]" : "bg-amber-400"
                  }`}
                />
              )}
            </div>
          );
        })}
      </div>

      <div className="mt-4 space-y-2 text-xs text-[var(--muted)]">
        {OPIC_CALENDAR.events.map((event) => (
          <div key={event.id} className="flex items-center gap-2">
            <span
              className={`h-2 w-2 rounded-full ${
                event.type === "exam" ? "bg-[var(--accent)]" : "bg-amber-400"
              }`}
            />
            <span>
              {event.date}일 · {event.label}
            </span>
          </div>
        ))}
      </div>
    </SectionShell>
  );
}
