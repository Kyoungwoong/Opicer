"use client";

import { diffWords } from "diff";

type Props = {
  original: string;
  improved: string;
};

export function TranscriptDiff({ original, improved }: Props) {
  const changes = diffWords(original, improved);

  return (
    <div className="rounded-2xl border border-black/10 bg-white/80 p-4 text-sm leading-relaxed">
      <p className="mb-2 text-xs font-semibold uppercase tracking-[0.2em] text-[var(--muted)]">
        개선 스크립트
      </p>
      <p className="flex flex-wrap gap-y-1">
        {changes.map((change, idx) => {
          if (change.removed) {
            return (
              <span
                key={idx}
                className="line-through text-red-500 opacity-70 mr-1"
              >
                {change.value}
              </span>
            );
          }
          if (change.added) {
            return (
              <span
                key={idx}
                className="bg-green-100 text-green-800 rounded px-0.5 mr-1"
              >
                {change.value}
              </span>
            );
          }
          return (
            <span key={idx} className="mr-1">
              {change.value}
            </span>
          );
        })}
      </p>
    </div>
  );
}
