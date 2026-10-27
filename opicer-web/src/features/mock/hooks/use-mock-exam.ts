"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { transcribeAudio } from "@/features/practice/api";
import { useAnswerRecorder, type RecordedClip } from "@/features/practice/hooks/use-answer-recorder";
import { useQuestionAudio } from "@/features/practice/hooks/use-question-audio";
import { adjustMockTest, submitMockTest } from "@/features/mock/api";
import { EXAM_SECONDS, MAX_PLAYS, TRANSCRIBE_CONCURRENCY, createLimiter } from "@/features/mock/exam";
import type { DifficultyAdjustment, MockItem, MockSubmitResult } from "@/features/mock/types";

/**
 * - `playing`: the question is being read; recording starts when it ends.
 * - `answering`: the answer is being recorded (or the microphone failed and the learner can only move on).
 * - `difficulty`: part 1 is over and the second part's difficulty is being chosen.
 * - `submitting` → `submitted`, or `submitFailed` with a retry.
 */
export type ExamPhase = "playing" | "answering" | "difficulty" | "submitting" | "submitted" | "submitFailed";

type Answer = { durationSeconds: number; transcript: Promise<string | null> };

/**
 * One mock test from its first question to the submit call. The 40-minute clock starts on mount and submits
 * whatever was recorded when it runs out. Each finished recording is turned into text in the background
 * (three at a time) and its audio is dropped once the text is back; nothing is uploaded or kept.
 */
