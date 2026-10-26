"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/routes";
import { SectionShell } from "@/features/home/components/SectionShell";
import { fetchPracticeSessions } from "@/features/practice/api";
import type { PracticeSessionSummary } from "@/features/practice/types";
import { HISTORY_STATUS_DOT, formatShortDate, historyPath, historyStatus } from "@/features/practice/history-format";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ko } from "@/locales/ko";

const RECENT_LIMIT = 3;

type State =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; sessions: PracticeSessionSummary[] };

export function RecentActivitySection() {
  const [state, setState] = useState<State>({ status: "loading" });
  const t = ko.home.recentActivity;

  useEffect(() => {
    let alive = true;
    fetchPracticeSessions(RECENT_LIMIT)
      .then((sessions) => alive && setState({ status: "ready", sessions }))
      .catch(() => alive && setState({ status: "error" }));
    return () => {
      alive = false;
    };
  }, []);

  return (
    <SectionShell
      title={t.description}
      action={
        <Button asChild variant="ghost" className="h-9 rounded-full px-3 font-semibold text-primary-strong">
          <Link href={ROUTES.mypage}>
            {t.viewAll}
            <ChevronRight />
          </Link>
        </Button>
      }
    >
      {state.status === "loading" ? (
        <div className="grid gap-3 sm:grid-cols-3" aria-busy="true">
          {Array.from({ length: RECENT_LIMIT }, (_, i) => (
            <Skeleton key={i} className="h-28 rounded-2xl" />
          ))}
        </div>
      ) : state.status === "error" ? (
        <p className="rounded-2xl bg-secondary px-5 py-6 text-sm text-muted-foreground">{t.error}</p>
      ) : state.sessions.length === 0 ? (
        <div className="flex flex-col items-start gap-3 rounded-2xl bg-secondary px-5 py-6 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-muted-foreground">{t.empty}</p>
          <Button asChild className="rounded-full px-5">
            <Link href={ROUTES.practice}>{t.startFirst}</Link>
          </Button>
        </div>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-3">
          {state.sessions.map((session) => {
            const status = historyStatus(session);
            return (
              <li key={session.id}>
                <Link
                  href={historyPath(session.id)}
                  className="group flex h-full flex-col gap-2.5 rounded-2xl border border-accent bg-secondary px-5 py-4 transition-colors hover:border-primary/40 hover:bg-card"
                >
                  <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
                    <span aria-hidden className={cn("size-2 rounded-full", HISTORY_STATUS_DOT[status.tone])} />
                    {formatShortDate(session.completedAt)} · {status.label}
                    {session.estimatedLevel ? ` · ${t.estimatedLevel.replace("{level}", session.estimatedLevel)}` : ""}
                  </span>
                  <span className="text-lg font-bold group-hover:text-primary-strong">{session.topicTitle}</span>
                  <span className="text-sm text-muted-foreground">
                    {t.questionCount.replace("{n}", String(session.answerCount))}
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </SectionShell>
  );
}
