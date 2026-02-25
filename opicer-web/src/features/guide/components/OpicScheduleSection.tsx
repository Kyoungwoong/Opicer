import { GuideSection } from "@/features/guide/components/GuideSection";
import { OPIC_CALENDAR, OPIC_SCHEDULE } from "@/features/guide/data";

const WEEKDAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

export function OpicScheduleSection() {
  const days = Array.from({ length: OPIC_CALENDAR.daysInMonth }, (_, i) => i + 1);
  const eventsByDate = new Map(
    OPIC_CALENDAR.events.map((event) => [event.date, event])
  );

  return (
    <GuideSection id="schedule" title="오픽 일정" subtitle="연동 준비중 일정">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-black/5 bg-white/70 px-4 py-3">
        <div>
          <p className="text-xs text-[var(--muted)]">공식 일정 연동 준비중</p>
          <p className="text-sm font-semibold text-[var(--accent-strong)]">
            {OPIC_CALENDAR.monthLabel}
          </p>
        </div>
        <a
          className="rounded-full border border-black/10 px-4 py-2 text-xs font-semibold text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
          href="https://www.opic.or.kr/opics/jsp/view/index.jsp"
          target="_blank"
          rel="noreferrer"
        >
          접수 바로가기
        </a>
      </div>

      <div className="mt-4 rounded-3xl border border-black/5 bg-white/80 p-4">
        <div className="grid grid-cols-7 gap-2 text-[11px] text-[var(--muted)]">
          {WEEKDAY_LABELS.map((label) => (
            <span key={label} className="text-center">
              {label}
            </span>
          ))}
        </div>
        <div className="mt-3 grid grid-cols-7 gap-2 text-sm">
          {Array.from({ length: OPIC_CALENDAR.startWeekday }).map((_, index) => (
            <div key={`empty-${index}`} className="h-10" />
          ))}
          {days.map((day) => {
            const event = eventsByDate.get(day);
            return (
              <div
                key={day}
                className="relative flex h-10 items-center justify-center rounded-xl border border-black/5 bg-white text-xs"
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
      </div>

      <div className="mt-4 space-y-3">
        {OPIC_SCHEDULE.map((item) => (
          <div
            key={item.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-black/5 bg-white/80 px-4 py-3"
          >
            <div>
              <p className="text-sm font-semibold">{item.title}</p>
              <p className="text-xs text-[var(--muted)]">{item.note}</p>
            </div>
            <span className="text-xs font-semibold text-[var(--accent-strong)]">
              {item.dateLabel}
            </span>
          </div>
        ))}
      </div>
    </GuideSection>
  );
}
