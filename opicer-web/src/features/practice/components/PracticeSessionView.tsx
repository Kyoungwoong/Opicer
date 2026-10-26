"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  LoaderCircle,
  Mic,
  MicOff,
  PenLine,
  RotateCcw,
  Send,
  Sparkles,
  Timer,
  Volume2,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Eyebrow, SectionHeader } from "@/components/common/SectionHeader";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import {
  analyzeAnswer,
  fetchPracticeQuestions,
  improveScript,
  savePracticeAnswerResult,
  submitPracticeSession,
  submitTopicSelection,
  transcribeAudio,
} from "@/features/practice/api";
import { ROUTES } from "@/lib/routes";
import type { PracticeAnswer, PracticeQuestion } from "@/features/practice/types";
import { TranscriptDiff } from "@/features/practice/components/TranscriptDiff";

const PRACTICE_DURATION_SECONDS = 120;
const REPLAY_WINDOW_SECONDS = 5;

type Mode = "loading" | "playing" | "recording" | "submitting" | "summary";

type QuestionAiState = {
  isAnalyzing: boolean;
  analysis: string | null;
  analyzeError: string | null;
  isImproving: boolean;
  improvement: string | null;
  improveError: string | null;
};

type Props = {
  topicId: string;
  topicSelectionId: string;
  userLabel?: string;
  onLogout?: () => void;
};

