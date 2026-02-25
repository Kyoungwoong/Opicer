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
          <div className="mt-4 grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
            <div className="flex justify-center">
              <div className="h-[240px] w-full max-w-sm">
                <svg
                  viewBox="0 0 300 240"
                  className="h-full w-full"
                  aria-label="OPIC level pyramid"
                >
                  <polygon points="150,0 190,60 110,60" fill="#dc2626" />
                  <polygon points="110,60 190,60 230,120 70,120" fill="#1d4ed8" />
                  <polygon points="70,120 230,120 270,180 30,180" fill="#047857" />
                  <polygon points="30,180 270,180 300,240 0,240" fill="#334155" />

                  <text x="150" y="38" textAnchor="middle" className="fill-white font-bold" fontSize="18">
                    AL
                  </text>
                  <text x="150" y="98" textAnchor="middle" className="fill-white font-bold" fontSize="18">
                    IH
                  </text>
                  <text x="150" y="158" textAnchor="middle" className="fill-white font-bold" fontSize="18">
                    IM
                  </text>
                  <text x="150" y="218" textAnchor="middle" className="fill-white font-bold" fontSize="16">
                    IL / NL
                  </text>
                </svg>
              </div>
            </div>

            <div className="grid h-[240px] grid-rows-4 gap-3">
              {[
                { level: "AL", detail: "자연스러운 흐름 / 설득력 있는 전개" },
                { level: "IH", detail: "복잡한 구조 / 높은 논리성" },
                { level: "IM", detail: "논리적 흐름 / 예시·근거 활용" },
                { level: "IL / NL", detail: "기본 문장 / 단순 시제" },
              ].map((item) => (
                <div
                  key={item.level}
                  className="flex items-center rounded-2xl border border-black/5 bg-white/80 px-4 py-3"
                >
                  <div>
                    <p className="text-sm font-semibold text-[var(--accent-strong)]">
                      {item.level}
                    </p>
                    <p className="text-xs font-semibold leading-4 text-[var(--muted)]">
                      {item.detail.split(" / ").map((chunk, idx, arr) => (
                        <span key={chunk}>
                          {chunk}
                          {idx < arr.length - 1 && <br />}
                        </span>
                      ))}
                    </p>
                  </div>
                </div>
              ))}
            </div>
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
