import { GuideSection } from "@/features/guide/components/GuideSection";
import { TIP_ITEMS } from "@/features/guide/data";

export function OpicTipsSection() {
  return (
    <GuideSection
      id="tips"
      title="오픽 보러 갈 때의 꿀팁"
      subtitle="당일 컨디션과 답변 템포를 위한 체크리스트"
    >
      <div className="space-y-3">
        {TIP_ITEMS.map((tip, index) => (
          <div
            key={tip.id}
            className="flex items-start gap-4 rounded-2xl border border-black/5 bg-white/80 px-4 py-3"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-full border border-[var(--accent)]/30 text-xs font-semibold text-[var(--accent-strong)]">
              {index + 1}
            </span>
            <div>
              <p className="text-sm font-semibold">{tip.title}</p>
              <p className="text-xs text-[var(--muted)]">{tip.detail}</p>
            </div>
          </div>
        ))}
      </div>
    </GuideSection>
  );
}
