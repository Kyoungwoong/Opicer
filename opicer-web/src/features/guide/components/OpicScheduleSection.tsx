import { GuideSection } from "@/features/guide/components/GuideSection";
import { OPIC_SCHEDULE } from "@/features/guide/data";

export function OpicScheduleSection() {
  return (
    <GuideSection id="schedule" title="오픽 일정" subtitle="연동 준비중 일정">
      <div className="flex items-center justify-between rounded-2xl border border-black/5 bg-white/70 px-4 py-3">
        <span className="text-xs text-[var(--muted)]">
          공식 일정 연동 준비중
        </span>
        <span className="rounded-full border border-black/10 px-3 py-1 text-[10px] font-semibold text-[var(--muted)]">
          준비중
        </span>
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
