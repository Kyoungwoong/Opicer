"use client";

import { useId, useRef, useState } from "react";
import { Clock, Lightbulb } from "lucide-react";
import { cn } from "@/lib/utils";
import { GuideSection } from "@/features/guide/components/GuideSection";
import { Eyebrow } from "@/components/common/SectionHeader";
import { EXAM_STEPS, LEVEL_GUIDES } from "@/features/guide/data";
import type { LevelGuide } from "@/features/guide/types";

// Brand teal scale, darkest at the top of the pyramid (same order as LEVEL_GUIDES).
const LEVEL_COLORS: Record<LevelGuide["id"], { fill: string; text: string }> = {
  AL: { fill: "var(--primary-strong)", text: "var(--primary-foreground)" },
  IH: { fill: "var(--primary)", text: "var(--primary-foreground)" },
  IM: { fill: "color-mix(in oklab, var(--primary) 65%, white)", text: "var(--accent-foreground)" },
  IL: { fill: "color-mix(in oklab, var(--primary) 30%, white)", text: "var(--accent-foreground)" },
};

const PYRAMID_ROWS = [
  { points: "150,0 187.5,60 112.5,60", y: 40 },
  { points: "112.5,60 187.5,60 225,120 75,120", y: 96 },
  { points: "75,120 225,120 262.5,180 37.5,180", y: 156 },
  { points: "37.5,180 262.5,180 300,240 0,240", y: 216 },
];

/** Panel swap animation; tw-animate-css classes, disabled under prefers-reduced-motion. */
const PANEL_ENTER = "animate-in fade-in slide-in-from-bottom-1 duration-300 motion-reduce:animate-none";

function ExamFlow() {
  const [activeIndex, setActiveIndex] = useState(0);
  const panelId = useId();
  const buttonsRef = useRef<(HTMLButtonElement | null)[]>([]);
  const step = EXAM_STEPS[activeIndex];

  const select = (index: number) => {
    const next = (index + EXAM_STEPS.length) % EXAM_STEPS.length;
    setActiveIndex(next);
    buttonsRef.current[next]?.focus();
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === "ArrowRight") select(activeIndex + 1);
    else if (event.key === "ArrowLeft") select(activeIndex - 1);
    else return;
    event.preventDefault();
  };

  return (
    <div className="flex flex-col gap-3">
      <Eyebrow>시험 흐름</Eyebrow>
      <p className="text-sm text-muted-foreground">단계를 눌러 무엇을 하는지 확인하세요. ←/→ 키로도 이동할 수 있어요.</p>
      <ol
        aria-label="시험 진행 단계"
        onKeyDown={handleKeyDown}
        className="-mx-1 flex snap-x gap-2 overflow-x-auto px-1 pb-1 [scrollbar-width:none] md:grid md:grid-cols-7 md:overflow-visible"
      >
        {EXAM_STEPS.map((item, index) => {
          const isActive = index === activeIndex;
          const isDone = index < activeIndex;
          return (
            <li key={item.id} className="shrink-0 snap-start md:shrink">
              <button
                ref={(el) => {
                  buttonsRef.current[index] = el;
                }}
                type="button"
                aria-pressed={isActive}
                aria-controls={panelId}
                tabIndex={isActive ? 0 : -1}
                onClick={() => setActiveIndex(index)}
                className={cn(
                  "flex h-full w-24 flex-col items-center gap-2 rounded-2xl border px-1 py-3 text-center transition-colors md:w-full",
                  isActive
                    ? "border-primary bg-accent text-accent-foreground"
                    : "bg-secondary text-muted-foreground hover:border-primary/40 hover:text-foreground"
                )}
              >
                <span
                  className={cn(
                    "grid size-7 place-items-center rounded-full text-xs font-bold transition-colors",
                    isActive || isDone ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
                  )}
                >
                  {index + 1}
                </span>
                <span className="text-xs font-semibold sm:text-sm">{item.label}</span>
              </button>
            </li>
          );
        })}
      </ol>

      <div
        key={step.id}
        id={panelId}
        role="region"
        aria-label="선택한 단계 설명"
        className={cn("flex flex-col gap-3 rounded-2xl bg-secondary p-4 sm:p-5", PANEL_ENTER)}
      >
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="font-semibold">
            <span className="text-primary">{activeIndex + 1}단계</span> {step.title}
          </p>
          <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
            <Clock aria-hidden className="size-3.5" />
            {step.duration}
          </span>
        </div>
        <p className="text-sm leading-6">{step.description}</p>
        <p className="flex gap-2 rounded-xl bg-highlight-subtle px-3 py-2.5 text-sm leading-6 text-highlight-foreground">
          <Lightbulb aria-hidden className="mt-0.5 size-4 shrink-0" />
          <span>{step.tip}</span>
        </p>
      </div>
    </div>
  );
}

