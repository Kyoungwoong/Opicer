"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Download, LoaderCircle, Trash2 } from "lucide-react";
import { ROUTES } from "@/lib/routes";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Eyebrow, SectionHeader } from "@/components/common/SectionHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { TranscriptDiff } from "@/features/practice/components/TranscriptDiff";
import { AnswerEvaluationPanel } from "@/features/practice/components/evaluation/AnswerEvaluationPanel";
import { EvaluationStatusNotice } from "@/features/practice/components/evaluation/EvaluationStatusNotice";
import { SessionReportCard } from "@/features/practice/components/evaluation/SessionReportCard";
import { deletePracticeSession } from "@/features/practice/api";
import type { OpicLevel, PracticeSessionAnswer } from "@/features/practice/types";
import { formatDateTime } from "@/features/practice/history-format";
import { isEvaluationInProgress, useSessionEvaluation } from "@/features/practice/use-session-evaluation";
import { downloadReportHtml } from "@/features/practice/report-export";

type Props = {
  sessionId: string;
  userLabel?: string;
  onLogout?: () => void;
};

const CARD = "flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6";
const PLACEHOLDER = "rounded-2xl border border-dashed bg-secondary px-4 py-4 text-sm text-muted-foreground";

function BackLink() {
  return (
    <Button asChild variant="outline" className="rounded-full px-4">
      <Link href={ROUTES.mypage}>
        <ArrowLeft />
        목록으로
      </Link>
    </Button>
  );
}

/** Free-text results saved by the manual analyze/improve flow, before automatic evaluation. */
function LegacyResult({ answer }: { answer: PracticeSessionAnswer }) {
  return (
    <>
      {answer.analysis && (
        <div className="flex flex-col gap-2">
          <p className="text-xs font-semibold text-muted-foreground">AI 분석</p>
          <p className="rounded-2xl border p-4 text-sm leading-7 whitespace-pre-wrap">{answer.analysis}</p>
        </div>
      )}

      {answer.improvedScript &&
        (answer.transcript ? (
          <TranscriptDiff original={answer.transcript} improved={answer.improvedScript} />
        ) : (
          <div className="flex flex-col gap-2">
            <p className="text-xs font-semibold text-muted-foreground">개선 스크립트</p>
            <p className="rounded-2xl bg-secondary p-4 text-sm leading-7 whitespace-pre-wrap">{answer.improvedScript}</p>
          </div>
        ))}
    </>
  );
}

function AnswerCard({
  answer,
  number,
  targetLevel,
  evaluating,
}: {
  answer: PracticeSessionAnswer;
  number: number;
  targetLevel: OpicLevel | null;
  evaluating: boolean;
}) {
  return (
    <li className={CARD}>
      <div className="flex flex-col gap-1.5">
        <div className="flex items-center justify-between gap-2">
          <Eyebrow>질문 {number}</Eyebrow>
          {answer.overallLevel && (
            <Badge variant="secondary" title="예상 레벨(참고용) · 공식 OPIc 점수가 아닙니다">
              예상 {answer.overallLevel}
            </Badge>
          )}
        </div>
        <p className="font-semibold leading-7">{answer.questionText}</p>
      </div>

      <div className="flex flex-col gap-2">
        <p className="text-xs font-semibold text-muted-foreground">내 답변 · 음성 인식 결과(추정)</p>
        {answer.transcript ? (
          <p className="rounded-2xl bg-secondary p-4 text-sm leading-7">{answer.transcript}</p>
        ) : (
          <p className={PLACEHOLDER}>전사된 답변이 없어요.</p>
        )}
      </div>

      {answer.evaluation ? (
        <div className="border-t pt-5">
          <AnswerEvaluationPanel evaluation={answer.evaluation} transcript={answer.transcript} targetLevel={targetLevel} />
        </div>
      ) : answer.evaluationStatus === "SKIPPED" ? (
        <p className="text-sm text-muted-foreground">답변이 비어 있거나 15단어보다 짧아서 평가하지 않았어요.</p>
      ) : answer.evaluationStatus === "FAILED" ? (
        <p className="text-sm text-destructive">이 답변은 평가하지 못했어요.</p>
      ) : answer.evaluationStatus === "PENDING" && evaluating ? (
        <p className="flex items-center gap-2 text-sm text-muted-foreground">
          <LoaderCircle aria-hidden className="size-4 animate-spin" />
          평가 중…
        </p>
      ) : (
        <LegacyResult answer={answer} />
      )}
    </li>
  );
}

