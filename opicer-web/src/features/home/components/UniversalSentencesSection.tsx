import { useEffect, useMemo, useState } from "react";
import { UNIVERSAL_SENTENCES } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";

const SLIDE_INTERVAL_MS = 5000;

export function UniversalSentencesSection() {
  const [index, setIndex] = useState(0);
  const total = UNIVERSAL_SENTENCES.length;
  const current = useMemo(() => UNIVERSAL_SENTENCES[index], [index]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setIndex((prev) => (prev + 1) % total);
    }, SLIDE_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [total]);

  const goTo = (next: number) => {
    const normalized = (next + total) % total;
    setIndex(normalized);
  };

  return (
    <SectionShell
      title="Universal sentences"
      description="OPIC에서 자주 쓰는 만능 문장"
      action={
        <span className="rounded-full border border-[var(--accent)]/20 px-4 py-2 text-xs font-semibold text-[var(--accent-strong)]">
          관리자 연동 예정
        </span>
      }
    >
      <div className="flex flex-col gap-4 rounded-3xl border border-black/5 bg-white/70 p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-[var(--accent-strong)]">
              {current.title}
            </p>
            <p className="mt-1 text-[10px] uppercase tracking-[0.2em] text-[var(--muted)]">
              {current.situation}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => goTo(index - 1)}
              className="flex h-9 w-9 items-center justify-center rounded-full border border-black/10 text-sm text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
              aria-label="Previous sentence"
            >
              ←
            </button>
            <button
              type="button"
              onClick={() => goTo(index + 1)}
              className="flex h-9 w-9 items-center justify-center rounded-full border border-black/10 text-sm text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
              aria-label="Next sentence"
            >
              →
            </button>
          </div>
        </div>

        <p className="text-lg font-semibold leading-relaxed text-[var(--ink)]">
          {current.sentence}
        </p>

        <div className="flex flex-wrap gap-2">
          {current.tags.map((tag) => (
            <span
              key={tag}
              className="rounded-full border border-[var(--accent)]/20 px-2.5 py-1 text-[10px] font-semibold text-[var(--accent-strong)]"
            >
              #{tag}
            </span>
          ))}
        </div>

        <div className="flex items-center gap-2 pt-2">
          {UNIVERSAL_SENTENCES.map((item, dotIndex) => (
            <button
              key={item.id}
              type="button"
              onClick={() => goTo(dotIndex)}
              className={`h-2.5 w-2.5 rounded-full transition ${
                dotIndex === index
                  ? "bg-[var(--accent)]"
                  : "bg-[var(--accent)]/20 hover:bg-[var(--accent)]/50"
              }`}
              aria-label={`Go to sentence ${dotIndex + 1}`}
            />
          ))}
        </div>
      </div>
    </SectionShell>
  );
}
