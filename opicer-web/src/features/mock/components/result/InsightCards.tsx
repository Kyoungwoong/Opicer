"use client";

import { ArrowRight, Clock, LoaderCircle, MessageSquareText, Shapes, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { CATEGORY_LABELS } from "@/features/practice/evaluation-labels";
import { formatClock } from "@/features/mock/exam";
import type { MockTestInsights } from "@/features/mock/types";

const CARD = "flex flex-col gap-4 rounded-3xl border bg-card p-5 shadow-card sm:p-6";
const NOTE_LIST = "flex flex-col gap-1.5 text-sm leading-relaxed";

function CardTitle({ icon: Icon, title, description }: { icon: typeof Clock; title: string; description: string }) {
  return (
    <div className="flex items-start gap-3">
      <span className="grid size-9 shrink-0 place-items-center rounded-full bg-accent text-primary-strong">
        <Icon aria-hidden className="size-4" />
      </span>
      <div className="flex flex-col gap-0.5">
        <h2 className="text-lg font-bold">{title}</h2>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}

function Notes({ notes }: { notes: string[] }) {
  if (notes.length === 0) return null;
  return (
    <ul className={NOTE_LIST}>
      {notes.map((note) => (
        <li key={note} className="rounded-2xl bg-secondary px-4 py-2.5">
          {note}
        </li>
      ))}
    </ul>
  );
}

export function TypeDiagnosisCard({ insights }: { insights: MockTestInsights }) {
  const { questionTypes, strongestTypes, weakestTypes } = insights;
  return (
    <section className={CARD}>
      <CardTitle icon={Shapes} title="유형별 진단" description="질문 유형마다 답변의 예상 레벨(참고용)과 가장 많이 고친 부분이에요." />
      {questionTypes.length === 0 ? (
        <p className="text-sm text-muted-foreground">평가한 답변이 없어 유형별 진단을 할 수 없어요.</p>
      ) : (
        <ul className="flex flex-col divide-y">
          {questionTypes.map((t) => (
            <li key={t.type} className="flex flex-wrap items-center gap-x-3 gap-y-1 py-3 first:pt-0 last:pb-0">
              <Badge className="h-6 min-w-10 justify-center px-2 text-sm font-bold">{t.level}</Badge>
              <span className="font-semibold">{t.label}</span>
              <span className="text-xs text-muted-foreground">답변 {t.answers}개</span>
              {strongestTypes.includes(t.type) ? <Badge variant="secondary">강점</Badge> : null}
              {weakestTypes.includes(t.type) ? <Badge variant="outline">보완하면 좋아요</Badge> : null}
              {t.topIssue ? (
                <span className="basis-full text-sm text-muted-foreground sm:ml-auto sm:basis-auto">
                  자주 고친 부분: {CATEGORY_LABELS[t.topIssue]}
                </span>
              ) : null}
            </li>
          ))}
        </ul>
      )}
      {questionTypes.length > 0 && strongestTypes.length === 0 ? (
        <p className="text-sm text-muted-foreground">모든 유형이 비슷한 수준이에요.</p>
      ) : null}
    </section>
  );
}

export function HabitsCard({ insights }: { insights: MockTestInsights }) {
  const { habits } = insights;
  const connectorTotal = habits.connectors.reduce((sum, c) => sum + c.count, 0);
  return (
    <section className={CARD}>
      <CardTitle icon={MessageSquareText} title="말하기 습관" description="15문항의 답변 전체에서 반복된 패턴이에요." />
      <dl className="grid grid-cols-2 gap-3">
        <div className="rounded-2xl bg-secondary px-4 py-3">
          <dt className="text-xs text-muted-foreground">평균 문장 길이</dt>
          <dd className="text-lg font-bold tabular-nums">{habits.averageSentenceLength}단어</dd>
        </div>
        <div className="rounded-2xl bg-secondary px-4 py-3">
          <dt className="text-xs text-muted-foreground">어휘 다양성</dt>
          <dd className="text-lg font-bold tabular-nums">{habits.lexicalDiversity.toFixed(2)}</dd>
        </div>
      </dl>
      {habits.connectors.length > 0 ? (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-semibold">연결어 {connectorTotal}번</p>
          <ul className="flex flex-wrap gap-1.5">
            {habits.connectors.map((c) => (
              <li
                key={c.word}
                className={
                  c.word === habits.dominantConnector
                    ? "rounded-full border border-primary bg-accent px-3 py-1 text-sm font-semibold text-primary-strong"
                    : "rounded-full border px-3 py-1 text-sm"
                }
              >
                {c.word} <span className="tabular-nums text-muted-foreground">×{c.count}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      {habits.repeatedWords.length > 0 ? (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-semibold">자주 반복한 단어</p>
          <ul className="flex flex-wrap gap-1.5">
            {habits.repeatedWords.map((w) => (
              <li key={w.word} className="rounded-full border px-3 py-1 text-sm">
                {w.word} <span className="tabular-nums text-muted-foreground">×{w.count}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      <Notes notes={habits.notes} />
    </section>
  );
}

function orders(list: number[]): string {
  return list.map((n) => `${n}번`).join(", ");
}

export function TimeCard({ insights }: { insights: MockTestInsights }) {
  const { time } = insights;
  return (
    <section className={CARD}>
      <CardTitle icon={Clock} title="시간 관리" description="40분을 어떻게 썼는지 보여 줘요." />
      <div className="flex flex-col gap-2">
        <div className="flex items-baseline justify-between gap-3">
          <span className="text-2xl font-extrabold tabular-nums">{formatClock(time.usedSeconds)}</span>
          <span className="text-sm text-muted-foreground">/ {formatClock(time.limitSeconds)}</span>
        </div>
        <Progress value={Math.min(100, (time.usedSeconds / time.limitSeconds) * 100)} aria-label="사용한 시험 시간" className="h-2" />
      </div>
      <dl className="grid grid-cols-2 gap-3">
        <div className="rounded-2xl bg-secondary px-4 py-3">
          <dt className="text-xs text-muted-foreground">1부 답변 시간</dt>
          <dd className="text-lg font-bold tabular-nums">{formatClock(time.part1Seconds)}</dd>
        </div>
        <div className="rounded-2xl bg-secondary px-4 py-3">
          <dt className="text-xs text-muted-foreground">2부 답변 시간</dt>
          <dd className="text-lg font-bold tabular-nums">{formatClock(time.part2Seconds)}</dd>
        </div>
      </dl>
      <ul className="flex flex-col gap-1 text-sm">
        {time.shortAnswers.length > 0 ? <li>30초 미만: {orders(time.shortAnswers)}</li> : null}
        {time.longAnswers.length > 0 ? <li>3분 초과: {orders(time.longAnswers)}</li> : null}
        {time.unanswered.length > 0 ? <li>답하지 못함: {orders(time.unanswered)}</li> : null}
      </ul>
      <Notes notes={time.notes} />
    </section>
  );
}

type RecommendationsProps = {
  insights: MockTestInsights;
  /** Topic being opened for practice, if any. */
  startingTopicId: string | null;
  error: string | null;
  onPractice: (topicId: string) => void;
};

export function RecommendationsCard({ insights, startingTopicId, error, onPractice }: RecommendationsProps) {
  if (insights.recommendations.length === 0) return null;
  return (
    <section className={CARD}>
      <CardTitle
        icon={Sparkles}
        title="다음 연습 추천"
        description="약했던 유형의 질문이 있는 주제예요. 바로 주제별 연습을 시작할 수 있어요."
      />
      <ul className="flex flex-col gap-4">
        {insights.recommendations.map((r) => (
          <li key={r.type} className="flex flex-col gap-2">
            <p className="text-sm font-semibold">{r.label}</p>
            <div className="flex flex-wrap gap-2">
              {r.topics.map((topic) => (
                <Button
                  key={topic.id}
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={startingTopicId !== null}
                  onClick={() => onPractice(topic.id)}
                  className="rounded-full px-4"
                >
                  {startingTopicId === topic.id ? <LoaderCircle className="animate-spin" /> : null}
                  {topic.title}
                  <ArrowRight />
                </Button>
              ))}
            </div>
          </li>
        ))}
      </ul>
      {error ? (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      ) : null}
    </section>
  );
}
