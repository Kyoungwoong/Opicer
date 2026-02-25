"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { TopNav } from "@/components/common/TopNav";
import { fetchPracticeQuestions } from "@/features/practice/api";
import { ROUTES } from "@/lib/routes";
import type { PracticeAnswer, PracticeQuestion } from "@/features/practice/types";

const PRACTICE_DURATION_SECONDS = 120;
const REPLAY_WINDOW_SECONDS = 5;

type Props = {
  topicId: string;
  userLabel?: string;
  onLogout?: () => void;
};

export function PracticeSessionView({ topicId, userLabel, onLogout }: Props) {
  const router = useRouter();

  // ── Core state ─────────────────────────────────────────────
  const [questions, setQuestions] = useState<PracticeQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<PracticeAnswer[]>([]);
  const [mode, setMode] = useState<"loading" | "playing" | "recording" | "summary">("loading");
  const [error, setError] = useState<string | null>(null);

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
      .catch((err: any) => setError(err?.message ?? "질문을 불러오지 못했습니다."));
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

  // ── startRecording: use recorderRef.current check (not isRecording state)
  //    to avoid stale closure bug when called from audio onended callbacks
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
        setAnswers((prev) => {
          const next = prev.filter((a) => a.questionId !== question.id);
          return [
            ...next,
            {
              questionId: question.id,
              questionText: question.promptText,
              audioUrl: url,
              recordedAt: new Date().toISOString(),
            },
          ];
        });
      };
      recorderRef.current = recorder;
      recorder.start();
      setIsRecording(true);
    } catch {
      setError("마이크 접근이 필요합니다. 브라우저에서 마이크 권한을 허용해주세요.");
    }
  };

  // startTimer: only clears main timer (NOT replay timer)
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

  // ── Play audio for a question, call onEnded when done ───────
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

  // ── goToNext / goToSummary ───────────────────────────────────
  const goToNext = () => {
    stopAll();
    setHasReplayed(false);
    setReplayWindow(0);
    setRemainingSeconds(PRACTICE_DURATION_SECONDS);
    setTimeExpired(false);
    if (isLastQuestion) {
      setMode("summary");
    } else {
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [timeExpired]);

  // ── Auto-play when question changes ─────────────────────────
  useEffect(() => {
    if (mode !== "playing" || questions.length === 0) return;
    const question = questions[currentIndex];
    if (!question) return;

    // Reset state
    setHasReplayed(false);
    setReplayWindow(0);
    setRemainingSeconds(PRACTICE_DURATION_SECONDS);
    setTimeExpired(false);

    // Auto-play → when done: startReplayWindow + startRecording + startTimer
    // Note: startReplayWindow MUST be called AFTER startTimer to avoid
    //       startTimer's clearMainTimer accidentally clearing the replay timer
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

  // ── Summary screen ───────────────────────────────────────────
  if (mode === "summary") {
    return (
      <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
        <div className="mx-auto flex max-w-4xl flex-col gap-8">
          <TopNav userLabel={userLabel} onLogout={onLogout} />

          <section className="rounded-[32px] border border-black/5 bg-white/70 p-8 shadow-sm">
            <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">Practice Summary</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight">연습 완료</h1>
            <p className="mt-1 text-sm text-[var(--muted)]">{topicTitle} · {answers.length}문항 답변 완료</p>
          </section>

          <div className="space-y-6">
            {answers.map((answer, idx) => (
              <div
                key={answer.questionId}
                className="rounded-[24px] border border-black/10 bg-white/70 p-5 shadow-sm"
              >
                <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
                  Question {idx + 1}
                </p>
                <p className="mt-2 text-base font-semibold">{answer.questionText}</p>
                <div className="mt-4 rounded-2xl border border-[var(--accent)]/20 bg-[var(--accent)]/10 p-4">
                  <p className="text-xs font-semibold text-[var(--accent-strong)]">Your Answer</p>
                  {answer.audioUrl ? (
                    <audio controls src={answer.audioUrl} className="mt-2 w-full" />
                  ) : (
                    <p className="mt-2 text-sm text-[var(--muted)]">녹음된 답변이 없습니다.</p>
                  )}
                </div>
              </div>
            ))}
          </div>

          <div className="flex flex-col items-center gap-3 pb-10">
            <button
              type="button"
              onClick={() => router.push(ROUTES.practice)}
              className="rounded-full bg-[var(--accent)] px-8 py-3 text-sm font-semibold text-white hover:bg-[var(--accent-strong)]"
            >
              다른 주제 연습하기
            </button>
            <button
              type="button"
              onClick={() => {
                setAnswers([]);
                setCurrentIndex(0);
                setMode("playing");
              }}
              className="text-sm text-[var(--muted)] hover:underline"
            >
              같은 주제 다시 연습하기
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ── Loading / error screen ───────────────────────────────────
  if (mode === "loading") {
    return (
      <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
        <div className="mx-auto flex max-w-4xl flex-col gap-8">
          <TopNav userLabel={userLabel} onLogout={onLogout} />
          <div className="flex flex-col items-center justify-center py-24 gap-4">
            {error ? (
              <>
                <p className="text-red-600">{error}</p>
                <button
                  type="button"
                  onClick={() => router.push(ROUTES.practice)}
                  className="text-sm text-[var(--muted)] hover:underline"
                >
                  ← 주제 선택으로 돌아가기
                </button>
              </>
            ) : (
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-[var(--accent)] border-t-transparent" />
            )}
          </div>
        </div>
      </div>
    );
  }

  // ── Practice screen ──────────────────────────────────────────
  return (
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      <div className="mx-auto flex max-w-4xl flex-col gap-8">
        <TopNav userLabel={userLabel} onLogout={onLogout} />

        {/* Header */}
        <section className="rounded-[32px] border border-black/5 bg-white/70 p-8 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">Topic Practice</p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight">{topicTitle}</h1>
              <p className="mt-2 text-sm text-[var(--muted)]">
                질문 {currentIndex + 1} / {questions.length}
              </p>
            </div>
            <div
              className={`rounded-2xl border px-4 py-3 text-center ${
                warning
                  ? "border-red-400 bg-red-50 text-red-700 animate-pulse"
                  : "border-black/10 bg-white text-[var(--muted)]"
              }`}
            >
              <p className="text-xs uppercase tracking-[0.2em]">Time</p>
              <p className="mt-1 text-xl font-semibold tabular-nums">
                {Math.floor(remainingSeconds / 60).toString().padStart(2, "0")}:
                {(remainingSeconds % 60).toString().padStart(2, "0")}
              </p>
              {warning ? <p className="text-xs font-semibold">마감 임박</p> : null}
            </div>
          </div>
        </section>

        {/* Question card */}
        <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
          <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Question</p>
          <h2 className="mt-3 text-2xl font-semibold leading-relaxed">
            {currentQuestion?.promptText ?? "질문을 불러오는 중입니다."}
          </h2>
          {currentQuestion?.structuralHint ? (
            <p className="mt-3 text-sm text-[var(--muted)]">{currentQuestion.structuralHint}</p>
          ) : null}

          {/* Status + replay */}
          <div className="mt-6 flex flex-wrap items-center gap-3">
            {mode === "playing" ? (
              <span className="flex items-center gap-2 rounded-full bg-[var(--accent)]/10 px-4 py-2 text-sm font-semibold text-[var(--accent-strong)]">
                <span className="h-2 w-2 rounded-full bg-[var(--accent)] animate-pulse" />
                질문 재생 중…
              </span>
            ) : (
              <span className={`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold ${
                isRecording
                  ? "bg-red-50 text-red-700"
                  : "bg-black/5 text-[var(--muted)]"
              }`}>
                <span className={`h-2 w-2 rounded-full ${isRecording ? "bg-red-500 animate-pulse" : "bg-black/20"}`} />
                {isRecording ? "녹음 중…" : "대기 중"}
              </span>
            )}

            {/* Replay button — only during replay window */}
            {replayWindow > 0 && !hasReplayed && (
              <button
                type="button"
                onClick={handleReplay}
                className="rounded-full border border-[var(--accent)]/40 px-4 py-2 text-sm font-semibold text-[var(--accent-strong)] hover:bg-[var(--accent)]/10"
              >
                다시 듣기 ({replayWindow}s)
              </button>
            )}
          </div>

          {error ? <p className="mt-4 text-sm text-red-600">{error}</p> : null}

          {/* Navigation */}
          <div className="mt-6 flex justify-end">
            {!isLastQuestion ? (
              <button
                type="button"
                onClick={goToNext}
                disabled={mode === "playing"}
                className={`rounded-full px-6 py-2 text-sm font-semibold ${
                  mode === "playing"
                    ? "cursor-not-allowed bg-black/10 text-[var(--muted)]"
                    : "bg-black text-white hover:bg-black/80"
                }`}
              >
                다음 질문 →
              </button>
            ) : (
              <button
                type="button"
                onClick={goToNext}
                disabled={mode === "playing"}
                className={`rounded-full px-6 py-2 text-sm font-semibold ${
                  mode === "playing"
                    ? "cursor-not-allowed bg-black/10 text-[var(--muted)]"
                    : "bg-[var(--accent)] text-white hover:bg-[var(--accent-strong)]"
                }`}
              >
                제출하기
              </button>
            )}
          </div>
        </section>

        {/* Back link */}
        <div className="flex justify-center">
          <button
            type="button"
            onClick={() => { stopAll(); router.push(ROUTES.practice); }}
            className="text-sm text-[var(--muted)] hover:underline"
          >
            ← 주제 선택으로 돌아가기
          </button>
        </div>
      </div>
    </div>
  );
}
