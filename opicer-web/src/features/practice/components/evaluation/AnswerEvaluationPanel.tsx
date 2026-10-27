import { CheckCircle2, MessageSquareQuote } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Eyebrow } from "@/components/common/SectionHeader";
import { TranscriptDiff } from "@/features/practice/components/TranscriptDiff";
import { CATEGORY_LABELS, DIMENSION_LABELS, DIMENSION_ORDER } from "@/features/practice/evaluation-labels";
import type { AnswerEvaluation, OpicLevel } from "@/features/practice/types";

type Props = {
  evaluation: AnswerEvaluation;
  /** The learner's transcript, diffed against the target-level script. */
  transcript: string | null;
  targetLevel: OpicLevel | null;
  /** "coach" (topic practice) shows only fixes, the target script and expressions — no levels. */
  variant?: "full" | "coach";
};

const SECTION = "flex flex-col gap-2.5";

function signalsLine(signals: AnswerEvaluation["signals"]): string {
  const parts = [`${signals.wordCount}단어`, `${signals.sentenceCount}문장`];
  if (signals.wordsPerMinute != null) parts.push(`분당 ${Math.round(signals.wordsPerMinute)}단어`);
  parts.push(`어휘 다양성 ${signals.lexicalDiversity.toFixed(2)}`);
  return parts.join(" · ");
}

/** One answer's evaluation in a fixed layout; all text comes from the stored evaluation document. */
export function AnswerEvaluationPanel({ evaluation, transcript, targetLevel, variant = "full" }: Props) {
  const full = variant === "full";
  const dimensions = [...(evaluation.dimensions ?? [])].sort(
    (a, b) => DIMENSION_ORDER.indexOf(a.code) - DIMENSION_ORDER.indexOf(b.code)
  );
  const strengths = evaluation.strengths ?? [];

  return (
    <div className="flex flex-col gap-5">
      {full && evaluation.overallLevel && (
        <div className="flex flex-wrap items-center gap-2">
          <Badge className="h-6 px-2.5 text-sm font-bold">{evaluation.overallLevel}</Badge>
          <span className="text-sm font-semibold">이 답변의 예상 레벨(참고용)</span>
          <span className="text-xs text-muted-foreground">{signalsLine(evaluation.signals)}</span>
        </div>
      )}

      {full && dimensions.length > 0 && (
      <div className={SECTION}>
        <Eyebrow>역량별 평가</Eyebrow>
        <ul className="grid gap-3 sm:grid-cols-2">
          {dimensions.map((dimension) => (
            <li key={dimension.code} className="flex flex-col gap-1.5 rounded-2xl border p-4">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-semibold">{DIMENSION_LABELS[dimension.code]}</span>
                <span className="text-sm font-bold tabular-nums text-primary-strong">{dimension.level}</span>
              </div>
              <p className="text-sm leading-relaxed text-muted-foreground">{dimension.commentKo}</p>
              {dimension.evidence && (
                <p className="flex gap-1.5 text-xs leading-relaxed text-muted-foreground">
                  <MessageSquareQuote aria-hidden className="mt-0.5 size-3.5 shrink-0" />
                  <span className="italic">&ldquo;{dimension.evidence}&rdquo;</span>
                </p>
              )}
            </li>
          ))}
        </ul>
      </div>
      )}

      {full && strengths.length > 0 && (
        <div className={SECTION}>
          <Eyebrow>잘한 점</Eyebrow>
          <ul className="flex flex-col gap-1.5">
            {strengths.map((strength, index) => (
              <li key={index} className="flex gap-2 text-sm leading-relaxed">
                <CheckCircle2 aria-hidden className="mt-0.5 size-4 shrink-0 text-success" />
                {strength}
              </li>
            ))}
          </ul>
        </div>
      )}

      {evaluation.corrections.length > 0 && (
        <div className={SECTION}>
          <Eyebrow>고쳐 보면 좋은 부분</Eyebrow>
          <ul className="flex flex-col gap-2.5">
            {evaluation.corrections.map((correction, index) => (
              <li key={index} className="flex flex-col gap-1.5 rounded-2xl bg-secondary p-4">
                <div className="flex flex-wrap items-start gap-2">
                  <Badge variant="outline" className="bg-card">
                    {CATEGORY_LABELS[correction.category]}
                  </Badge>
                  <p className="min-w-0 flex-1 text-sm leading-relaxed">
                    <del className="text-destructive/80 decoration-destructive/60">{correction.original}</del>
                    <span aria-hidden className="mx-2 text-muted-foreground">→</span>
                    <span className="sr-only">고친 표현: </span>
                    <ins className="font-medium text-success no-underline">{correction.corrected}</ins>
                  </p>
                </div>
                <p className="text-xs leading-relaxed text-muted-foreground">{correction.reasonKo}</p>
              </li>
            ))}
          </ul>
        </div>
      )}

      {evaluation.targetScript &&
        (transcript ? (
          <TranscriptDiff
            original={transcript}
            improved={evaluation.targetScript}
            title={full ? (targetLevel ? `${targetLevel} 목표 스크립트` : "목표 스크립트") : "추천 답변"}
          />
        ) : (
          <div className={SECTION}>
            <Eyebrow>{full ? "목표 스크립트" : "추천 답변"}</Eyebrow>
            <p className="rounded-2xl bg-secondary p-4 text-sm leading-7 whitespace-pre-wrap">
              {evaluation.targetScript}
            </p>
          </div>
        ))}

      {evaluation.keyExpressions.length > 0 && (
        <div className={SECTION}>
          <Eyebrow>추천 표현</Eyebrow>
          <ul className="flex flex-col gap-2">
            {evaluation.keyExpressions.map((item, index) => (
              <li key={index} className="flex flex-col gap-0.5 rounded-2xl border px-4 py-3 sm:flex-row sm:items-baseline sm:gap-3">
                <span className="text-sm font-semibold">{item.expression}</span>
                <span className="text-xs text-muted-foreground">{item.meaningKo}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
