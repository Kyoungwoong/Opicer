"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Eye,
  EyeOff,
  LoaderCircle,
  Mic,
  MicOff,
  RotateCcw,
  Send,
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
import { Skeleton } from "@/components/ui/skeleton";
import {
  fetchPracticeQuestions,
  submitPracticeSession,
  submitTopicSelection,
  transcribeAudio,
} from "@/features/practice/api";
import { ROUTES } from "@/lib/routes";
import type { PracticeAnswer, PracticeQuestion, TargetLevel } from "@/features/practice/types";
import { MicLevelMeter } from "@/features/practice/components/MicLevelMeter";
import { AnswerEvaluationPanel } from "@/features/practice/components/evaluation/AnswerEvaluationPanel";
import { EvaluationStatusNotice } from "@/features/practice/components/evaluation/EvaluationStatusNotice";
import { isEvaluationInProgress, useSessionEvaluation } from "@/features/practice/use-session-evaluation";
import { historyPath } from "@/features/practice/history-format";
import { Mascot } from "@/components/brand/Mascot";

const PRACTICE_DURATION_SECONDS = 120;
const REPLAY_WINDOW_SECONDS = 5;
/** Upper bound the server accepts for an answer's recording length. */
const MAX_DURATION_SECONDS = 600;

type Mode = "loading" | "playing" | "recording" | "submitting" | "summary";

type Props = {
  topicId: string;
  topicSelectionId: string;
  targetLevel: TargetLevel;
  userLabel?: string;
  onLogout?: () => void;
};

