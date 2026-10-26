import { GuideSection } from "@/features/guide/components/GuideSection";
import { Eyebrow } from "@/components/common/SectionHeader";

const STEPS = ["설문", "질문", "녹음", "제출", "리포트"];

// Brand teal scale, darkest at the top of the pyramid.
const LEVELS = [
  { level: "AL", detail: ["자연스러운 흐름", "설득력 있는 전개"], fill: "var(--primary-strong)", text: "var(--primary-foreground)" },
  { level: "IH", detail: ["복잡한 구조", "높은 논리성"], fill: "var(--primary)", text: "var(--primary-foreground)" },
  { level: "IM", detail: ["논리적 흐름", "예시·근거 활용"], fill: "color-mix(in oklab, var(--primary) 65%, white)", text: "var(--accent-foreground)" },
  { level: "IL / NL", detail: ["기본 문장", "단순 시제"], fill: "color-mix(in oklab, var(--primary) 30%, white)", text: "var(--accent-foreground)" },
];

const PYRAMID_ROWS = [
  { points: "150,0 187.5,60 112.5,60", y: 40 },
  { points: "112.5,60 187.5,60 225,120 75,120", y: 96 },
  { points: "75,120 225,120 262.5,180 37.5,180", y: 156 },
  { points: "37.5,180 262.5,180 300,240 0,240", y: 216 },
];

export function OpicIntroSection() {
  return (
    <GuideSection id="intro" title="오픽 설명" subtitle="OPIC 시험 구조 한눈에 보기">
      <div className="flex flex-col gap-8">
        <div className="flex flex-col gap-3">
          <Eyebrow>시험 흐름</Eyebrow>
          <ol className="grid grid-cols-5 gap-1.5 sm:gap-3">
            {STEPS.map((label, index) => (
              <li
                key={label}
                className="flex flex-col items-center gap-2 rounded-2xl border bg-secondary px-1 py-3 text-center sm:py-4"
              >
                <span className="grid size-7 place-items-center rounded-full bg-primary text-xs font-bold text-primary-foreground">
                  {index + 1}
                </span>
                <span className="text-sm font-semibold">{label}</span>
              </li>
            ))}
          </ol>
        </div>

        <div className="flex flex-col gap-3">
          <Eyebrow>레벨 기준</Eyebrow>
          <div className="grid items-center gap-6 md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
            <svg
              viewBox="0 0 300 240"
              className="mx-auto h-auto w-full max-w-xs"
              role="img"
              aria-label="OPIC 레벨 피라미드: 위에서부터 AL, IH, IM, IL/NL"
            >
              {PYRAMID_ROWS.map((row, index) => (
                <g key={LEVELS[index].level}>
                  <polygon points={row.points} style={{ fill: LEVELS[index].fill, stroke: "var(--card)" }} strokeWidth="3" />
                  <text
                    x="150"
                    y={row.y}
                    textAnchor="middle"
                    fontSize={index === 3 ? 16 : 18}
                    fontWeight="700"
                    style={{ fill: LEVELS[index].text }}
                  >
                    {LEVELS[index].level}
                  </text>
                </g>
              ))}
            </svg>

            <ul className="flex flex-col gap-2">
              {LEVELS.map((item) => (
                <li key={item.level} className="flex items-center gap-3 rounded-2xl border bg-secondary px-4 py-3">
                  <span aria-hidden className="size-3 shrink-0 rounded-full" style={{ backgroundColor: item.fill }} />
                  <span className="w-16 shrink-0 text-sm font-bold text-primary-strong">{item.level}</span>
                  <span className="text-sm text-muted-foreground">{item.detail.join(" · ")}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="grid gap-3 md:grid-cols-2">
          {[
            { title: "질문 유형", description: "Description / Past / Opinion / Role-play 등" },
            { title: "평가 포인트", description: "구조, 논리성, 표현 다양성, 일관된 흐름" },
          ].map((item) => (
            <div key={item.title} className="rounded-2xl border bg-accent/60 p-4">
              <p className="text-sm font-semibold text-accent-foreground">{item.title}</p>
              <p className="mt-1.5 text-sm text-muted-foreground">{item.description}</p>
            </div>
          ))}
        </div>
      </div>
    </GuideSection>
  );
}
