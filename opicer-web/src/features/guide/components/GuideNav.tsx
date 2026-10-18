import { cn } from "@/lib/utils";
import type { GuideSection } from "@/features/guide/types";

type Props = {
  sections: GuideSection[];
  activeId: string;
  onSelect: (id: string) => void;
};

/**
 * Section switcher. Horizontal scrollable chips on mobile so content stays
 * near the top; a sticky vertical list with summaries from lg up.
 */
export function GuideNav({ sections, activeId, onSelect }: Props) {
  return (
    <nav aria-label="가이드 목차" className="-mx-4 px-4 lg:mx-0 lg:px-0">
      <ul className="flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none] lg:sticky lg:top-6 lg:flex-col lg:overflow-visible lg:pb-0">
        {sections.map((section) => {
          const isActive = activeId === section.id;
          return (
            <li key={section.id} className="shrink-0 lg:shrink">
              <button
                type="button"
                onClick={() => onSelect(section.id)}
                aria-pressed={isActive}
                className={cn(
                  "flex w-full items-start gap-3 rounded-full border px-4 py-2 text-left text-sm font-medium whitespace-nowrap transition-colors lg:rounded-2xl lg:px-4 lg:py-3.5 lg:whitespace-normal",
                  isActive
                    ? "border-primary bg-primary text-primary-foreground lg:border-primary/40 lg:bg-card lg:text-foreground lg:shadow-card"
                    : "bg-card/70 text-muted-foreground hover:border-primary/30 hover:text-foreground"
                )}
              >
                <span
                  aria-hidden
                  className={cn(
                    "mt-1.5 hidden size-2 shrink-0 rounded-full lg:block",
                    isActive ? "bg-primary" : "bg-primary/30"
                  )}
                />
                <span className="flex flex-col gap-1">
                  <span className="lg:font-semibold">{section.title}</span>
                  <span className="hidden text-sm font-normal leading-5 text-muted-foreground lg:block">
                    {section.summary}
                  </span>
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
