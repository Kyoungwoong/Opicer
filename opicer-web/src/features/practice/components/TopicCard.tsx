import { Check } from "lucide-react";
import { cn } from "@/lib/utils";
import type { TopicItem } from "@/features/practice/types";

type Props = {
  topic: TopicItem;
  selected: boolean;
  onSelect: (topic: TopicItem) => void;
};

/** Selectable topic tile used in every category of the topic list. */
export function TopicCard({ topic, selected, onSelect }: Props) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={() => onSelect(topic)}
      className={cn(
        "group flex h-full flex-col gap-3 rounded-2xl border p-4 text-left transition-all",
        selected
          ? "border-primary bg-primary text-primary-foreground shadow-raised"
          : "bg-card hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-card"
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-base font-semibold">{topic.title}</p>
          <p className={cn("mt-0.5 text-sm", selected ? "text-primary-foreground/85" : "text-muted-foreground")}>
            {topic.englishTitle}
          </p>
        </div>
        <span
          className={cn(
            "inline-flex shrink-0 items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold",
            selected ? "bg-primary-foreground/20" : "bg-accent text-accent-foreground"
          )}
        >
          {selected ? <Check aria-hidden className="size-3.5" /> : null}
          {selected ? "선택됨" : "선택"}
        </span>
      </div>
      {topic.badges && topic.badges.length > 0 ? (
        <div className="flex flex-wrap gap-1.5">
          {topic.badges.map((badge) => (
            <span
              key={`${topic.id}-${badge.label}`}
              className={cn(
                "rounded-full border px-2 py-0.5 text-xs font-medium",
                selected ? "border-primary-foreground/30 text-primary-foreground/90" : "text-primary-strong"
              )}
            >
              {badge.label}
              {badge.count != null ? ` ${badge.count}` : ""}
            </span>
          ))}
        </div>
      ) : null}
    </button>
  );
}
