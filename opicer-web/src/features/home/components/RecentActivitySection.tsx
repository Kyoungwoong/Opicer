import { cn } from "@/lib/utils";
import { RECENT_ACTIVITIES } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";
import { Button } from "@/components/ui/button";
import { ko } from "@/locales/ko";

const STATUS = {
  complete: { label: ko.home.recentActivity.status.complete, dot: "bg-primary" },
  in_progress: { label: ko.home.recentActivity.status.in_progress, dot: "bg-highlight" },
  recommended: { label: ko.home.recentActivity.status.recommended, dot: "bg-info" },
} as const;

export function RecentActivitySection() {
  return (
    <SectionShell
      title={ko.home.recentActivity.description}
      action={
        <Button variant="ghost" className="h-9 rounded-full px-3 font-semibold text-primary">
          {ko.home.recentActivity.viewAll}
        </Button>
      }
    >
      <ul className="grid gap-3 sm:grid-cols-3">
        {RECENT_ACTIVITIES.map((item) => (
          <li
            key={item.id}
            className="flex h-full flex-col gap-2.5 rounded-2xl border border-accent bg-secondary px-5 py-4"
          >
            <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
              <span aria-hidden className={cn("size-2 rounded-full", STATUS[item.status].dot)} />
              {item.dateLabel} · {STATUS[item.status].label}
            </span>
            <span className="text-lg font-bold">{item.title}</span>
            <span className="text-sm text-muted-foreground">{item.summary}</span>
          </li>
        ))}
      </ul>
    </SectionShell>
  );
}
