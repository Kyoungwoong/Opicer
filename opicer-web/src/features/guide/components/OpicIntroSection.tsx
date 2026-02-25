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
            ].map((item, index) => (
              <div key={item.step} className="relative">
                <div className="rounded-2xl border border-black/5 bg-white/80 px-4 py-3 text-center">
                  <span className="text-xs font-semibold text-[var(--accent-strong)]">
                    STEP {item.step}
                  </span>
                  <p className="mt-2 text-sm font-semibold">{item.label}</p>
                </div>
                {index < 4 && (
                  <span className="absolute right-[-14px] top-1/2 hidden -translate-y-1/2 text-lg text-[var(--muted)] md:block">
                    →
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>

        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-[var(--muted)]">
            레벨 기준 (Pyramid)
          </p>
          <div className="mt-4 space-y-3">
            {[
              {
                level: "AL",
                detail: "자연스러운 흐름 / 설득력 있는 전개",
                width: "w-2/3",
                color: "bg-red-600",
              },
              {
                level: "IH",
                detail: "복잡한 구조 / 높은 논리성",
                width: "w-4/5",
                color: "bg-blue-700",
              },
              {
                level: "IM",
                detail: "논리적 흐름 / 예시·근거 활용",
                width: "w-5/6",
                color: "bg-emerald-700",
              },
              {
                level: "IL / NL",
                detail: "기본 문장 / 단순 시제",
                width: "w-full",
                color: "bg-slate-700",
              },
            ].map((item) => (
              <div key={item.level} className="flex items-center gap-4">
                <div className="flex w-32 justify-end text-center">
                  <span className="text-xs font-semibold leading-4 text-[var(--muted)]">
                    {item.detail.split(" / ").map((chunk, idx, arr) => (
                      <span key={chunk}>
                        {chunk}
                        {idx < arr.length - 1 && <br />}
                      </span>
                    ))}
                  </span>
                </div>
                <div className="flex flex-1 justify-center">
                  <div
                    className={`relative flex ${item.width} items-center justify-center px-4 py-3 text-sm font-bold text-white`}
                    style={{
                      clipPath: "polygon(6% 0, 94% 0, 100% 100%, 0 100%)",
                    }}
                  >
                    <div className={`absolute inset-0 -z-10 ${item.color}`} />
                    {item.level}
                  </div>
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
