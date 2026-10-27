"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Download, LineChart, LoaderCircle, Trash2 } from "lucide-react";
import { ROUTES } from "@/lib/routes";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Eyebrow, SectionHeader } from "@/components/common/SectionHeader";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { AnswerEvaluationPanel } from "@/features/practice/components/evaluation/AnswerEvaluationPanel";
import { EvaluationStatusNotice } from "@/features/practice/components/evaluation/EvaluationStatusNotice";
import { SessionReportCard } from "@/features/practice/components/evaluation/SessionReportCard";
import { submitTopicSelection } from "@/features/practice/api";
import { DEFAULT_TARGET_LEVEL, isTargetLevel } from "@/features/practice/evaluation-labels";
import { formatDateTime } from "@/features/practice/history-format";
import { isEvaluationInProgress, useEvaluationPolling } from "@/features/practice/use-session-evaluation";
import { deleteMockTest, fetchMockTest, retryMockEvaluation } from "@/features/mock/api";
import { PRACTICE_NOTICE, QUESTION_TYPE_LABELS, formatClock } from "@/features/mock/exam";
import { downloadMockReportHtml } from "@/features/mock/report-export";
import type { MockTestDetail, MockTestItemDetail } from "@/features/mock/types";
import { LevelTrendChart } from "@/features/mock/components/result/LevelTrendChart";
import { HabitsCard, RecommendationsCard, TimeCard, TypeDiagnosisCard } from "@/features/mock/components/result/InsightCards";

type Props = { mockTestId: string; userLabel?: string; onLogout?: () => void };

const CARD = "flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6";

function BackLink() {
  return (
    <Button asChild variant="outline" className="rounded-full px-4">
      <Link href={`${ROUTES.mypage}?tab=mock`}>
        <ArrowLeft />
        목록으로
      </Link>
    </Button>
  );
}

function ItemCard({ item, test, evaluating }: { item: MockTestItemDetail; test: MockTestDetail; evaluating: boolean }) {
  const type = item.questionType ? QUESTION_TYPE_LABELS[item.questionType] ?? null : null;
  return (
    <li className={CARD}>
      <div className="flex flex-col gap-1.5">
        <Eyebrow>
          질문 {item.order}
          {type ? ` · ${type}` : ""}
          {item.topicTitle ? ` · ${item.topicTitle}` : ""}
          {item.durationSeconds != null ? ` · ${formatClock(item.durationSeconds)}` : ""}
        </Eyebrow>
        <p className="font-semibold leading-7">{item.questionText}</p>
      </div>
      <div className="flex flex-col gap-2">
        <p className="text-xs font-semibold text-muted-foreground">내 답변 · 음성 인식 결과(추정)</p>
        {item.transcript ? (
          <p className="rounded-2xl bg-secondary p-4 text-sm leading-7">{item.transcript}</p>
        ) : (
          <p className="rounded-2xl border border-dashed bg-secondary px-4 py-4 text-sm text-muted-foreground">
            답변이 없어요.
          </p>
        )}
      </div>
      {item.evaluation ? (
        <div className="border-t pt-5">
          <AnswerEvaluationPanel evaluation={item.evaluation} transcript={item.transcript} targetLevel={test.targetLevel} />
        </div>
      ) : item.evaluationStatus === "SKIPPED" ? (
        <p className="text-sm text-muted-foreground">답변이 비어 있거나 15단어보다 짧아서 평가하지 않았어요.</p>
      ) : item.evaluationStatus === "FAILED" ? (
        <p className="text-sm text-destructive">이 답변은 평가하지 못했어요.</p>
      ) : item.evaluationStatus === "PENDING" && evaluating ? (
        <p className="flex items-center gap-2 text-sm text-muted-foreground">
          <LoaderCircle aria-hidden className="size-4 animate-spin" />
          평가 중…
        </p>
      ) : null}
    </li>
  );
}

