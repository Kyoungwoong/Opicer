"use client";

import { useEffect } from "react";
import Link from "next/link";
import { AlertCircle, ArrowRight, CheckCircle2, LoaderCircle, Mic, MicOff, RotateCcw, Send, Timer, Volume2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/routes";
import { Mascot } from "@/components/brand/Mascot";
import { Eyebrow, SectionHeader } from "@/components/common/SectionHeader";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { MicLevelMeter } from "@/features/practice/components/MicLevelMeter";
import { useMockExam } from "@/features/mock/hooks/use-mock-exam";
import { DIFFICULTY_OPTIONS, EXAM_SECONDS, PRACTICE_NOTICE, TOTAL_QUESTIONS, formatClock } from "@/features/mock/exam";
import type { MockItem } from "@/features/mock/types";

/** Warn in the last five minutes, when an answer should be wrapped up. */
const CLOCK_WARNING_SECONDS = 5 * 60;

type Props = { testId: string; firstPart: MockItem[] };

export function ExamScreen({ testId, firstPart }: Props) {
  const exam = useMockExam(testId, firstPart);
  const { phase } = exam;
  const inExam = phase !== "submitted";

  // A reload or closed tab would lose every recording, so ask first.
  useEffect(() => {
    if (!inExam) return;
    const warn = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [inExam]);

  if (phase === "submitted" && exam.result) {
    return (
      <section className="flex flex-col items-center gap-6 rounded-3xl border bg-card px-5 py-12 text-center shadow-card sm:px-10">
        <span className="grid size-14 place-items-center rounded-full bg-success-subtle text-success">
          <CheckCircle2 aria-hidden className="size-7" />
        </span>
        <SectionHeader
          as="h1"
          title="수고했어요! 제출을 마쳤어요"
          description={`${exam.recorded}문항의 답변을 보냈어요. 평가에는 몇 분 정도 걸려요.`}
          className="justify-center"
        />
        <p className="text-sm text-muted-foreground">
          {exam.result.alreadyCharged
            ? "이미 차감된 시험이라 크레딧은 다시 차감되지 않았어요."
            : `${exam.result.deductedCredits}크레딧이 차감됐어요.`}
        </p>
        <p className="rounded-full bg-secondary px-4 py-2 text-xs font-semibold text-muted-foreground">{PRACTICE_NOTICE}</p>
        <div className="flex flex-wrap justify-center gap-2">
          <Button asChild className="h-11 rounded-full px-6 text-sm font-semibold">
            <Link href={ROUTES.mockResult(exam.result.mockTestId)}>결과 보러 가기</Link>
          </Button>
          <Button asChild variant="ghost" className="h-11 rounded-full px-6 text-muted-foreground">
            <Link href={ROUTES.home}>홈으로</Link>
          </Button>
        </div>
      </section>
    );
  }

  const warning = exam.remainingSeconds <= CLOCK_WARNING_SECONDS;
  const header = (
    <section className="flex flex-col gap-4 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <SectionHeader
          as="h1"
          eyebrow="실전연습 · 연습용 모의 시험"
          title={phase === "difficulty" ? "난이도 다시 고르기" : `질문 ${Math.min(exam.index + 1, TOTAL_QUESTIONS)} / ${TOTAL_QUESTIONS}`}
        />
        <div
          role="timer"
          aria-live={exam.remainingSeconds <= 60 ? "assertive" : "off"}
          className={cn(
            "flex items-center gap-2 rounded-full px-4 py-2",
            warning ? "bg-destructive/10 text-destructive" : "bg-secondary text-foreground"
          )}
        >
          <Timer aria-hidden className="size-4 text-muted-foreground" />
          <span className="sr-only">시험 남은 시간</span>
          <span className="text-lg font-bold tabular-nums">{formatClock(exam.remainingSeconds)}</span>
        </div>
      </div>
      <Progress value={(exam.remainingSeconds / EXAM_SECONDS) * 100} aria-label="시험 남은 시간" className="h-1.5" />
      <ol aria-hidden className="flex gap-1">
        {Array.from({ length: TOTAL_QUESTIONS }, (_, i) => (
          <li
            key={i}
            className={cn(
              "h-1.5 flex-1 rounded-full",
              i < exam.index || (phase === "difficulty" && i === exam.index) ? "bg-primary" : i === exam.index ? "bg-primary/50" : "bg-muted"
            )}
          />
        ))}
      </ol>
    </section>
  );

  if (phase === "submitting" || phase === "submitFailed") {
    return (
      <>
        {header}
        <section className="flex flex-col items-center gap-4 rounded-3xl border bg-card px-5 py-16 text-center shadow-card">
          {phase === "submitting" ? (
            <>
              <LoaderCircle role="status" aria-label="제출 중" className="size-8 animate-spin text-primary-strong" />
              <p className="text-sm text-muted-foreground">
                답변을 텍스트로 옮기고 있어요… ({exam.transcribed} / {exam.recorded})
              </p>
            </>
          ) : (
            <Alert variant="destructive" className="max-w-md text-left">
              <AlertCircle />
              <AlertTitle>제출하지 못했어요</AlertTitle>
              <AlertDescription className="flex flex-col gap-3">
                <p>{exam.error}</p>
                <p>녹음한 답변은 이 화면에 남아 있어요. 다시 제출해도 크레딧은 한 번만 차감돼요.</p>
                <Button type="button" variant="outline" size="sm" onClick={exam.retrySubmit} className="self-start rounded-full">
                  <RotateCcw />
                  다시 제출하기
                </Button>
              </AlertDescription>
            </Alert>
          )}
        </section>
      </>
    );
  }

  if (phase === "difficulty") {
    return (
      <>
        {header}
        <section className="flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
          <SectionHeader
            title="1부가 끝났어요. 남은 8문항의 난이도를 골라 주세요"
            description="한 번만 고를 수 있어요. 시험 시간은 계속 흐르고 있어요."
          />
          <div className="grid gap-3 sm:grid-cols-3">
            {DIFFICULTY_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                disabled={exam.adjusting}
                onClick={() => void exam.chooseDifficulty(option.value)}
                className="flex flex-col gap-1 rounded-2xl border bg-card px-4 py-5 text-left transition-colors hover:border-primary hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none disabled:opacity-50"
              >
                <span className="text-lg font-extrabold">{option.label}</span>
                <span className="text-sm text-muted-foreground">{option.description}</span>
              </button>
            ))}
          </div>
          {exam.adjusting ? (
            <p role="status" className="flex items-center gap-2 text-sm text-muted-foreground">
              <LoaderCircle aria-hidden className="size-4 animate-spin" />
              다음 문항을 준비하고 있어요…
            </p>
          ) : null}
          {exam.error ? (
            <Alert variant="destructive">
              <AlertCircle />
              <AlertDescription>{exam.error} 다시 골라 주세요.</AlertDescription>
            </Alert>
          ) : null}
        </section>
      </>
    );
  }

  const playing = phase === "playing";
  const interviewer = playing
    ? { expression: "talk" as const, caption: "오피가 질문을 읽고 있어요" }
    : exam.isRecording
      ? { expression: "listen" as const, caption: "답변을 듣고 있어요. 다 말했으면 다음으로 넘어가세요" }
      : { expression: "smile" as const, caption: "준비되면 다음으로 넘어가세요" };

  return (
    <>
      {header}
      <div className="grid gap-4 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-6">
        <section
          aria-label="인터뷰어"
          className="flex items-center gap-4 rounded-3xl border bg-card p-4 shadow-card md:flex-col md:justify-center md:p-6 md:text-center"
        >
          <Mascot expression={interviewer.expression} label="" className="max-w-20 shrink-0 md:max-w-64" />
          <div className="flex min-w-0 flex-col gap-1">
            <p className="text-lg font-bold">
              오피 <span className="text-sm font-medium text-muted-foreground">· 인터뷰어</span>
            </p>
            <p className="text-sm text-muted-foreground">{interviewer.caption}</p>
          </div>
        </section>

        <section aria-label="문항" className="flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <Eyebrow>
              질문 {exam.index + 1} · {exam.current?.part === 1 ? "1부" : "2부"}
            </Eyebrow>
            {exam.canReplay ? (
              <Button type="button" variant="outline" size="sm" onClick={exam.replay} className="rounded-full px-3">
                <RotateCcw />
                다시 듣기 ({exam.playsLeft}회 남음)
              </Button>
            ) : null}
          </div>

          <div className="flex items-center gap-4 rounded-2xl bg-secondary px-4 py-4">
            <span
              className={cn(
                "grid size-12 shrink-0 place-items-center rounded-full",
                playing ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
              )}
            >
              <Volume2 aria-hidden className="size-5" />
            </span>
            <div aria-hidden className="flex h-10 flex-1 items-center gap-1">
              {Array.from({ length: 16 }, (_, i) => (
                <span
                  key={i}
                  className={cn(
                    "h-full flex-1 origin-center rounded-full",
                    playing ? "animate-eq-bar bg-primary motion-reduce:animate-none" : "scale-y-[0.2] bg-muted-foreground/30"
                  )}
                  style={playing ? { animationDelay: `${(i % 5) * 0.12 + (i % 3) * 0.07}s` } : undefined}
                />
              ))}
            </div>
          </div>

          {exam.audioFailed && exam.current ? (
            <div className="flex flex-col gap-2 rounded-2xl border border-dashed p-4">
              <p className="text-xs font-semibold text-muted-foreground">질문을 재생하지 못해서 글로 보여 드려요</p>
              <p className="text-base font-semibold leading-relaxed">{exam.current.questionText}</p>
            </div>
          ) : (
            <p className="text-xs text-muted-foreground">실제 시험처럼 질문은 소리로만 나와요. 답을 다 하면 다음으로 넘어가세요.</p>
          )}

          <div className="flex flex-col gap-3 border-t pt-5">
            <div aria-live="polite" className="flex">
              {playing ? (
                <span className="inline-flex items-center gap-2 rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-foreground">
                  <Volume2 aria-hidden className="size-4" />
                  질문 재생 중…
                </span>
              ) : exam.isRecording ? (
                <span className="inline-flex items-center gap-2 rounded-full bg-destructive/10 px-4 py-2 text-sm font-semibold text-destructive">
                  <span aria-hidden className="relative flex size-2.5">
                    <span className="absolute inline-flex size-full animate-ping rounded-full bg-destructive/60 motion-reduce:animate-none" />
                    <span className="relative inline-flex size-2.5 rounded-full bg-destructive" />
                  </span>
                  <Mic aria-hidden className="size-4" />
                  녹음 중…
                </span>
              ) : (
                <span className="inline-flex items-center gap-2 rounded-full bg-muted px-4 py-2 text-sm font-semibold text-muted-foreground">
                  <MicOff aria-hidden className="size-4" />
                  녹음하지 않는 중
                </span>
              )}
            </div>
            <MicLevelMeter stream={exam.micStream} />
          </div>

          {exam.error ? (
            <Alert variant="destructive">
              <AlertCircle />
              <AlertDescription>{exam.error}</AlertDescription>
            </Alert>
          ) : null}

          <div className="flex justify-end">
            <Button
              type="button"
              onClick={exam.next}
              disabled={playing}
              variant={exam.isLastQuestion ? "default" : "secondary"}
              className="h-11 rounded-full px-6 text-sm font-semibold"
            >
              {exam.isLastQuestion ? (
                <>
                  <Send />
                  제출하기
                </>
              ) : (
                <>
                  다음
                  <ArrowRight />
                </>
              )}
            </Button>
          </div>
        </section>
      </div>
      <p className="text-center text-xs text-muted-foreground">이전 문항으로는 돌아갈 수 없어요. 새로고침하거나 창을 닫으면 녹음이 사라져요.</p>
    </>
  );
}