export function PracticeHistoryDetailView({ sessionId, userLabel, onLogout }: Props) {
  const router = useRouter();
  // Polls only while the evaluation is still running; a settled session is fetched once.
  const evaluation = useSessionEvaluation(sessionId);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const session = evaluation.session;

  const handleDelete = async () => {
    if (!window.confirm("이 연습 기록을 삭제할까요? 전사·분석 결과가 모두 지워지고 되돌릴 수 없어요.")) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deletePracticeSession(sessionId);
      router.replace(ROUTES.mypage);
    } catch {
      setDeleteError("삭제하지 못했어요. 잠시 후 다시 시도해 주세요.");
      setDeleting(false);
    }
  };

  // Without a loaded session, a failed poll or a timeout means the page cannot recover by waiting.
  const failedToLoad = !session && (evaluation.pollFailed || evaluation.timedOut);

  return (
    <PageShell nav={<TopNav userLabel={userLabel} onLogout={onLogout} maxWidthClassName="max-w-6xl" />}>
      {!session && !evaluation.missing && !failedToLoad ? (
        <div className="flex flex-col gap-4" aria-busy="true">
          <Skeleton className="h-32 rounded-3xl" />
          <Skeleton className="h-64 rounded-3xl" />
        </div>
      ) : !session ? (
        <section className={CARD}>
          <SectionHeader
            as="h1"
            title={evaluation.missing ? "기록을 찾을 수 없어요" : "기록을 불러오지 못했어요"}
            description={evaluation.missing ? "삭제되었거나 다른 계정의 기록이에요." : "잠시 후 다시 시도해 주세요."}
          />
          <div>
            <BackLink />
          </div>
        </section>
      ) : (
        <>
          <section className={CARD}>
            <SectionHeader
              as="h1"
              title={session.topicTitle}
              description={`${formatDateTime(session.completedAt)} · ${session.answers.length}문항${
                session.targetLevel ? ` · 목표 ${session.targetLevel}` : ""
              }`}
              action={<BackLink />}
            />
            <div className="flex flex-col gap-2 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-xs text-muted-foreground">
                녹음 파일은 저장하지 않아요. 전사와 분석은 참고용 추정 결과예요.
              </p>
              <div className="flex flex-wrap gap-1 self-start sm:self-auto">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => downloadReportHtml(session)}
                  disabled={session.answers.length === 0}
                  title="HTML 파일로 저장해요. 브라우저에서 열어 인쇄하면 PDF로도 저장할 수 있어요."
                  className="rounded-full px-3"
                >
                  <Download />
                  리포트 저장
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  onClick={handleDelete}
                  disabled={deleting}
                  className="rounded-full px-3 text-destructive hover:bg-destructive/10 hover:text-destructive"
                >
                  <Trash2 />
                  {deleting ? "삭제 중…" : "기록 삭제"}
                </Button>
              </div>
            </div>
            {deleteError && (
              <p role="alert" className="text-sm text-destructive">
                {deleteError}
              </p>
            )}
          </section>

          {/* Sessions from before automatic evaluation have no status; they show their saved text below. */}
          {session.evaluationStatus !== null && (
            <EvaluationStatusNotice
              session={session}
              timedOut={evaluation.timedOut}
              pollFailed={evaluation.pollFailed}
              retrying={evaluation.retrying}
              retryError={evaluation.retryError}
              onRetry={evaluation.retry}
              onCheckAgain={evaluation.checkAgain}
            />
          )}

          {session.report && <SessionReportCard report={session.report} />}

          {session.answers.length === 0 ? (
            <div className={PLACEHOLDER}>저장된 답변이 없어요.</div>
          ) : (
            <ol className="flex flex-col gap-4">
              {/* answers arrive sorted by questionOrder, which is 0-based */}
              {session.answers.map((answer, index) => (
                <AnswerCard
                  key={answer.questionId}
                  answer={answer}
                  number={index + 1}
                  targetLevel={session.targetLevel}
                  evaluating={isEvaluationInProgress(session.evaluationStatus)}
                />
              ))}
            </ol>
          )}
        </>
      )}
    </PageShell>
  );
}
