import { GuideSection } from "@/features/guide/components/GuideSection";

export function OpicIntroSection() {
  return (
    <GuideSection
      id="intro"
      title="오픽 설명"
      subtitle="OPIC 시험 구조 한눈에 보기"
    >
      <div className="grid gap-4 md:grid-cols-2">
        {[
          {
            title: "시험 흐름",
            description: "설문 → 질문 → 녹음 → 제출 → 분석 리포트",
          },
          {
            title: "질문 유형",
            description: "Description / Past / Opinion / Role-play 등",
          },
          {
            title: "레벨 기준",
            description: "NL·IL → IM → IH → AL 단계별 목표",
          },
          {
            title: "평가 포인트",
            description: "구조, 논리성, 표현 다양성, 일관된 흐름",
          },
        ].map((item) => (
          <div
            key={item.title}
            className="rounded-2xl border border-black/5 bg-white/80 p-4"
          >
            <p className="text-sm font-semibold">{item.title}</p>
            <p className="mt-2 text-sm text-[var(--muted)]">{item.description}</p>
          </div>
        ))}
      </div>
    </GuideSection>
  );
}
