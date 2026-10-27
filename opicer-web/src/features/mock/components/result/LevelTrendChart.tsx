"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { LEVEL_ORDER } from "@/features/practice/evaluation-labels";
import { formatShortDate } from "@/features/practice/history-format";
import type { OpicLevel } from "@/features/practice/types";
import type { MockTrendPoint } from "@/features/mock/types";

const WIDTH = 560;
const HEIGHT = 200;
const PAD = { top: 16, right: 24, bottom: 32, left: 40 };

type Props = { points: MockTrendPoint[]; currentId: string };

/**
 * Estimated level of up to six evaluated tests, oldest first. One series on an ordinal NL–AL axis, so it is a
 * single line in the brand color; every point has a hover/focus tooltip and the same data is in a table.
 */
export function LevelTrendChart({ points, currentId }: Props) {
  const [active, setActive] = useState<number | null>(null);
  const plotW = WIDTH - PAD.left - PAD.right;
  const plotH = HEIGHT - PAD.top - PAD.bottom;
  const x = (i: number) => PAD.left + (points.length === 1 ? plotW / 2 : (plotW * i) / (points.length - 1));
  const y = (level: OpicLevel) => PAD.top + plotH - (plotH * LEVEL_ORDER.indexOf(level)) / (LEVEL_ORDER.length - 1);
  const path = points.map((p, i) => `${i === 0 ? "M" : "L"}${x(i)},${y(p.estimatedLevel)}`).join(" ");
  const shown = active == null ? null : points[active];

  return (
    <figure className="flex flex-col gap-2">
      <div className="relative">
        <svg
          viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
          className="h-auto w-full overflow-visible"
          role="img"
          aria-label={`예상 레벨 추이: ${points.map((p) => p.estimatedLevel).join(", ")}`}
        >
          {LEVEL_ORDER.map((level) => (
            <g key={level}>
              <line
                x1={PAD.left}
                x2={WIDTH - PAD.right}
                y1={y(level)}
                y2={y(level)}
                className="stroke-border"
                strokeWidth={1}
              />
              <text x={PAD.left - 10} y={y(level)} dy="0.35em" textAnchor="end" className="fill-muted-foreground text-[11px]">
                {level}
              </text>
            </g>
          ))}
          {points.map((p, i) => (
            <text
              key={p.id}
              x={x(i)}
              y={HEIGHT - 8}
              textAnchor="middle"
              className={cn("text-[11px]", p.id === currentId ? "fill-foreground font-semibold" : "fill-muted-foreground")}
            >
              {p.id === currentId ? "이번" : formatShortDate(p.submittedAt)}
            </text>
          ))}
          {points.length > 1 ? (
            <path d={path} fill="none" className="stroke-primary" strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />
          ) : null}
          {points.map((p, i) => (
            <g key={p.id}>
              <circle
                cx={x(i)}
                cy={y(p.estimatedLevel)}
                r={p.id === currentId ? 6 : 4.5}
                className={cn("stroke-card", p.id === currentId ? "fill-primary" : "fill-primary/70")}
                strokeWidth={2}
              />
              {/* A hit area larger than the dot. */}
              <circle
                cx={x(i)}
                cy={y(p.estimatedLevel)}
                r={16}
                fill="transparent"
                tabIndex={0}
                aria-label={`${formatShortDate(p.submittedAt)} ${p.estimatedLevel}`}
                onMouseEnter={() => setActive(i)}
                onMouseLeave={() => setActive(null)}
                onFocus={() => setActive(i)}
                onBlur={() => setActive(null)}
                className="cursor-default outline-none"
              />
            </g>
          ))}
        </svg>
        {shown && active != null ? (
          <div
            className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-[calc(100%+12px)] rounded-xl border bg-popover px-3 py-2 text-xs shadow-card"
            style={{ left: `${(x(active) / WIDTH) * 100}%`, top: `${(y(shown.estimatedLevel) / HEIGHT) * 100}%` }}
          >
            <p className="font-semibold text-foreground">
              {formatShortDate(shown.submittedAt)} · {shown.estimatedLevel}
            </p>
          </div>
        ) : null}
      </div>
      <table className="sr-only">
        <caption>실전연습 예상 레벨 추이</caption>
        <thead>
          <tr>
            <th scope="col">날짜</th>
            <th scope="col">예상 레벨</th>
          </tr>
        </thead>
        <tbody>
          {points.map((p) => (
            <tr key={p.id}>
              <td>{p.id === currentId ? `${formatShortDate(p.submittedAt)} (이번)` : formatShortDate(p.submittedAt)}</td>
              <td>{p.estimatedLevel}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}
