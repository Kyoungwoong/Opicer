import { UNIVERSAL_SENTENCES } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";

export function UniversalSentencesSection() {
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
      <div className="grid gap-4 md:grid-cols-2">
        {UNIVERSAL_SENTENCES.map((item) => (
          <div
            key={item.id}
            className="rounded-2xl border border-black/5 bg-white/80 p-4 shadow-sm"
          >
            <div className="flex items-center justify-between gap-3">
              <span className="text-sm font-semibold">{item.title}</span>
              <span className="text-[10px] uppercase tracking-[0.2em] text-[var(--muted)]">
                {item.situation}
              </span>
            </div>
            <p className="mt-3 text-sm leading-6 text-[var(--muted)]">
              {item.sentence}
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              {item.tags.map((tag) => (
                <span
                  key={tag}
                  className="rounded-full border border-[var(--accent)]/20 px-2.5 py-1 text-[10px] font-semibold text-[var(--accent-strong)]"
                >
                  #{tag}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
    </SectionShell>
  );
}
