"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/routes";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Eyebrow, SectionHeader } from "@/components/common/SectionHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchPracticeSessions } from "@/features/practice/api";
import type { PracticeSessionSummary } from "@/features/practice/types";
import { HISTORY_STATUS_DOT, formatDateTime, historyPath, historyStatus } from "@/features/practice/history-format";

type Props = {
  userLabel?: string;
  onLogout?: () => void;
};

type PracticeHistoryType = "topic" | "mock";

type HistoryState =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; sessions: PracticeSessionSummary[] };

const HISTORY_LIMIT = 50;

const SECTION_META: Record<PracticeHistoryType, { label: string; helper: string }> = {
  topic: {
    label: "주제별 연습하기",
    helper: "선택한 주제 기반으로 연습한 기록을 확인할 수 있어요.",
  },
  mock: {
    label: "실전 연습하기",
    helper: "시간 제한을 고려한 실전 연습 기록을 모아 보여줘요.",
  },
};

const CARD = "flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6";
const PLACEHOLDER = "rounded-2xl border border-dashed bg-secondary px-4 py-4 text-sm text-muted-foreground";

function CategoryList({
  activeType,
  onChange,
  topicCount,
}: {
  activeType: PracticeHistoryType;
  onChange: (type: PracticeHistoryType) => void;
  topicCount: string | null;
}) {
  return (
    <nav aria-label="연습 종류" className="-mx-4 px-4 lg:mx-0 lg:rounded-3xl lg:border lg:bg-card lg:p-5 lg:shadow-card">
      <Eyebrow className="mb-3 hidden lg:block">
        연습 종류
      </Eyebrow>
      <ul className="flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none] lg:flex-col lg:overflow-visible lg:pb-0">
        {(["topic", "mock"] as const).map((type) => {
          const isActive = activeType === type;
          return (
            <li key={type} className="shrink-0 lg:shrink">
              <button
                type="button"
                aria-pressed={isActive}
                onClick={() => onChange(type)}
                className={cn(
                  "flex w-full flex-col gap-1.5 rounded-full border px-4 py-2 text-left whitespace-nowrap transition-colors lg:rounded-2xl lg:py-3.5 lg:whitespace-normal",
                  isActive ? "border-primary bg-accent text-accent-foreground" : "bg-card hover:border-primary/40"
                )}
              >
                <span className="flex items-center justify-between gap-3">
                  <span className="text-sm font-semibold">{SECTION_META[type].label}</span>
                  {type === "mock" ? (
                    <Badge variant="secondary">준비 중</Badge>
                  ) : topicCount !== null ? (
                    <Badge variant={isActive ? "default" : "secondary"}>{topicCount}건</Badge>
                  ) : null}
                </span>
                <span className="hidden text-sm text-muted-foreground lg:block">{SECTION_META[type].helper}</span>
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

function HistoryList({ state }: { state: HistoryState }) {
  if (state.status === "loading") {
    return (
      <div className="flex flex-col gap-2" aria-busy="true">
        {Array.from({ length: 3 }, (_, i) => (
          <Skeleton key={i} className="h-20 rounded-2xl" />
        ))}
      </div>
    );
  }

  if (state.status === "error") {
    return <div className={PLACEHOLDER}>연습 기록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</div>;
  }

  if (state.sessions.length === 0) {
    return (
      <div className="flex flex-col items-start gap-3 rounded-2xl border border-dashed bg-secondary px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-muted-foreground">아직 연습 기록이 없어요.</p>
        <Button asChild className="rounded-full px-5">
          <Link href={ROUTES.practice}>첫 연습 시작하기</Link>
        </Button>
      </div>
    );
  }

  return (
    <ul className="flex flex-col gap-2">
      {state.sessions.map((session) => {
        const status = historyStatus(session);
        return (
          <li key={session.id}>
            <Link
              href={historyPath(session.id)}
              className="group flex w-full flex-col gap-3 rounded-2xl border bg-secondary px-4 py-4 transition-colors hover:border-primary/40 hover:bg-card"
            >
              <span className="flex items-start justify-between gap-4">
                <span className="min-w-0 text-sm font-semibold text-foreground">{session.topicTitle}</span>
                <span className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground">
                  <span aria-hidden className={cn("size-2 rounded-full", HISTORY_STATUS_DOT[status.tone])} />
                  {status.label}
                </span>
              </span>
              <span className="flex items-center justify-between text-xs text-muted-foreground">
                <span>
                  {formatDateTime(session.completedAt)} · {session.answerCount}문항
                </span>
                <span className="inline-flex items-center gap-0.5 font-medium text-primary group-hover:underline">
                  자세히 보기
                  <ChevronRight aria-hidden className="size-3.5" />
                </span>
              </span>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

export function MyPageView({ userLabel, onLogout }: Props) {
  const [activeType, setActiveType] = useState<PracticeHistoryType>("topic");
  const [history, setHistory] = useState<HistoryState>({ status: "loading" });

  useEffect(() => {
    let alive = true;
    fetchPracticeSessions(HISTORY_LIMIT)
      .then((sessions) => alive && setHistory({ status: "ready", sessions }))
      .catch(() => alive && setHistory({ status: "error" }));
    return () => {
      alive = false;
    };
  }, []);

  // The list is capped at HISTORY_LIMIT, so a full page means "at least that many".
  const topicCount =
    history.status === "ready"
      ? `${history.sessions.length}${history.sessions.length >= HISTORY_LIMIT ? "+" : ""}`
      : null;

  return (
    <PageShell nav={<TopNav userLabel={userLabel} onLogout={onLogout} maxWidthClassName="max-w-6xl" />}>
      <section className="rounded-3xl border bg-card p-6 shadow-card sm:p-8">
        <SectionHeader
          as="h1"
          title="연습 히스토리"
          description="연습한 답변의 전사, 분석, 개선 스크립트를 다시 확인할 수 있어요."
        />
      </section>

      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-[240px_minmax(0,1fr)] lg:gap-8">
        <aside>
          <CategoryList activeType={activeType} onChange={setActiveType} topicCount={topicCount} />
        </aside>

        <section className={CARD}>
          <SectionHeader
            title={SECTION_META[activeType].label}
            description={SECTION_META[activeType].helper}
            action={
              activeType === "topic" && topicCount !== null ? (
                <span className="text-sm text-muted-foreground">{topicCount}건</span>
              ) : undefined
            }
          />
          {activeType === "topic" ? (
            <HistoryList state={history} />
          ) : (
            <div className={PLACEHOLDER}>실전 연습은 준비 중이에요. 곧 만나요!</div>
          )}
        </section>
      </div>
    </PageShell>
  );
}
