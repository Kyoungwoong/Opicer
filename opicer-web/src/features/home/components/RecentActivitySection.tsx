import { RECENT_ACTIVITIES } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";
import { ko } from "@/locales/ko";

const STATUS_LABEL = {
  complete: ko.home.recentActivity.status.complete,
  in_progress: ko.home.recentActivity.status.in_progress,
  recommended: ko.home.recentActivity.status.recommended,
} as const;

export function RecentActivitySection() {
  return (
    <SectionShell
      title="Recent activity"
      description={ko.home.recentActivity.description}
      action={
        <button className="rounded-full border border-black/10 px-4 py-2 text-xs font-semibold text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white">
          {ko.home.recentActivity.viewAll}
        </button>
      }
    >
      <div className="space-y-3">
        {RECENT_ACTIVITIES.map((item) => (
          <div
            key={item.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-black/5 bg-white/80 p-4"
          >
            <div>
              <p className="text-sm font-semibold">{item.title}</p>
              <p className="mt-1 text-xs text-[var(--muted)]">{item.summary}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs text-[var(--muted)]">{item.dateLabel}</span>
              <span className="rounded-full bg-[var(--accent)]/10 px-3 py-1 text-[10px] font-semibold text-[var(--accent-strong)]">
                {STATUS_LABEL[item.status]}
              </span>
            </div>
          </div>
        ))}
      </div>
    </SectionShell>
  );
}