/** A submitted mock test: evaluation progress, the report, extra feedback, the level trend and every answer. */
export function MockResultView({ mockTestId, userLabel, onLogout }: Props) {
  const router = useRouter();
  const evaluation = useEvaluationPolling(mockTestId, fetchMockTest, retryMockEvaluation);
  const test = evaluation.session;
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [startingTopicId, setStartingTopicId] = useState<string | null>(null);
  const [practiceError, setPracticeError] = useState<string | null>(null);

  const handleDelete = async () => {
    if (!window.confirm("이 실전연습 기록을 삭제할까요? 답변·평가·피드백이 모두 지워지고 되돌릴 수 없어요.")) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteMockTest(mockTestId);
      router.replace(`${ROUTES.mypage}?tab=mock`);
    } catch {
      setDeleteError("삭제하지 못했어요. 잠시 후 다시 시도해 주세요.");
      setDeleting(false);
    }
  };

  const practice = async (topicId: string) => {
    setStartingTopicId(topicId);
    setPracticeError(null);
    try {
      const selection = await submitTopicSelection(topicId);
      const level = test?.targetLevel;
      const target = isTargetLevel(level) ? level : DEFAULT_TARGET_LEVEL;
      router.push(`${ROUTES.practiceSession(topicId)}?selectionId=${selection.id}&target=${target}`);
    } catch (err) {
      setPracticeError(err instanceof Error ? err.message : "연습을 시작하지 못했어요.");
      setStartingTopicId(null);
    }
  };

  const failedToLoad = !test && (evaluation.pollFailed || evaluation.timedOut);
  const nav = <TopNav userLabel={userLabel} onLogout={onLogout} maxWidthClassName="max-w-6xl" />;

  if (!test) {
    return (
      <PageShell nav={nav}>
        {!evaluation.missing && !failedToLoad ? (
          <div className="flex flex-col gap-4" aria-busy="true">
            <Skeleton className="h-32 rounded-3xl" />
            <Skeleton className="h-64 rounded-3xl" />
          </div>
        ) : (
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
        )}
      </PageShell>
    );
  }

  const evaluating = isEvaluationInProgress(test.evaluationStatus);
  const parts = [1, 2]
    .map((part) => ({ part, items: test.items.filter((item) => item.part === part) }))
    .filter((group) => group.items.length > 0);

  return (
    <PageShell nav={nav}>
      <section className={CARD}>
        <SectionHeader
          as="h1"
          eyebrow="실전연습 결과"
          title={test.estimatedLevel ? `예상 레벨 ${test.estimatedLevel}` : evaluating ? "평가 중이에요" : "실전연습 기록"}
          description={`${formatDateTime(test.submittedAt)} · ${test.items.length}문항${
            test.targetLevel ? ` · 목표 ${test.targetLevel}` : ""
          }${test.examSeconds != null ? ` · 시험 시간 ${formatClock(test.examSeconds)}` : ""}`}
          action={<BackLink />}
        />
        <div className="flex flex-col gap-2 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-muted-foreground">{PRACTICE_NOTICE} 녹음 파일은 저장하지 않아요.</p>
          <div className="flex flex-wrap gap-1 self-start sm:self-auto">
            <Button
              type="button"
              variant="ghost"
              onClick={() => downloadMockReportHtml(test)}
              disabled={evaluating}
              title="HTML 파일로 저장해요. 브라우저에서 열어 인쇄하면 PDF로도 저장할 수 있어요."
              className="rounded-full px-3"
            >
              <Download />
              결과 저장
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
        {deleteError ? (
          <p role="alert" className="text-sm text-destructive">
            {deleteError}
          </p>
        ) : null}
      </section>

      <EvaluationStatusNotice
        session={{ evaluationStatus: test.evaluationStatus, answers: test.items }}
        timedOut={evaluation.timedOut}
        pollFailed={evaluation.pollFailed}
        retrying={evaluation.retrying}
        retryError={evaluation.retryError}
        onRetry={evaluation.retry}
        onCheckAgain={evaluation.checkAgain}
      />

      {test.report ? <SessionReportCard report={test.report} /> : null}

      {test.trend.length > 0 ? (
        <section className={CARD}>
          <div className="flex items-start gap-3">
            <span className="grid size-9 shrink-0 place-items-center rounded-full bg-accent text-primary-strong">
              <LineChart aria-hidden className="size-4" />
            </span>
            <div className="flex flex-col gap-0.5">
              <h2 className="text-lg font-bold">성장 추이</h2>
              <p className="text-sm text-muted-foreground">
                {test.trend.length > 1
                  ? `최근 실전연습 ${test.trend.length}번의 예상 레벨(참고용)이에요.`
                  : "실전연습을 한 번 더 보면 예상 레벨이 어떻게 바뀌는지 여기에 그려져요."}
              </p>
            </div>
          </div>
          <LevelTrendChart points={test.trend} currentId={test.id} />
        </section>
      ) : null}

      {test.insights ? (
        <div className="grid gap-4 lg:grid-cols-2">
          <TypeDiagnosisCard insights={test.insights} />
          <HabitsCard insights={test.insights} />
          <TimeCard insights={test.insights} />
          <RecommendationsCard
            insights={test.insights}
            startingTopicId={startingTopicId}
            error={practiceError}
            onPractice={(topicId) => void practice(topicId)}
          />
        </div>
      ) : null}

      {parts.map((group) => (
        <section key={group.part} className="flex flex-col gap-4">
          <SectionHeader title={group.part === 1 ? "1부 · 자기소개와 설문 주제" : "2부 · 돌발, 롤플레이, 고난도"} />
          <ol className="flex flex-col gap-4">
            {group.items.map((item) => (
              <ItemCard key={item.id} item={item} test={test} evaluating={evaluating} />
            ))}
          </ol>
        </section>
      ))}
    </PageShell>
  );
}
