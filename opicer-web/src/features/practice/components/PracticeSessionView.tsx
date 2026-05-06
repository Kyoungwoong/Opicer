"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { TopNav } from "@/components/common/TopNav";
import {
  analyzeAnswer,
  fetchPracticeQuestions,
  improveScript,
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

  // text Core state text
  const [questions, setQuestions] = useState<PracticeQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<PracticeAnswer[]>([]);
  const [mode, setMode] = useState<Mode>("loading");
  const [error, setError] = useState<string | null>(null);

  // text AI state per question text
  const [aiStates, setAiStates] = useState<Record<string, QuestionAiState>>({});

  // text Timer / replay state text
  const [remainingSeconds, setRemainingSeconds] = useState(PRACTICE_DURATION_SECONDS);
  const [replayWindow, setReplayWindow] = useState(0);
  const [hasReplayed, setHasReplayed] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [timeExpired, setTimeExpired] = useState(false);

  // text Refs (always fresh, no stale closure risk) text
  const timerRef = useRef<number | null>(null);
  const replayTimerRef = useRef<number | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const voicesRef = useRef<SpeechSynthesisVoice[]>([]);
  // Stable ref to goToNext text updated every render to avoid stale closures
  const goToNextRef = useRef<() => void>(() => {});
  // answersRef mirrors answers state for sync access inside async callbacks
  const answersRef = useRef<PracticeAnswer[]>([]);
  // pendingSubmitRef signals that onstop should trigger transcription
  const pendingSubmitRef = useRef(false);

  const currentQuestion = questions[currentIndex];
  const isLastQuestion = currentIndex === questions.length - 1;

  // text Voice preload text
  useEffect(() => {
    if (!("speechSynthesis" in window)) return;
    const load = () => { voicesRef.current = window.speechSynthesis.getVoices(); };
    load();
    window.speechSynthesis.addEventListener("voiceschanged", load);
    return () => window.speechSynthesis.removeEventListener("voiceschanged", load);
  }, []);

  // text Fetch questions text
  useEffect(() => {
    fetchPracticeQuestions(topicId)
      .then((data) => {
        if (data.length === 0) {
          setError("text Topictext text Questiontext text.");
          return;
        }
        setQuestions(data);
        setMode("playing");
      })
      .catch((err: unknown) => {
        const message = err instanceof Error ? err.message : "Questiontext text text.";
        setError(message);
      });
  }, [topicId]);

  // text Helpers text
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

  // text Transcribe all answers and switch to summary text
  const doTranscribeAndSummary = (snapshot: PracticeAnswer[]) => {
    setMode("submitting");
    Promise.all(
      snapshot.map(async (a) => {
        if (!a.audioUrl) return a;
        try {
          const transcript = await transcribeAudio(a.audioUrl, a.questionText);
          return { ...a, transcript };
        } catch (err) {
          const transcribeError = err instanceof Error ? err.message : "text text";
          return { ...a, transcribeError };
        }
      })
    ).then(async (updated) => {
      try {
        await submitPracticeSession(topicSelectionId);
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
      setError("text text text. text text text text.");
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
      audioRef.current.play().catch(() => setError("text text text."));
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
        // Recording is active text flag it; onstop will call doTranscribeAndSummary
        pendingSubmitRef.current = true;
        stopAll();
        setMode("submitting");
      } else {
        // No active recording text safe to submit immediately
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

  // text Auto-advance when timer expires text
  useEffect(() => {
    if (timeExpired) {
      stopRecording();
      goToNextRef.current();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [timeExpired]);

  // text Auto-play when question changes text
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

  // text Replay button handler text
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

  // text AI actions text
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
    } catch (err) {
      const message = err instanceof Error ? err.message : "mintext text.";
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
    } catch (err) {
      const message = err instanceof Error ? err.message : "Edittext text.";
      setAiStates((prev) => ({
        ...prev,
        [id]: { ...prev[id]!, isImproving: false, improveError: message },
      }));
    }
  };

  // text Cleanup on unmount text
  useEffect(() => {
    return () => { stopAll(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const topicTitle = currentQuestion?.topic ?? questions[0]?.topic ?? "text";
  const warning = remainingSeconds <= 10 && remainingSeconds > 0;

  // text Submitting screen text
  if (mode === "submitting") {
    return (
      <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
        <div className="mx-auto flex max-w-4xl flex-col gap-8">
          <TopNav userLabel={userLabel} onLogout={onLogout} />
          <div className="flex flex-col items-center justify-center py-24 gap-4">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-[var(--accent)] border-t-transparent" />
            <p className="text-sm text-[var(--muted)]">text mintext text</p>
          </div>
        </div>
      </div>
    );
  }

  // text Summary screen text
  if (mode === "summary") {
    return (
      <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
        <div className="mx-auto flex max-w-4xl flex-col gap-8">
          <TopNav userLabel={userLabel} onLogout={onLogout} />

          {error ? (
            <section className="rounded-2xl border border-rose-300 bg-rose-50 p-4 text-sm text-rose-800">
              <p className="font-semibold">크레딧 차감/제출 처리에 실패했습니다.</p>
              <p className="mt-1">{error}</p>
            </section>
          ) : null}

          <section className="rounded-[32px] border border-black/5 bg-white/70 p-8 shadow-sm">
            <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">Practice Summary</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight">Practice complete</h1>
            <p className="mt-1 text-sm text-[var(--muted)]">{topicTitle} text {answers.length}questions answered</p>
          </section>

          <div className="space-y-6">
            {answers.map((answer, idx) => {
              const ai = getOrInitAiState(answer.questionId);
              return (
                <div
                  key={answer.questionId}
                  className="rounded-[24px] border border-black/10 bg-white/70 p-5 shadow-sm"
                >
                  <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
                    Question {idx + 1}
                  </p>
                  <p className="mt-2 text-base font-semibold">{answer.questionText}</p>

                  {/* Transcript + audio */}
                  <div className="mt-4 rounded-2xl border border-[var(--accent)]/20 bg-[var(--accent)]/10 p-4">
                    <p className="text-xs font-semibold text-[var(--accent-strong)]">Your Answer</p>
                    {answer.transcript ? (
                      <p className="mt-2 text-sm leading-relaxed">{answer.transcript}</p>
                    ) : answer.transcribeError ? (
                      <p className="mt-2 text-sm text-red-500">Transcription failed: {answer.transcribeError}</p>
                    ) : (
                      <p className="mt-2 text-sm text-[var(--muted)]">No transcript</p>
                    )}
                    {answer.audioUrl ? (
                      <audio controls src={answer.audioUrl} className="mt-3 w-full" />
                    ) : (
                      <p className="mt-2 text-sm text-[var(--muted)]">Recordingtext text text.</p>
                    )}
                  </div>

                  {/* Action buttons text only if transcript exists */}
                  {answer.transcript && (
                    <div className="mt-4 flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => handleAnalyze(answer)}
                        disabled={ai.isAnalyzing || ai.analysis !== null}
                        className={`rounded-full px-5 py-2 text-sm font-semibold transition-colors ${
                          ai.isAnalyzing || ai.analysis !== null
                            ? "cursor-not-allowed bg-black/5 text-[var(--muted)]"
                            : "bg-[var(--accent)] text-white hover:bg-[var(--accent-strong)]"
                        }`}
                      >
                        {ai.isAnalyzing ? "mintext text" : ai.analysis ? "mintext text" : "mintext"}
                      </button>
                      <button
                        type="button"
                        onClick={() => handleImprove(answer)}
                        disabled={ai.isImproving || ai.improvement !== null}
                        className={`rounded-full px-5 py-2 text-sm font-semibold transition-colors ${
                          ai.isImproving || ai.improvement !== null
                            ? "cursor-not-allowed bg-black/5 text-[var(--muted)]"
                            : "bg-black text-white hover:bg-black/80"
                        }`}
                      >
                        {ai.isImproving ? "Improving..." : ai.improvement ? "Improvement complete" : "Improve"}
                      </button>
                    </div>
                  )}

                  {/* Analysis result */}
                  {ai.analyzeError && (
                    <p className="mt-3 text-sm text-red-600">{ai.analyzeError}</p>
                  )}
                  {ai.analysis && (
                    <div className="mt-4 rounded-2xl border border-black/10 bg-white/80 p-4 text-sm leading-relaxed whitespace-pre-wrap">
                      <p className="mb-2 text-xs font-semibold uppercase tracking-[0.2em] text-[var(--muted)]">AI mintext</p>
                      {ai.analysis}
                    </div>
                  )}

                  {/* Improvement diff */}
                  {ai.improveError && (
                    <p className="mt-3 text-sm text-red-600">{ai.improveError}</p>
                  )}
                  {ai.improvement && answer.transcript && (
                    <div className="mt-4">
                      <TranscriptDiff
                        original={answer.transcript}
                        improved={ai.improvement}
                      />
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          <div className="flex flex-col items-center gap-3 pb-10">
            <button
              type="button"
              onClick={() => router.push(ROUTES.practice)}
              className="rounded-full bg-[var(--accent)] px-8 py-3 text-sm font-semibold text-white hover:bg-[var(--accent-strong)]"
            >
              Practice another topic
            </button>
              <button
                type="button"
                onClick={async () => {
                  try {
                    const selection = await submitTopicSelection(topicId);
                    router.push(`${ROUTES.practiceSession(topicId)}?selectionId=${selection.id}`);
                  } catch (err) {
                    const message = err instanceof Error ? err.message : "Failed to create a new practice session.";
                    setError(message);
                  }
                }}
                className="text-sm text-[var(--muted)] hover:underline"
              >
                Retry same topic
              </button>
          </div>
        </div>
      </div>
    );
  }

  // text Loading / error screen text
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
                  text Topic Selecttext text
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

  // text Practice screen text
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
                Question {currentIndex + 1} / {questions.length}
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
              {warning ? <p className="text-xs font-semibold">Time almost up</p> : null}
            </div>
          </div>
        </section>

        {/* Question card */}
        <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
          <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Question</p>
          <h2 className="mt-3 text-2xl font-semibold leading-relaxed">
            {currentQuestion?.promptText ?? "Questiontext text text."}
          </h2>
          {currentQuestion?.structuralHint ? (
            <p className="mt-3 text-sm text-[var(--muted)]">{currentQuestion.structuralHint}</p>
          ) : null}

          {/* Status + replay */}
          <div className="mt-6 flex flex-wrap items-center gap-3">
            {mode === "playing" ? (
              <span className="flex items-center gap-2 rounded-full bg-[var(--accent)]/10 px-4 py-2 text-sm font-semibold text-[var(--accent-strong)]">
                <span className="h-2 w-2 rounded-full bg-[var(--accent)] animate-pulse" />
                Question text text
              </span>
            ) : (
              <span className={`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold ${
                isRecording
                  ? "bg-red-50 text-red-700"
                  : "bg-black/5 text-[var(--muted)]"
              }`}>
                <span className={`h-2 w-2 rounded-full ${isRecording ? "bg-red-500 animate-pulse" : "bg-black/20"}`} />
                {isRecording ? "Recording text" : "Idle"}
              </span>
            )}

            {/* Replay button text only during replay window */}
            {replayWindow > 0 && !hasReplayed && (
              <button
                type="button"
                onClick={handleReplay}
                className="rounded-full border border-[var(--accent)]/40 px-4 py-2 text-sm font-semibold text-[var(--accent-strong)] hover:bg-[var(--accent)]/10"
              >
                Replay ({replayWindow}s)
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
                Next Question text
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
                Submittext
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
            text Topic Selecttext text
          </button>
        </div>
      </div>
    </div>
  );
}
