import { Info, Lightbulb, Target } from "lucide-react";
import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Eyebrow } from "@/components/common/SectionHeader";
import {
  CATEGORY_LABELS,
  CONFIDENCE_LABELS,
  DIMENSION_LABELS,
  DIMENSION_ORDER,
  LEVEL_DISCLAIMER,
  LEVEL_ORDER,
  gapLabel,
  levelRank,
} from "@/features/practice/evaluation-labels";
import type { SessionReport } from "@/features/practice/types";

type Props = {
  report: SessionReport;
  className?: string;
};

const SECTION = "flex flex-col gap-3 border-t pt-5";

/**
 * Fixed layout for the session report: the server sends only numbers and codes, and every sentence
 * here comes from this template or the report's fixed tips.
 */
export function SessionReportCard({ report, className }: Props) {
  const range =
    report.lowestLevel === report.highestLevel
      ? report.estimatedLevel
      : `${report.lowestLevel} ~ ${report.highestLevel}`;

  return (
    <section
      aria-labelledby="session-report-title"
      className={cn("flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6", className)}
    >
      {/* Headline */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <span className="grid size-20 shrink-0 place-items-center rounded-3xl bg-accent text-3xl font-extrabold text-primary-strong">
            {report.estimatedLevel}
          </span>
          <div className="flex flex-col gap-1">
            <Eyebrow>예상 레벨(참고용)</Eyebrow>
            <h2 id="session-report-title" className="text-xl font-extrabold tracking-tight">
              이번 연습은 {report.estimatedLevel} 수준으로 보여요
            </h2>
            <p className="text-sm text-muted-foreground">
              답변별 범위 {range} · 평가한 답변 {report.evaluatedAnswers}개
            </p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2 sm:flex-col sm:items-end">
          <Badge variant="secondary" className="h-6 px-2.5">
            <Target aria-hidden />
            {gapLabel(report.gapToTarget, report.targetLevel)}
          </Badge>
          <Badge variant="outline" className="h-6 px-2.5">
            {CONFIDENCE_LABELS[report.confidence]}
          </Badge>
        </div>
      </div>

      {(report.skippedAnswers > 0 || report.failedAnswers > 0) && (
        <p className="rounded-2xl bg-secondary px-4 py-3 text-sm text-muted-foreground">
          {[
            report.skippedAnswers > 0 ? `짧거나 비어 있는 답변 ${report.skippedAnswers}개는 평가에서 뺐어요.` : null,
            report.failedAnswers > 0 ? `답변 ${report.failedAnswers}개는 평가하지 못했어요.` : null,
          ]
            .filter(Boolean)
            .join(" ")}
        </p>
      )}

      {/* Competencies */}
      <div className={SECTION}>
        <Eyebrow>역량별 예상 레벨</Eyebrow>
        <ul className="flex flex-col gap-3">
          {DIMENSION_ORDER.map((code) => {
            const level = report.dimensionLevels[code];
            if (!level) return null;
            const reachesTarget = levelRank(level) >= levelRank(report.targetLevel);
            return (
              <li key={code} className="grid grid-cols-[5.5rem_minmax(0,1fr)_2.5rem] items-center gap-3 text-sm">
                <span className="font-medium">{DIMENSION_LABELS[code]}</span>
                <span
                  aria-hidden
                  className="relative h-2.5 overflow-hidden rounded-full bg-muted"
                >
                  <span
                    className={cn("absolute inset-y-0 left-0 rounded-full", reachesTarget ? "bg-primary" : "bg-primary/50")}
                    style={{ width: `${(levelRank(level) / LEVEL_ORDER.length) * 100}%` }}
                  />
                  {/* Target marker */}
                  <span
                    className="absolute inset-y-0 w-0.5 bg-foreground/60"
                    style={{ left: `calc(${(levelRank(report.targetLevel) / LEVEL_ORDER.length) * 100}% - 1px)` }}
                  />
                </span>
                <span className="text-right font-bold tabular-nums">{level}</span>
              </li>
            );
          })}
        </ul>
        <p className="text-xs text-muted-foreground">세로선은 목표 레벨({report.targetLevel})이에요.</p>
      </div>

      {/* Recurring issues */}
      {report.topIssues.length > 0 && (
        <div className={SECTION}>
          <Eyebrow>자주 나온 실수</Eyebrow>
          <ol className="flex flex-col gap-3">
            {report.topIssues.map((issue, index) => (
              <li key={issue.category} className="flex flex-col gap-2 rounded-2xl bg-secondary p-4">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm font-bold">
                    {index + 1}. {CATEGORY_LABELS[issue.category]}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    답변 {issue.answerCount}개에서 {issue.totalCount}번
                  </span>
                </div>
                {issue.example && (
                  <p className="text-sm leading-relaxed">
                    <del className="text-destructive/80 decoration-destructive/60">{issue.example.original}</del>
                    <span aria-hidden className="mx-2 text-muted-foreground">→</span>
                    <span className="sr-only">고친 표현: </span>
                    <ins className="text-success no-underline">{issue.example.corrected}</ins>
                  </p>
                )}
              </li>
            ))}
          </ol>
        </div>
      )}

      {/* Tips */}
      {report.tips.length > 0 && (
        <div className={SECTION}>
          <Eyebrow>다음 레벨을 위한 팁</Eyebrow>
          <ul className="flex flex-col gap-2">
            {report.tips.map((tip, index) => (
              <li key={index} className="flex gap-2 text-sm leading-relaxed">
                <Lightbulb aria-hidden className="mt-0.5 size-4 shrink-0 text-primary-strong" />
                {tip}
              </li>
            ))}
          </ul>
        </div>
      )}

      <p className="flex items-start gap-2 border-t pt-4 text-xs text-muted-foreground">
        <Info aria-hidden className="mt-0.5 size-3.5 shrink-0" />
        {report.disclaimer || LEVEL_DISCLAIMER}. 음성 인식 결과(추정)를 바탕으로 한 연습용 결과예요.
      </p>
    </section>
  );
}