export function PracticeSessionView({ topicId, topicSelectionId, userLabel, onLogout }: Props) {
  const router = useRouter();

  // ── Core state ─────────────────────────────────────────────
  const [questions, setQuestions] = useState<PracticeQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<PracticeAnswer[]>([]);
  const [mode, setMode] = useState<Mode>("loading");
  const [error, setError] = useState<string | null>(null);

  // ── AI state per question ───────────────────────────────────
  const [aiStates, setAiStates] = useState<Record<string, QuestionAiState>>({});

  // ── Timer / replay state ────────────────────────────────────
  const [remainingSeconds, setRemainingSeconds] = useState(PRACTICE_DURATION_SECONDS);
  const [replayWindow, setReplayWindow] = useState(0);
  const [hasReplayed, setHasReplayed] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [timeExpired, setTimeExpired] = useState(false);

  // ── Refs (always fresh, no stale closure risk) ──────────────
  const timerRef = useRef<number | null>(null);
  const replayTimerRef = useRef<number | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const voicesRef = useRef<SpeechSynthesisVoice[]>([]);
  // Stable ref to goToNext — updated every render to avoid stale closures
  const goToNextRef = useRef<() => void>(() => {});
  // answersRef mirrors answers state for sync access inside async callbacks
  const answersRef = useRef<PracticeAnswer[]>([]);
  // pendingSubmitRef signals that onstop should trigger transcription
  const pendingSubmitRef = useRef(false);
  // Saved practice session id (from submit), used to persist AI results to history
  const sessionIdRef = useRef<string | null>(null);

  /** Best-effort: a failed history save must not disturb the result already on screen. */
  const persistAiResult = (questionId: string, result: { analysis?: string; improvedScript?: string }) => {
    const sessionId = sessionIdRef.current;
    if (!sessionId) return;
    savePracticeAnswerResult(sessionId, questionId, result).catch(() => {});
  };

  const currentQuestion = questions[currentIndex];
  const isLastQuestion = currentIndex === questions.length - 1;

  // ── Voice preload ───────────────────────────────────────────
  useEffect(() => {
    if (!("speechSynthesis" in window)) return;
    const load = () => { voicesRef.current = window.speechSynthesis.getVoices(); };
    load();
    window.speechSynthesis.addEventListener("voiceschanged", load);
    return () => window.speechSynthesis.removeEventListener("voiceschanged", load);
  }, []);

  // ── Fetch questions ─────────────────────────────────────────
  useEffect(() => {
    fetchPracticeQuestions(topicId)
      .then((data) => {
        if (data.length === 0) {
          setError("이 주제에 등록된 질문이 없습니다.");
          return;
        }
        setQuestions(data);
        setMode("playing");
      })
      .catch((err: unknown) => {
        const message = err instanceof Error ? err.message : "질문을 불러오지 못했습니다.";
        setError(message);
      });
  }, [topicId]);

  // ── Helpers ─────────────────────────────────────────────────
  const getEnglishFemaleVoice = (): SpeechSynthesisVoice | null => {
    const voices = voicesRef.current.length
      ? voicesRef.current
      : window.speechSynthesis.getVoices();
    const enVoices = voices.filter((v) => v.lang.startsWith("en"));
    const femaleNames = [
      "Samantha", "Karen", "Ava", "Victoria", "Susan", "Allison",
      "Google US English", "Google UK English Female",
    ];
    return (
      enVoices.find((v) => femaleNames.some((n) => v.name.includes(n))) ??
      enVoices[0] ??
      null
    );
  };

  const stopRecording = () => {
    if (recorderRef.current && recorderRef.current.state !== "inactive") {
      recorderRef.current.stop();
    }
    recorderRef.current = null;
    setIsRecording(false);
  };

  const clearMainTimer = () => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  const clearReplayTimer = () => {
    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
      replayTimerRef.current = null;
    }
  };

  const stopAll = () => {
    stopRecording();
    clearMainTimer();
    clearReplayTimer();
    if ("speechSynthesis" in window) window.speechSynthesis.cancel();
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.onended = null;
    }
  };

  // ── Transcribe all answers and switch to summary ─────────────
  const doTranscribeAndSummary = (snapshot: PracticeAnswer[]) => {
    setMode("submitting");
    Promise.all(
      snapshot.map(async (a) => {
        if (!a.audioUrl) return a;
        try {
          const transcript = await transcribeAudio(a.audioUrl, a.questionText);
          return { ...a, transcript };
        } catch (err) {
          const transcribeError = err instanceof Error ? err.message : "전사 실패";
          return { ...a, transcribeError };
        }
      })
    ).then(async (updated) => {
      try {
        const result = await submitPracticeSession(
          topicSelectionId,
          updated.map((a) => ({ questionId: a.questionId, questionText: a.questionText, transcript: a.transcript ?? null }))
        );
        sessionIdRef.current = result.sessionId;
      } catch (err) {
        const message = err instanceof Error ? err.message : "연습 제출 처리에 실패했습니다.";
        setError(message);
      }
      answersRef.current = updated;
      setAnswers(updated);
      setMode("summary");
    });
  };

  const startRecording = async (question: PracticeQuestion) => {
    if (recorderRef.current && recorderRef.current.state !== "inactive") return;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      chunksRef.current = [];
      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data);
      };
      recorder.onstop = () => {
        stream.getTracks().forEach((t) => t.stop());
        const blob = new Blob(chunksRef.current, { type: "audio/webm" });
        const url = URL.createObjectURL(blob);
        const newAnswer: PracticeAnswer = {
          questionId: question.id,
          questionText: question.promptText,
          audioUrl: url,
          recordedAt: new Date().toISOString(),
        };
        // Sync update answersRef first, then state
        const nextAnswers = [
          ...answersRef.current.filter((a) => a.questionId !== question.id),
          newAnswer,
        ];
        answersRef.current = nextAnswers;
        setAnswers(nextAnswers);

        // If submit was requested while recording was active, now safe to transcribe
        if (pendingSubmitRef.current) {
          pendingSubmitRef.current = false;
          doTranscribeAndSummary(nextAnswers);
        }
      };
      recorderRef.current = recorder;
      recorder.start();
      setIsRecording(true);
    } catch {
      setError("마이크 접근이 필요합니다. 브라우저에서 마이크 권한을 허용해주세요.");
    }
  };

  const startTimer = () => {
    clearMainTimer();
    setRemainingSeconds(PRACTICE_DURATION_SECONDS);
    timerRef.current = window.setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          window.clearInterval(timerRef.current!);
          timerRef.current = null;
          setTimeExpired(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const startReplayWindow = () => {
    clearReplayTimer();
    setReplayWindow(REPLAY_WINDOW_SECONDS);
    replayTimerRef.current = window.setInterval(() => {
      setReplayWindow((prev) => {
        if (prev <= 1) {
          window.clearInterval(replayTimerRef.current!);
          replayTimerRef.current = null;
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const playQuestionAudio = (question: PracticeQuestion, onEnded: () => void) => {
    if (question.promptAudioUrl) {
      if (!audioRef.current) audioRef.current = new Audio(question.promptAudioUrl);
      else audioRef.current.src = question.promptAudioUrl;
      audioRef.current.onended = onEnded;
      audioRef.current.play().catch(() => setError("오디오 재생에 실패했습니다."));
    } else if ("speechSynthesis" in window) {
      const utterance = new SpeechSynthesisUtterance(question.promptText);
      utterance.lang = "en-US";
      utterance.rate = 0.9;
      const voice = getEnglishFemaleVoice();
      if (voice) utterance.voice = voice;
      utterance.onend = onEnded;
      window.speechSynthesis.cancel();
      window.speechSynthesis.speak(utterance);
    }
  };

  const goToNext = () => {
    setHasReplayed(false);
    setReplayWindow(0);
    setRemainingSeconds(PRACTICE_DURATION_SECONDS);
    setTimeExpired(false);
    if (isLastQuestion) {
      if (recorderRef.current && recorderRef.current.state !== "inactive") {
        // Recording is active → flag it; onstop will call doTranscribeAndSummary
        pendingSubmitRef.current = true;
        stopAll();
        setMode("submitting");
      } else {
        // No active recording → safe to submit immediately
        stopAll();
        doTranscribeAndSummary(answersRef.current);
      }
    } else {
      stopAll();
      setMode("playing");
      setCurrentIndex((prev) => prev + 1);
    }
  };

  // Keep goToNextRef fresh every render
  useEffect(() => { goToNextRef.current = goToNext; });

  // ── Auto-advance when timer expires ─────────────────────────
  useEffect(() => {
    if (timeExpired) {
      stopRecording();
      goToNextRef.current();
    }
  }, [timeExpired]);

  // ── Auto-play when question changes ─────────────────────────
  useEffect(() => {
    if (mode !== "playing" || questions.length === 0) return;
    const question = questions[currentIndex];
    if (!question) return;

    setHasReplayed(false);
    setReplayWindow(0);
    setRemainingSeconds(PRACTICE_DURATION_SECONDS);
    setTimeExpired(false);

    playQuestionAudio(question, () => {
      setMode("recording");
      startRecording(question);
      startTimer();
      startReplayWindow();
    });

    return () => {
      if ("speechSynthesis" in window) window.speechSynthesis.cancel();
      if (audioRef.current) { audioRef.current.pause(); audioRef.current.onended = null; }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentIndex, mode === "playing" ? "playing" : null, questions.length]);

  // ── Replay button handler ────────────────────────────────────
  const handleReplay = () => {
    if (hasReplayed || replayWindow <= 0) return;
    const question = questions[currentIndex];
    if (!question) return;

    setHasReplayed(true);
    stopRecording();
    clearMainTimer();
    clearReplayTimer();
    setReplayWindow(0);
    setMode("playing");

    playQuestionAudio(question, () => {
      setMode("recording");
      startRecording(question);
      startTimer();
    });
  };

  // ── AI actions ───────────────────────────────────────────────
  const getOrInitAiState = (questionId: string): QuestionAiState =>
    aiStates[questionId] ?? {
      isAnalyzing: false, analysis: null, analyzeError: null,
      isImproving: false, improvement: null, improveError: null,
    };

  const findQuestionMeta = (questionId: string) =>
    questions.find((q) => q.id === questionId);

  const handleAnalyze = async (answer: PracticeAnswer) => {
    if (!answer.transcript) return;
    const id = answer.questionId;
    setAiStates((prev) => ({
      ...prev,
      [id]: { ...getOrInitAiState(id), isAnalyzing: true, analyzeError: null },
    }));
    try {
      const question = findQuestionMeta(answer.questionId);
      const analysis = await analyzeAnswer(
        topicId,
        answer.questionText,
        answer.transcript,
        question?.type,
        question?.targetLevels?.[0]
      );
      setAiStates((prev) => ({
        ...prev,
        [id]: { ...prev[id]!, isAnalyzing: false, analysis },
      }));
      persistAiResult(id, { analysis });
    } catch (err) {
      const message = err instanceof Error ? err.message : "분석에 실패했습니다.";
      setAiStates((prev) => ({
        ...prev,
        [id]: { ...prev[id]!, isAnalyzing: false, analyzeError: message },
      }));
    }
  };

  const handleImprove = async (answer: PracticeAnswer) => {
    if (!answer.transcript) return;
    const id = answer.questionId;
    setAiStates((prev) => ({
      ...prev,
      [id]: { ...getOrInitAiState(id), isImproving: true, improveError: null },
    }));
    try {
      const question = findQuestionMeta(answer.questionId);
      const improvement = await improveScript(
        topicId,
        answer.questionText,
        answer.transcript,
        question?.type,
        question?.targetLevels?.[0]
      );
      setAiStates((prev) => ({
        ...prev,
        [id]: { ...prev[id]!, isImproving: false, improvement },
      }));
      persistAiResult(id, { improvedScript: improvement });
    } catch (err) {
      const message = err instanceof Error ? err.message : "수정에 실패했습니다.";
      setAiStates((prev) => ({
        ...prev,
        [id]: { ...prev[id]!, isImproving: false, improveError: message },
      }));
    }
  };

  // ── Cleanup on unmount ───────────────────────────────────────
  useEffect(() => {
    return () => { stopAll(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const topicTitle = currentQuestion?.topic ?? questions[0]?.topic ?? "연습";
  const warning = remainingSeconds <= 10 && remainingSeconds > 0;


  const nav = <TopNav userLabel={userLabel} onLogout={onLogout} />;

  // ── Submitting screen ────────────────────────────────────────
  if (mode === "submitting") {
    return (
      <PageShell width="md" nav={nav}>
        <div role="status" className="flex flex-col items-center justify-center gap-4 py-24">
          <LoaderCircle aria-hidden className="size-8 animate-spin text-primary" />
          <p className="text-sm text-muted-foreground">답변 분석 중…</p>
        </div>
      </PageShell>
    );
  }

  // ── Summary screen ───────────────────────────────────────────
  if (mode === "summary") {
    return (
      <PageShell width="md" nav={nav}>
        {error ? (
          <Alert variant="destructive">
            <AlertCircle />
            <AlertTitle>크레딧 차감/제출 처리에 실패했습니다.</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        <section className="flex items-center gap-4 rounded-3xl border bg-card p-6 shadow-card sm:p-8">
          <span className="grid size-12 shrink-0 place-items-center rounded-full bg-success-subtle text-success">
            <CheckCircle2 aria-hidden className="size-6" />
          </span>
          <SectionHeader
            as="h1"
            title="연습 완료"
            description={`${topicTitle} · ${answers.length}문항 답변 완료`}
          />
        </section>

        <ol className="flex flex-col gap-5">
          {answers.map((answer, idx) => {
            const ai = getOrInitAiState(answer.questionId);
            return (
              <li key={answer.questionId} className="flex flex-col gap-4 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
                <div className="flex flex-col gap-1.5">
                  <Eyebrow>질문 {idx + 1}</Eyebrow>
                  <p className="text-base font-semibold leading-relaxed">{answer.questionText}</p>
                </div>

                {/* Transcript + audio */}
                <div className="rounded-2xl border border-primary/20 bg-accent/60 p-4">
                  <p className="text-xs font-semibold text-primary-strong">Your Answer</p>
                  {answer.transcript ? (
                    <p className="mt-2 text-sm leading-relaxed">{answer.transcript}</p>
                  ) : answer.transcribeError ? (
                    <p className="mt-2 text-sm text-destructive">전사 실패: {answer.transcribeError}</p>
                  ) : (
                    <p className="mt-2 text-sm text-muted-foreground">전사 텍스트 없음</p>
                  )}
                  {answer.audioUrl ? (
                    <audio controls src={answer.audioUrl} className="mt-3 w-full" />
                  ) : (
                    <p className="mt-2 text-sm text-muted-foreground">녹음된 답변이 없습니다.</p>
                  )}
                </div>

                {/* Action buttons — only if transcript exists */}
                {answer.transcript && (
                  <div className="flex flex-wrap gap-2">
                    <Button
                      type="button"
                      onClick={() => handleAnalyze(answer)}
                      disabled={ai.isAnalyzing || ai.analysis !== null}
                      className="h-10 rounded-full px-5"
                    >
                      {ai.isAnalyzing ? <LoaderCircle className="animate-spin" /> : <Sparkles />}
                      {ai.isAnalyzing ? "분석 중…" : ai.analysis ? "분석 완료" : "분석하기"}
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => handleImprove(answer)}
                      disabled={ai.isImproving || ai.improvement !== null}
                      className="h-10 rounded-full px-5"
                    >
                      {ai.isImproving ? <LoaderCircle className="animate-spin" /> : <PenLine />}
                      {ai.isImproving ? "수정 중…" : ai.improvement ? "수정 완료" : "수정하기"}
                    </Button>
                  </div>
                )}

                {/* Analysis result */}
                {ai.analyzeError && (
                  <Alert variant="destructive">
                    <AlertCircle />
                    <AlertDescription>{ai.analyzeError}</AlertDescription>
                  </Alert>
                )}
                {ai.analysis && (
                  <div className="rounded-2xl border bg-secondary p-4 text-sm leading-relaxed whitespace-pre-wrap">
                    <Eyebrow className="mb-2">AI 분석</Eyebrow>
                    {ai.analysis}
                  </div>
                )}

                {/* Improvement diff */}
                {ai.improveError && (
                  <Alert variant="destructive">
                    <AlertCircle />
                    <AlertDescription>{ai.improveError}</AlertDescription>
                  </Alert>
                )}
                {ai.improvement && answer.transcript && (
                  <TranscriptDiff original={answer.transcript} improved={ai.improvement} />
                )}
              </li>
            );
          })}
        </ol>

        <div className="flex flex-col items-center gap-2 pb-10">
          <Button
            type="button"
            onClick={() => router.push(ROUTES.practice)}
            className="h-11 rounded-full px-8 text-sm font-semibold"
          >
            다른 주제 연습하기
          </Button>
          <Button
            type="button"
            variant="ghost"
            onClick={async () => {
              try {
                const selection = await submitTopicSelection(topicId);
                router.push(`${ROUTES.practiceSession(topicId)}?selectionId=${selection.id}`);
              } catch (err) {
                const message = err instanceof Error ? err.message : "새 연습 세션 생성에 실패했습니다.";
                setError(message);
              }
            }}
            className="text-muted-foreground"
          >
            <RotateCcw />
            같은 주제 다시 연습하기
          </Button>
        </div>
      </PageShell>
    );
  }

  // ── Loading / error screen ───────────────────────────────────
  if (mode === "loading") {
    return (
      <PageShell width="md" nav={nav}>
        <div className="flex flex-col items-center justify-center gap-4 py-24">
          {error ? (
            <>
              <Alert variant="destructive" className="max-w-md">
                <AlertCircle />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
              <Button
                type="button"
                variant="ghost"
                onClick={() => router.push(ROUTES.practice)}
                className="text-muted-foreground"
              >
                <ArrowLeft />
                주제 선택으로 돌아가기
              </Button>
            </>
          ) : (
            <LoaderCircle role="status" aria-label="불러오는 중" className="size-8 animate-spin text-primary" />
          )}
        </div>
      </PageShell>
    );
  }

  // ── Practice screen ──────────────────────────────────────────
  const timeLabel = `${Math.floor(remainingSeconds / 60).toString().padStart(2, "0")}:${(remainingSeconds % 60)
    .toString()
    .padStart(2, "0")}`;

  return (
    <PageShell width="md" nav={nav}>
      {/* Header */}
      <section className="flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <SectionHeader
            as="h1"
            title={topicTitle}
            description={`질문 ${currentIndex + 1} / ${questions.length}`}
          />
          <div
            role="timer"
            aria-live={warning ? "assertive" : "off"}
            className={cn(
              "flex min-w-28 flex-col items-center rounded-2xl border px-4 py-3",
              warning ? "border-destructive/40 bg-destructive/10 text-destructive" : "bg-secondary text-foreground"
            )}
          >
            <span className="inline-flex items-center gap-1 text-xs font-medium text-muted-foreground">
              <Timer aria-hidden className="size-3.5" />
              남은 시간
            </span>
            <span className={cn("text-2xl font-bold tabular-nums", warning && "animate-pulse")}>{timeLabel}</span>
            {warning ? <span className="text-xs font-semibold">마감 임박</span> : null}
          </div>
        </div>
        <Progress
          value={(remainingSeconds / PRACTICE_DURATION_SECONDS) * 100}
          aria-label="남은 답변 시간"
          className={cn("h-1.5", warning && "*:data-[slot=progress-indicator]:bg-destructive")}
        />
      </section>

      {/* Question card */}
      <section className="flex flex-col gap-6 rounded-3xl border bg-card p-5 shadow-card sm:p-8">
        <div className="flex flex-col gap-3">
          <Eyebrow>질문</Eyebrow>
          <h2 className="text-xl font-semibold leading-relaxed sm:text-2xl">
            {currentQuestion?.promptText ?? "질문을 불러오는 중입니다."}
          </h2>
          {currentQuestion?.structuralHint ? (
            <p className="rounded-xl bg-muted px-3 py-2 text-sm text-muted-foreground">{currentQuestion.structuralHint}</p>
          ) : null}
        </div>

        {/* Status + replay */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Only the status pill is live; the replay countdown changes every second. */}
          <div aria-live="polite" className="flex">
            {mode === "playing" ? (
              <span className="inline-flex items-center gap-2 rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-foreground">
                <Volume2 aria-hidden className="size-4 animate-pulse" />
                질문 재생 중…
              </span>
            ) : isRecording ? (
              <span className="inline-flex items-center gap-2 rounded-full bg-destructive/10 px-4 py-2 text-sm font-semibold text-destructive">
                <span aria-hidden className="relative flex size-2.5">
                  <span className="absolute inline-flex size-full animate-ping rounded-full bg-destructive/60" />
                  <span className="relative inline-flex size-2.5 rounded-full bg-destructive" />
                </span>
                <Mic aria-hidden className="size-4" />
                녹음 중…
              </span>
            ) : (
              <span className="inline-flex items-center gap-2 rounded-full bg-muted px-4 py-2 text-sm font-semibold text-muted-foreground">
                <MicOff aria-hidden className="size-4" />
                대기 중
              </span>
            )}
          </div>

          {/* Replay button — only during replay window */}
          {replayWindow > 0 && !hasReplayed && (
            <Button type="button" variant="outline" onClick={handleReplay} className="h-9 rounded-full px-4">
              <RotateCcw />
              다시 듣기 ({replayWindow}s)
            </Button>
          )}
        </div>

        {error ? (
          <Alert variant="destructive">
            <AlertCircle />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        {/* Navigation */}
        <div className="flex justify-end">
          <Button
            type="button"
            onClick={goToNext}
            disabled={mode === "playing"}
            variant={isLastQuestion ? "default" : "secondary"}
            className="h-11 rounded-full px-6 text-sm font-semibold"
          >
            {isLastQuestion ? (
              <>
                <Send />
                제출하기
              </>
            ) : (
              <>
                다음 질문
                <ArrowRight />
              </>
            )}
          </Button>
        </div>
      </section>

      {/* Back link */}
      <div className="flex justify-center">
        <Button
          type="button"
          variant="ghost"
          onClick={() => { stopAll(); router.push(ROUTES.practice); }}
          className="text-muted-foreground"
        >
          <ArrowLeft />
          주제 선택으로 돌아가기
        </Button>
      </div>
    </PageShell>
  );
}
