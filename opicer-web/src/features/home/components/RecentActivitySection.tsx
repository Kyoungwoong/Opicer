import { cn } from "@/lib/utils";
import { RECENT_ACTIVITIES } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";
import { Button } from "@/components/ui/button";
import { ko } from "@/locales/ko";

const STATUS = {
  complete: { label: ko.home.recentActivity.status.complete, className: "bg-success-subtle text-success" },
  in_progress: { label: ko.home.recentActivity.status.in_progress, className: "bg-info-subtle text-info" },
  recommended: { label: ko.home.recentActivity.status.recommended, className: "bg-highlight/50 text-highlight-foreground" },
} as const;

export function RecentActivitySection() {
  return (
    <SectionShell
      title="Recent activity"
      description={ko.home.recentActivity.description}
      action={
        <Button variant="outline" className="h-9 rounded-full px-4">
          {ko.home.recentActivity.viewAll}
        </Button>
      }
    >
      <ul className="flex flex-col gap-2">
        {RECENT_ACTIVITIES.map((item) => (
          <li
            key={item.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border bg-background/60 px-4 py-3.5"
          >
            <div className="min-w-0">
              <p className="text-sm font-semibold">{item.title}</p>
              <p className="mt-0.5 text-sm text-muted-foreground">{item.summary}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs text-muted-foreground">{item.dateLabel}</span>
              <span className={cn("rounded-full px-2.5 py-1 text-xs font-semibold", STATUS[item.status].className)}>
                {STATUS[item.status].label}
              </span>
            </div>
          </li>
        ))}
      </ul>
    </SectionShell>
  );
}
