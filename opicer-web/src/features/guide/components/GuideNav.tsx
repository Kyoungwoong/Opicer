import type { GuideSection } from "@/features/guide/types";

export function GuideNav({ sections }: { sections: GuideSection[] }) {
  return (
    <nav className="sticky top-6 space-y-2">
      {sections.map((section) => (
        <a
          key={section.id}
          href={`#${section.id}`}
          className="flex items-start gap-3 rounded-2xl border border-black/5 bg-white/80 px-4 py-3 text-sm text-[var(--muted)] transition hover:border-[var(--accent)]/20 hover:text-[var(--accent-strong)]"
        >
          <span className="mt-1 h-2 w-2 rounded-full bg-[var(--accent)]/40" />
          <span>
            <strong className="block text-[var(--ink)]">{section.title}</strong>
            <span className="text-xs text-[var(--muted)]">{section.summary}</span>
          </span>
        </a>
      ))}
    </nav>
  );
}