function LevelFocus() {
  const [activeId, setActiveId] = useState<LevelGuide["id"]>("IM");
  const panelId = useId();
  const level = LEVEL_GUIDES.find((item) => item.id === activeId) ?? LEVEL_GUIDES[0];
  const dimmed = (id: LevelGuide["id"]) => id !== activeId;

  return (
    <div className="flex flex-col gap-3">
      <Eyebrow>레벨 기준</Eyebrow>
      <p className="text-sm text-muted-foreground">레벨을 눌러 그 등급에 필요한 말하기 수준을 확인하세요.</p>
      <div className="grid items-center gap-6 md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        {/* Decorative mirror of the buttons; mouse users can click a layer, keyboard users use the list. */}
        <svg viewBox="0 0 300 240" className="mx-auto h-auto w-full max-w-xs" aria-hidden>
          {PYRAMID_ROWS.map((row, index) => {
            const item = LEVEL_GUIDES[index];
            return (
              <g
                key={item.id}
                onClick={() => setActiveId(item.id)}
                className={cn(
                  "cursor-pointer transition-[opacity,filter] duration-300 motion-reduce:transition-none",
                  dimmed(item.id) && "opacity-30 grayscale"
                )}
              >
                <polygon
                  points={row.points}
                  style={{ fill: LEVEL_COLORS[item.id].fill, stroke: "var(--card)" }}
                  strokeWidth="3"
                />
                <text
                  x="150"
                  y={row.y}
                  textAnchor="middle"
                  fontSize={index === 3 ? 16 : 18}
                  fontWeight="700"
                  style={{ fill: LEVEL_COLORS[item.id].text }}
                >
                  {item.label}
                </text>
              </g>
            );
          })}
        </svg>

        <ul className="flex flex-col gap-2" aria-label="레벨 선택">
          {LEVEL_GUIDES.map((item) => (
            <li key={item.id}>
              <button
                type="button"
                aria-pressed={item.id === activeId}
                aria-controls={panelId}
                onClick={() => setActiveId(item.id)}
                className={cn(
                  "flex w-full items-center gap-3 rounded-2xl border px-4 py-3 text-left transition-[opacity,filter,border-color,background-color] duration-300 motion-reduce:transition-none",
                  item.id === activeId
                    ? "border-primary bg-accent"
                    : "bg-secondary opacity-50 grayscale hover:opacity-80"
                )}
              >
                <span
                  aria-hidden
                  className="size-3 shrink-0 rounded-full"
                  style={{ backgroundColor: LEVEL_COLORS[item.id].fill }}
                />
                <span className="w-16 shrink-0 text-sm font-bold text-primary-strong">{item.label}</span>
                <span className="text-sm text-muted-foreground">{item.summary}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>

      <div
        key={level.id}
        id={panelId}
        role="region"
        aria-label="선택한 레벨 설명"
        className={cn("flex flex-col gap-4 rounded-2xl bg-secondary p-4 sm:p-5", PANEL_ENTER)}
      >
        <p className="font-semibold">
          <span className="text-primary">{level.label}</span> 레벨에 필요한 것
        </p>
        <dl className="grid gap-3 sm:grid-cols-3">
          {[
            { term: "문장 구조", value: level.structure },
            { term: "시제", value: level.tense },
            { term: "답변 길이", value: level.length },
          ].map((row) => (
            <div key={row.term} className="flex flex-col gap-1 rounded-xl bg-card p-3">
              <dt className="text-xs font-semibold text-primary">{row.term}</dt>
              <dd className="text-sm leading-6">{row.value}</dd>
            </div>
          ))}
        </dl>
        <figure className="flex flex-col gap-1.5 rounded-xl border bg-card p-3">
          <figcaption className="text-xs font-semibold text-muted-foreground">이런 느낌의 답변</figcaption>
          <blockquote className="text-sm leading-6 italic" lang="en">
            {level.example}
          </blockquote>
        </figure>
        <p className="flex gap-2 rounded-xl bg-highlight-subtle px-3 py-2.5 text-sm leading-6 text-highlight-foreground">
          <Lightbulb aria-hidden className="mt-0.5 size-4 shrink-0" />
          <span>{level.nextStep}</span>
        </p>
        <p className="text-xs text-muted-foreground">
          공식 채점 기준을 쉽게 풀어 쓴 참고용 설명이에요. 실제 등급은 OPIc 시험 결과로만 확인할 수 있어요.
        </p>
      </div>
    </div>
  );
}

export function OpicIntroSection() {
  return (
    <GuideSection id="intro" title="오픽 설명" subtitle="OPIC 시험 구조 한눈에 보기">
      <div className="flex flex-col gap-10">
        <ExamFlow />
        <LevelFocus />
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
