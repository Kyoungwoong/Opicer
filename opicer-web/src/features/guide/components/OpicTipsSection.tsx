import { GuideSection } from "@/features/guide/components/GuideSection";
import { TIP_ITEMS } from "@/features/guide/data";

export function OpicTipsSection() {
  return (
    <GuideSection
      id="tips"
      title="오픽 보러 갈 때의 꿀팁"
      subtitle="당일 컨디션과 답변 템포를 위한 체크리스트"
    >
      <ol className="flex flex-col gap-2">
        {TIP_ITEMS.map((tip, index) => (
          <li key={tip.id} className="flex items-start gap-4 rounded-2xl border bg-background/60 px-4 py-3.5">
            <span className="grid size-8 shrink-0 place-items-center rounded-full bg-accent text-sm font-bold text-accent-foreground">
              {index + 1}
            </span>
            <div className="flex flex-col gap-0.5">
              <p className="text-sm font-semibold">{tip.title}</p>
              <p className="text-sm text-muted-foreground">{tip.detail}</p>
            </div>
          </li>
        ))}
      </ol>
    </GuideSection>
  );
}