export function useMockExam(testId: string, firstPart: MockItem[]) {
  const [items, setItems] = useState(firstPart);
  const [index, setIndex] = useState(0);
  const [phase, setPhase] = useState<ExamPhase>("playing");
  const [plays, setPlays] = useState(1);
  const [remainingSeconds, setRemainingSeconds] = useState(EXAM_SECONDS);
  const [audioFailed, setAudioFailed] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [adjusting, setAdjusting] = useState(false);
  const [recorded, setRecorded] = useState(0);
  const [transcribed, setTranscribed] = useState(0);
  const [result, setResult] = useState<MockSubmitResult | null>(null);

  const { start: startRecorder, stop: stopRecorder, isActive, isRecording, micStream } = useAnswerRecorder();
  const questionAudio = useQuestionAudio(() => setAudioFailed(true));

  const startedAtRef = useRef(Date.now());
  const answersRef = useRef(new Map<string, Answer>());
  const limiterRef = useRef(createLimiter(TRANSCRIBE_CONCURRENCY));
  /** Recording generations whose clip must be thrown away (the learner replayed the question). */
  const discardedRef = useRef(new Set<number>());
  const generationRef = useRef(0);
  /** Bumped whenever playback is started or abandoned, so a stale "ended" callback cannot start recording. */
  const playTokenRef = useRef(0);
  /** Set when the exam should be submitted as soon as the current recording has stopped. */
  const finishAfterStopRef = useRef(false);
  const finishedRef = useRef(false);

  const current = items[index];
  const part1Count = items.filter((item) => item.part === 1).length;
  const hasPart2 = items.length > part1Count;

  const elapsedSeconds = () => Math.min(EXAM_SECONDS, Math.round((Date.now() - startedAtRef.current) / 1000));

  const submit = useCallback(async () => {
    setPhase("submitting");
    setError(null);
    const entries = [...answersRef.current.entries()];
    const answers = await Promise.all(
      entries.map(async ([itemId, answer]) => ({
        itemId,
        transcript: await answer.transcript,
        durationSeconds: answer.durationSeconds,
      }))
    );
    try {
      setResult(await submitMockTest(testId, answers, elapsedSeconds()));
      setPhase("submitted");
    } catch (err) {
      setError(err instanceof Error ? err.message : "제출하지 못했어요.");
      setPhase("submitFailed");
    }
  }, [testId]);

  const keep = useCallback((item: MockItem, clip: RecordedClip) => {
    const transcript = limiterRef.current(() => transcribeAudio(clip.audioUrl, item.questionText))
      .then((text) => text?.trim() || null)
      .catch(() => null)
      .finally(() => {
        URL.revokeObjectURL(clip.audioUrl);
        setTranscribed((n) => n + 1);
      });
    answersRef.current.set(item.id, { durationSeconds: clip.durationSeconds, transcript });
    setRecorded(answersRef.current.size);
  }, []);

  const startRecording = useCallback(
    async (item: MockItem) => {
      const generation = ++generationRef.current;
      setPhase("answering");
      const started = await startRecorder((clip) => {
        if (discardedRef.current.delete(generation)) {
          URL.revokeObjectURL(clip.audioUrl);
          return;
        }
        keep(item, clip);
        if (finishAfterStopRef.current) {
          finishAfterStopRef.current = false;
          void submit();
        }
      });
      if (!started) setError("마이크를 쓸 수 없어요. 브라우저에서 마이크 권한을 허용한 뒤 다음 문항부터 답해 주세요.");
    },
    [startRecorder, keep, submit]
  );

  /** Stops everything and submits once the last recording (if any) has been kept. */
  const finish = useCallback(() => {
    if (finishedRef.current) return;
    finishedRef.current = true;
    questionAudio.stop();
    if (isActive()) {
      finishAfterStopRef.current = true;
      setPhase("submitting");
      stopRecorder();
    } else {
      void submit();
    }
  }, [questionAudio, isActive, stopRecorder, submit]);

  // Read the question whenever a new one comes up; answering starts when it ends.
  useEffect(() => {
    if (phase !== "playing" || !current) return;
    const token = ++playTokenRef.current;
    setAudioFailed(false);
    questionAudio.play({ promptText: current.questionText, promptAudioUrl: current.promptAudioUrl }, () => {
      // A cancelled utterance may still report that it ended.
      if (playTokenRef.current === token) void startRecording(current);
    });
    return () => {
      playTokenRef.current += 1;
      questionAudio.stop();
    };
    // Only a new question (or a replay, which sets `playing` again) should start playback.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current?.id, phase === "playing", plays]);

  // When the question cannot be played, show its text and let the learner answer anyway.
  useEffect(() => {
    if (audioFailed && phase === "playing" && current) void startRecording(current);
  }, [audioFailed, phase, current, startRecording]);

  // The exam clock keeps running through the difficulty choice, like the real test.
  const finishRef = useRef(finish);
  useEffect(() => {
    finishRef.current = finish;
  });
  useEffect(() => {
    const timer = window.setInterval(() => {
      const left = EXAM_SECONDS - Math.floor((Date.now() - startedAtRef.current) / 1000);
      setRemainingSeconds(Math.max(0, left));
      if (left <= 0) {
        window.clearInterval(timer);
        finishRef.current();
      }
    }, 1000);
    return () => window.clearInterval(timer);
  }, []);

  // Leaving the page mid-exam would lose the recordings.
  useEffect(() => {
    return () => {
      questionAudio.stop();
      stopRecorder();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** One replay per question; the answer recorded so far is dropped and recording restarts after it. */
  const replay = () => {
    if (plays >= MAX_PLAYS || phase !== "answering" || !current) return;
    if (isActive()) discardedRef.current.add(generationRef.current);
    stopRecorder();
    setError(null);
    setPlays((n) => n + 1);
    setPhase("playing");
  };

  /** Moves on without a way back: to the next question, the difficulty choice, or submission. */
  const next = () => {
    if (phase !== "answering" || !current) return;
    questionAudio.stop();
    const isLast = hasPart2 ? index === items.length - 1 : index === part1Count - 1;
    if (isLast && hasPart2) {
      finish();
      return;
    }
    stopRecorder();
    setError(null);
    if (isLast) {
      setPhase("difficulty");
      return;
    }
    setPlays(1);
    setIndex((i) => i + 1);
    setPhase("playing");
  };

  const chooseDifficulty = async (difficulty: DifficultyAdjustment) => {
    if (adjusting) return;
    setAdjusting(true);
    setError(null);
    try {
      const part2 = await adjustMockTest(testId, difficulty);
      if (finishedRef.current) return;
      if (part2.length === 0) {
        finish();
        return;
      }
      setItems((prev) => [...prev, ...part2]);
      setPlays(1);
      setIndex((i) => i + 1);
      setPhase("playing");
    } catch (err) {
      setError(err instanceof Error ? err.message : "다음 문항을 불러오지 못했어요.");
    } finally {
      setAdjusting(false);
    }
  };

  return {
    items,
    index,
    current,
    phase,
    canReplay: phase === "answering" && plays < MAX_PLAYS,
    playsLeft: MAX_PLAYS - plays,
    remainingSeconds,
    audioFailed,
    error,
    adjusting,
    isRecording,
    micStream,
    isLastQuestion: hasPart2 && index === items.length - 1,
    recorded,
    transcribed,
    result,
    replay,
    next,
    chooseDifficulty,
    finish,
    retrySubmit: () => void submit(),
  };
}
