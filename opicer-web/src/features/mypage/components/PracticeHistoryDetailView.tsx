"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Trash2 } from "lucide-react";
import { ROUTES } from "@/lib/routes";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Eyebrow, SectionHeader } from "@/components/common/SectionHeader";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { TranscriptDiff } from "@/features/practice/components/TranscriptDiff";
import { deletePracticeSession, fetchPracticeSession } from "@/features/practice/api";
import type { PracticeSessionAnswer, PracticeSessionDetail } from "@/features/practice/types";
import { formatDateTime } from "@/features/practice/history-format";

type Props = {
  sessionId: string;
  userLabel?: string;
  onLogout?: () => void;
};

type DetailState =
  | { status: "loading" }
  | { status: "error" }
  | { status: "not-found" }
  | { status: "ready"; session: PracticeSessionDetail };

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

function AnswerCard({ answer }: { answer: PracticeSessionAnswer }) {
  return (
    <li className={CARD}>
      <div className="flex flex-col gap-1.5">
        <Eyebrow>질문 {answer.questionOrder}</Eyebrow>
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
    </li>
  );
}

export function PracticeHistoryDetailView({ sessionId, userLabel, onLogout }: Props) {
  const router = useRouter();
  const [state, setState] = useState<DetailState>({ status: "loading" });
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    fetchPracticeSession(sessionId)
      .then((session) => {
        if (!alive) return;
        setState(session ? { status: "ready", session } : { status: "not-found" });
      })
      .catch(() => alive && setState({ status: "error" }));
    return () => {
      alive = false;
    };
  }, [sessionId]);

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

  return (
    <PageShell nav={<TopNav userLabel={userLabel} onLogout={onLogout} maxWidthClassName="max-w-6xl" />}>
      {state.status === "loading" ? (
        <div className="flex flex-col gap-4" aria-busy="true">
          <Skeleton className="h-32 rounded-3xl" />
          <Skeleton className="h-64 rounded-3xl" />
        </div>
      ) : state.status !== "ready" ? (
        <section className={CARD}>
          <SectionHeader
            as="h1"
            title={state.status === "not-found" ? "기록을 찾을 수 없어요" : "기록을 불러오지 못했어요"}
            description={
              state.status === "not-found"
                ? "삭제되었거나 다른 계정의 기록이에요."
                : "잠시 후 다시 시도해 주세요."
            }
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
              title={state.session.topicTitle}
              description={`${formatDateTime(state.session.completedAt)} · ${state.session.answers.length}문항`}
              action={<BackLink />}
            />
            <div className="flex flex-col gap-2 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-xs text-muted-foreground">
                녹음 파일은 저장하지 않아요. 전사와 분석은 참고용 추정 결과예요.
              </p>
              <Button
                type="button"
                variant="ghost"
                onClick={handleDelete}
                disabled={deleting}
                className="self-start rounded-full px-3 text-destructive hover:bg-destructive/10 hover:text-destructive sm:self-auto"
              >
                <Trash2 />
                {deleting ? "삭제 중…" : "기록 삭제"}
              </Button>
            </div>
            {deleteError && (
              <p role="alert" className="text-sm text-destructive">
                {deleteError}
              </p>
            )}
          </section>

          {state.session.answers.length === 0 ? (
            <div className={PLACEHOLDER}>저장된 답변이 없어요.</div>
          ) : (
            <ol className="flex flex-col gap-4">
              {state.session.answers.map((answer) => (
                <AnswerCard key={answer.questionId} answer={answer} />
              ))}
            </ol>
          )}
        </>
      )}
    </PageShell>
  );
}
