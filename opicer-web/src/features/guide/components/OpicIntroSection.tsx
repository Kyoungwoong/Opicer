import { GuideSection } from "@/features/guide/components/GuideSection";

export function OpicIntroSection() {
  return (
    <GuideSection
      id="intro"
      title="오픽 설명"
      subtitle="OPIC 시험 구조 한눈에 보기"
    >
      <div className="space-y-6">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-[var(--muted)]">
            시험 흐름
          </p>
          <div className="mt-3 grid gap-3 md:grid-cols-5">
            {[
              { step: "1", label: "설문" },
              { step: "2", label: "질문" },
              { step: "3", label: "녹음" },
              { step: "4", label: "제출" },
              { step: "5", label: "리포트" },
            ].map((item) => (
              <div
                key={item.step}
                className="relative rounded-2xl border border-black/5 bg-white/80 px-4 py-3 text-center"
              >
                <span className="text-xs font-semibold text-[var(--accent-strong)]">
                  STEP {item.step}
                </span>
                <p className="mt-2 text-sm font-semibold">{item.label}</p>
              </div>
            ))}
          </div>
        </div>

        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-[var(--muted)]">
            레벨 기준 (Pyramid)
          </p>
          <div className="mt-4 space-y-2">
            {[
              {
                level: "AL",
                detail: "자연스러운 흐름, 설득력 있는 전개",
                width: "w-2/3",
              },
              {
                level: "IH",
                detail: "복잡한 구조, 높은 논리성",
                width: "w-4/5",
              },
              {
                level: "IM",
                detail: "논리적 흐름, 예시/근거 활용",
                width: "w-5/6",
              },
              {
                level: "IL / NL",
                detail: "기본 문장, 단순 시제",
                width: "w-full",
              },
            ].map((item) => (
              <div key={item.level} className="flex justify-center">
                <div
                  className={`flex ${item.width} items-center justify-between rounded-2xl border border-black/5 bg-white/80 px-4 py-3`}
                >
                  <span className="text-sm font-semibold text-[var(--accent-strong)]">
                    {item.level}
                  </span>
                  <span className="text-xs text-[var(--muted)]">
                    {item.detail}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          {[
            {
              title: "질문 유형",
              description: "Description / Past / Opinion / Role-play 등",
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
      </div>
    </GuideSection>
  );
}