export function PracticeSessionView({ topicId, topicSelectionId, targetLevel, userLabel, onLogout }: Props) {
  const router = useRouter();

  // ── Core state ─────────────────────────────────────────────
  const [questions, setQuestions] = useState<PracticeQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<PracticeAnswer[]>([]);
  const [mode, setMode] = useState<Mode>("loading");
  const [error, setError] = useState<string | null>(null);

  // Saved practice session (from submit); its evaluation is polled on the summary screen.
  const [sessionId, setSessionId] = useState<string | null>(null);
  const evaluation = useSessionEvaluation(sessionId);

  // ── Timer / replay state ────────────────────────────────────
  const [remainingSeconds, setRemainingSeconds] = useState(PRACTICE_DURATION_SECONDS);
  const [replayWindow, setReplayWindow] = useState(0);
  const [hasReplayed, setHasReplayed] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [timeExpired, setTimeExpired] = useState(false);
  // Live mic stream while recording, for the input level meter only.
  const [micStream, setMicStream] = useState<MediaStream | null>(null);
  // Like the real test, the question is audio-first; the text stays hidden unless asked for.
  const [showQuestionText, setShowQuestionText] = useState(false);

  // ── Refs (always fresh, no stale closure risk) ──────────────
  const timerRef = useRef<number | null>(null);
  const replayTimerRef = useRef<number | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const recordingStartedAtRef = useRef(0);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const voicesRef = useRef<SpeechSynthesisVoice[]>([]);
  // Stable ref to goToNext — updated every render to avoid stale closures
  const goToNextRef = useRef<() => void>(() => {});
  // answersRef mirrors answers state for sync access inside async callbacks
  const answersRef = useRef<PracticeAnswer[]>([]);
  // pendingSubmitRef signals that onstop should trigger transcription
  const pendingSubmitRef = useRef(false);

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
          targetLevel,
          updated.map((a) => ({
            questionId: a.questionId,
            questionText: a.questionText,
            transcript: a.transcript ?? null,
            durationSeconds: a.durationSeconds ?? null,
          }))
        );
        setSessionId(result.sessionId);
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
        setMicStream(null);
        const blob = new Blob(chunksRef.current, { type: "audio/webm" });
        const url = URL.createObjectURL(blob);
        const elapsedSeconds = Math.round((Date.now() - recordingStartedAtRef.current) / 1000);
        const newAnswer: PracticeAnswer = {
          questionId: question.id,
          questionText: question.promptText,
          audioUrl: url,
          recordedAt: new Date().toISOString(),
          durationSeconds: Math.min(Math.max(elapsedSeconds, 1), MAX_DURATION_SECONDS),
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
      recordingStartedAtRef.current = Date.now();
      setIsRecording(true);
      setMicStream(stream);
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
          <LoaderCircle aria-hidden className="size-8 animate-spin text-primary-strong" />
          <p className="text-sm text-muted-foreground">답변을 텍스트로 옮기고 있어요…</p>
        </div>
      </PageShell>
    );
  }

  // ── Summary screen ───────────────────────────────────────────
  if (mode === "summary") {
    const evaluatedSession = evaluation.session;
    const serverAnswers = new Map(evaluatedSession?.answers.map((a) => [a.questionId, a]) ?? []);
    const reportTarget = evaluatedSession?.targetLevel ?? targetLevel;
    // A re-recorded answer moves to the end of `answers`; show them in question order.
    const questionIndex = (questionId: string) => questions.findIndex((q) => q.id === questionId);
    const orderedAnswers = [...answers].sort((a, b) => questionIndex(a.questionId) - questionIndex(b.questionId));
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
            description={`${topicTitle} · ${answers.length}문항 답변 완료 · 목표 ${targetLevel}`}
          />
        </section>

        {evaluatedSession ? (
          <EvaluationStatusNotice
            session={evaluatedSession}
            timedOut={evaluation.timedOut}
            pollFailed={evaluation.pollFailed}
            retrying={evaluation.retrying}
            retryError={evaluation.retryError}
            historyHref={historyPath(evaluatedSession.id)}
            onRetry={evaluation.retry}
            onCheckAgain={evaluation.checkAgain}
          />
        ) : sessionId && (evaluation.missing || evaluation.timedOut) ? (
          // The detail never loaded, so EvaluationStatusNotice has nothing to render from.
          <Alert>
            <AlertCircle />
            <AlertTitle>
              {evaluation.missing ? "연습 기록을 찾을 수 없어요" : "평가 결과를 불러오지 못했어요"}
            </AlertTitle>
            <AlertDescription className="flex flex-col gap-3">
              <p>
                {evaluation.missing
                  ? "기록이 삭제되었을 수 있어요. 마이페이지에서 기록을 확인해 주세요."
                  : "연결 상태를 확인한 뒤 다시 시도해 주세요. 평가는 서버에서 계속 진행돼요."}
              </p>
              <div className="flex flex-wrap gap-2">
                {!evaluation.missing && (
                  <Button type="button" variant="outline" size="sm" onClick={evaluation.checkAgain} className="rounded-full px-4">
                    <RotateCcw />
                    다시 확인
                  </Button>
                )}
                <Button asChild variant="ghost" size="sm" className="rounded-full px-4">
                  <Link href={evaluation.missing ? ROUTES.mypage : historyPath(sessionId)}>마이페이지에서 보기</Link>
                </Button>
              </div>
            </AlertDescription>
          </Alert>
        ) : sessionId ? (
          <Skeleton className="h-24 rounded-3xl" />
        ) : null}


        <ol className="flex flex-col gap-5">
          {orderedAnswers.map((answer, idx) => {
            const result = serverAnswers.get(answer.questionId);
            const transcript = answer.transcript ?? result?.transcript ?? null;
            return (
              <li key={answer.questionId} className="flex flex-col gap-4 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
                <div className="flex flex-col gap-1.5">
                  <Eyebrow>질문 {idx + 1}</Eyebrow>
                  <p className="text-base font-semibold leading-relaxed">{answer.questionText}</p>
                </div>

                {/* Transcript + audio */}
                <div className="rounded-2xl border border-primary/20 bg-accent/60 p-4">
                  <p className="text-xs font-semibold text-primary-strong">내 답변 · 음성 인식 결과(추정)</p>
                  {transcript ? (
                    <p className="mt-2 text-sm leading-relaxed">{transcript}</p>
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

                {result?.evaluation ? (
                  <div className="border-t pt-5">
                    <AnswerEvaluationPanel
                      evaluation={result.evaluation}
                      transcript={transcript}
                      targetLevel={reportTarget}
                      variant="coach"
                    />
                  </div>
                ) : result?.evaluationStatus === "SKIPPED" ? (
                  <p className="text-sm text-muted-foreground">답변이 비어 있거나 15단어보다 짧아서 평가하지 않았어요.</p>
                ) : result?.evaluationStatus === "FAILED" ? (
                  <p className="text-sm text-destructive">이 답변은 평가하지 못했어요.</p>
                ) : result?.evaluationStatus === "PENDING" && isEvaluationInProgress(evaluatedSession?.evaluationStatus) ? (
                  <p className="flex items-center gap-2 text-sm text-muted-foreground">
                    <LoaderCircle aria-hidden className="size-4 animate-spin" />
                    평가 중…
                  </p>
                ) : null}
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
                router.push(`${ROUTES.practiceSession(topicId)}?selectionId=${selection.id}&target=${targetLevel}`);
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
            <LoaderCircle role="status" aria-label="불러오는 중" className="size-8 animate-spin text-primary-strong" />
          )}
        </div>
      </PageShell>
    );
  }

  // ── Practice screen ──────────────────────────────────────────
  const timeLabel = `${Math.floor(remainingSeconds / 60).toString().padStart(2, "0")}:${(remainingSeconds % 60)
    .toString()
    .padStart(2, "0")}`;

  const interviewer =
    mode === "playing"
      ? { expression: "talk" as const, caption: "오피가 질문을 읽고 있어요" }
      : isRecording
        ? { expression: "listen" as const, caption: "답변을 듣고 있어요. 편하게 말해 보세요" }
        : { expression: "smile" as const, caption: "준비되면 다음 질문으로 넘어가세요" };

  return (
    <PageShell width="lg" nav={nav}>
      {/* Header */}
      <section className="flex flex-col gap-4 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
        <SectionHeader as="h1" title={topicTitle} description={`질문 ${currentIndex + 1} / ${questions.length}`} />
        <ol aria-hidden className="flex gap-1.5">
          {questions.map((question, index) => (
            <li
              key={question.id}
              className={cn(
                "h-1.5 flex-1 rounded-full transition-colors",
                index < currentIndex ? "bg-primary" : index === currentIndex ? "bg-primary/50" : "bg-muted"
              )}
            />
          ))}
        </ol>
      </section>

      <div className="grid gap-4 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-6">
        {/* Interviewer: a compact row on phones so the audio controls stay above the fold. */}
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

        {/* Audio panel */}
        <section aria-label="오디오 패널" className="flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between gap-3">
              <Eyebrow>질문 {currentIndex + 1}</Eyebrow>
              {replayWindow > 0 && !hasReplayed && (
                <Button type="button" variant="outline" size="sm" onClick={handleReplay} className="rounded-full px-3">
                  <RotateCcw />
                  다시 듣기 ({replayWindow}s)
                </Button>
              )}
            </div>

            <div className="flex items-center gap-4 rounded-2xl bg-secondary px-4 py-4">
              <span
                className={cn(
                  "grid size-12 shrink-0 place-items-center rounded-full",
                  mode === "playing" ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
                )}
              >
                <Volume2 aria-hidden className="size-5" />
              </span>
              {/* Equalizer: moves only while the question is being read. */}
              <div aria-hidden className="flex h-10 flex-1 items-center gap-1">
                {Array.from({ length: 16 }, (_, i) => (
                  <span
                    key={i}
                    className={cn(
                      "h-full flex-1 origin-center rounded-full",
                      mode === "playing"
                        ? "animate-eq-bar bg-primary motion-reduce:animate-none"
                        : "scale-y-[0.2] bg-muted-foreground/30"
                    )}
                    style={mode === "playing" ? { animationDelay: `${(i % 5) * 0.12 + (i % 3) * 0.07}s` } : undefined}
                  />
                ))}
              </div>
            </div>

            <Button
              type="button"
              variant="ghost"
              size="sm"
              aria-expanded={showQuestionText}
              aria-controls="practice-question-text"
              onClick={() => setShowQuestionText((v) => !v)}
              className="self-start rounded-full px-3 text-muted-foreground"
            >
              {showQuestionText ? <EyeOff /> : <Eye />}
              {showQuestionText ? "질문 텍스트 숨기기" : "질문 텍스트 보기"}
            </Button>
            <div id="practice-question-text" hidden={!showQuestionText} className="flex flex-col gap-2">
              <h2 className="text-lg font-semibold leading-relaxed sm:text-xl">
                {currentQuestion?.promptText ?? "질문을 불러오는 중입니다."}
              </h2>
              {currentQuestion?.structuralHint ? (
                <p className="rounded-xl bg-muted px-3 py-2 text-sm text-muted-foreground">{currentQuestion.structuralHint}</p>
              ) : null}
            </div>
            {!showQuestionText && (
              <p className="text-xs text-muted-foreground">실제 시험처럼 질문은 소리로만 나와요. 필요하면 텍스트를 열어 보세요.</p>
            )}
          </div>

          <div className="flex flex-col gap-3 border-t pt-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              {/* Only the status pill is live; the countdowns change every second. */}
              <div aria-live="polite" className="flex">
                {mode === "playing" ? (
                  <span className="inline-flex items-center gap-2 rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-foreground">
                    <Volume2 aria-hidden className="size-4" />
                    질문 재생 중…
                  </span>
                ) : isRecording ? (
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
                    대기 중
                  </span>
                )}
              </div>
              <div
                role="timer"
                aria-live={warning ? "assertive" : "off"}
                className={cn(
                  "flex items-center gap-2 rounded-full px-4 py-2",
                  warning ? "bg-destructive/10 text-destructive" : "bg-secondary text-foreground"
                )}
              >
                <Timer aria-hidden className="size-4 text-muted-foreground" />
                <span className="sr-only">남은 시간</span>
                <span className={cn("text-lg font-bold tabular-nums", warning && "animate-pulse")}>{timeLabel}</span>
                {warning ? <span className="text-xs font-semibold">마감 임박</span> : null}
              </div>
            </div>
            <Progress
              value={(remainingSeconds / PRACTICE_DURATION_SECONDS) * 100}
              aria-label="남은 답변 시간"
              className={cn("h-1.5", warning && "*:data-[slot=progress-indicator]:bg-destructive")}
            />
            <MicLevelMeter stream={micStream} />
          </div>

          {error ? (
            <Alert variant="destructive">
              <AlertCircle />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          ) : null}

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
      </div>

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
