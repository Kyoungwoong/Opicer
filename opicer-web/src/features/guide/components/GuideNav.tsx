import type { GuideSection } from "@/features/guide/types";

type Props = {
  sections: GuideSection[];
  activeId: string;
  onSelect: (id: string) => void;
};

export function GuideNav({ sections, activeId, onSelect }: Props) {
  return (
    <nav className="sticky top-6 space-y-2">
      {sections.map((section) => (
        <button
          key={section.id}
          type="button"
          onClick={() => onSelect(section.id)}
          className={`flex w-full items-start gap-3 rounded-2xl border px-4 py-3 text-left text-sm transition ${
            activeId === section.id
              ? "border-[var(--accent)]/40 bg-white text-[var(--accent-strong)]"
              : "border-black/5 bg-white/80 text-[var(--muted)] hover:border-[var(--accent)]/20 hover:text-[var(--accent-strong)]"
          }`}
          aria-current={activeId === section.id ? "page" : undefined}
        >
          <span
            className={`mt-1 h-2 w-2 rounded-full ${
              activeId === section.id ? "bg-[var(--accent)]" : "bg-[var(--accent)]/40"
            }`}
          />
          <span>
            <strong className="block text-[var(--ink)]">{section.title}</strong>
            <span className="text-xs text-[var(--muted)]">{section.summary}</span>
          </span>
        </button>
      ))}
    </nav>
  );
}
