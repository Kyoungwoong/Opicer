import Link from "next/link";
import { AlertCircle, Info, LoaderCircle, RefreshCw, RotateCcw } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { isEvaluationInProgress } from "@/features/practice/use-session-evaluation";
import type { AnswerEvaluationStatus, SessionEvaluationStatus } from "@/features/practice/types";

/** What the notice needs from a practice session or a mock test (whose items are passed as `answers`). */
export type EvaluationProgress = {
  evaluationStatus: SessionEvaluationStatus | null;
  answers: { evaluationStatus: AnswerEvaluationStatus | null }[];
};

type Props = {
  session: EvaluationProgress;
  timedOut: boolean;
  pollFailed: boolean;
  retrying: boolean;
  retryError: string | null;
  /** Link to the history page; omit when already on it. */
  historyHref?: string;
  onRetry: () => void;
  onCheckAgain: () => void;
};

/** Session-level evaluation state above the report. Renders nothing once the evaluation is DONE. */
export function EvaluationStatusNotice({
  session,
  timedOut,
  pollFailed,
  retrying,
  retryError,
  historyHref,
  onRetry,
  onCheckAgain,
}: Props) {
  const status = session.evaluationStatus;

  const retryButton = (
    <Button type="button" variant="outline" size="sm" onClick={onRetry} disabled={retrying} className="rounded-full px-4">
      {retrying ? <LoaderCircle className="animate-spin" /> : <RotateCcw />}
      {retrying ? "요청 중…" : "다시 평가하기"}
    </Button>
  );
  const retryErrorLine = retryError ? (
    <p role="alert" className="text-sm text-destructive">
      {retryError}
    </p>
  ) : null;

  if (isEvaluationInProgress(status)) {
    const evaluable = session.answers.filter((a) => a.evaluationStatus && a.evaluationStatus !== "SKIPPED");
    const settled = evaluable.filter((a) => a.evaluationStatus === "DONE" || a.evaluationStatus === "FAILED");

    if (timedOut) {
      return (
        <Alert>
          <Info />
          <AlertTitle>평가가 평소보다 오래 걸리고 있어요</AlertTitle>
          <AlertDescription className="flex flex-col gap-3">
            <p>
              평가는 계속 진행돼요.
              {historyHref ? " 이 화면을 닫아도 마이페이지 기록에서 결과를 확인할 수 있어요." : " 잠시 후 다시 확인해 주세요."}
            </p>
            <div className="flex flex-wrap gap-2">
              <Button type="button" variant="outline" size="sm" onClick={onCheckAgain} className="rounded-full px-4">
                <RefreshCw />
                다시 확인
              </Button>
              {historyHref && (
                <Button asChild variant="ghost" size="sm" className="rounded-full px-4">
                  <Link href={historyHref}>마이페이지 기록에서 보기</Link>
                </Button>
              )}
            </div>
          </AlertDescription>
        </Alert>
      );
    }

    return (
      <section
        role="status"
        className="flex items-center gap-4 rounded-3xl border bg-card p-5 shadow-card sm:p-6"
      >
        <LoaderCircle aria-hidden className="size-7 shrink-0 animate-spin text-primary-strong" />
        <div className="flex flex-col gap-1">
          <p className="font-semibold">
            AI가 답변을 평가하고 있어요
            {evaluable.length > 0 ? ` (${settled.length}/${evaluable.length})` : ""}
          </p>
          <p className="text-sm text-muted-foreground">
            보통 1~2분 걸려요.{historyHref ? " 화면을 닫아도 마이페이지 기록에서 결과를 볼 수 있어요." : ""}
            {pollFailed ? " 연결이 잠시 끊겨 다시 확인하는 중이에요." : ""}
          </p>
        </div>
      </section>
    );
  }

  // Every answer was skipped (empty or under 15 words): a retry cannot change the result.
  const nothingToEvaluate =
    status === null || (status === "FAILED" && session.answers.every((a) => a.evaluationStatus === "SKIPPED"));

  if (nothingToEvaluate) {
    return (
      <Alert>
        <Info />
        <AlertTitle>평가할 답변이 없어요</AlertTitle>
        <AlertDescription>
          답변이 녹음·전사되지 않았거나 모두 너무 짧았어요. 15단어 이상 말하면 평가할 수 있어요.
        </AlertDescription>
      </Alert>
    );
  }

  if (status === "FAILED") {
    return (
      <Alert variant="destructive">
        <AlertCircle />
        <AlertTitle>답변을 평가하지 못했어요</AlertTitle>
        <AlertDescription className="flex flex-col gap-3">
          <p>일시적인 문제일 수 있어요. 추가 크레딧 없이 다시 평가할 수 있어요.</p>
          <div>{retryButton}</div>
          {retryErrorLine}
        </AlertDescription>
      </Alert>
    );
  }

  if (status === "PARTIAL") {
    return (
      <Alert>
        <Info />
        <AlertTitle>일부 답변을 평가하지 못했어요</AlertTitle>
        <AlertDescription className="flex flex-col gap-3">
          <p>평가한 답변의 결과는 아래에 있어요. 못 한 답변만 추가 크레딧 없이 다시 평가할 수 있어요.</p>
          <div>{retryButton}</div>
          {retryErrorLine}
        </AlertDescription>
      </Alert>
    );
  }

  return null;
}
