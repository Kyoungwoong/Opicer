"use client";

import { diffWords } from "diff";

type Props = {
  original: string;
  improved: string;
  title?: string;
};

export function TranscriptDiff({ original, improved, title = "개선 스크립트" }: Props) {
  const changes = diffWords(original, improved);

  return (
    <div className="rounded-2xl border bg-secondary p-4 text-sm leading-7">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <p className="text-xs font-semibold text-primary-strong">{title}</p>
        <p className="flex items-center gap-3 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1">
            <span aria-hidden className="rounded bg-destructive/10 px-1 text-destructive line-through">
              가
            </span>
            삭제
          </span>
          <span className="inline-flex items-center gap-1">
            <span aria-hidden className="rounded bg-success-subtle px-1 text-success underline decoration-2 underline-offset-2">
              가
            </span>
            추가
          </span>
        </p>
      </div>
      <p>
        {changes.map((change, idx) => {
          if (change.removed) {
            return (
              <del key={idx} className="rounded bg-destructive/10 px-0.5 text-destructive/80 decoration-destructive/60">
                {change.value}
              </del>
            );
          }
          if (change.added) {
            return (
              <ins
                key={idx}
                className="rounded bg-success-subtle px-0.5 text-success underline decoration-success/60 decoration-2 underline-offset-2"
              >
                {change.value}
              </ins>
            );
          }
          return <span key={idx}>{change.value}</span>;
        })}
      </p>
    </div>
  );
}
