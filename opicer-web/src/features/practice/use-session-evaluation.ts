"use client";

import { useCallback, useEffect, useState } from "react";
import { fetchPracticeSession, retryPracticeEvaluation } from "@/features/practice/api";
import type { PracticeSessionDetail, SessionEvaluationStatus } from "@/features/practice/types";

const POLL_INTERVAL_MS = 3000;
/** Past this the page stops polling and points to the history page, where the result lands anyway. */
const POLL_TIMEOUT_MS = 4 * 60 * 1000;

export function isEvaluationInProgress(status: SessionEvaluationStatus | null | undefined): boolean {
  return status === "PENDING" || status === "RUNNING";
}

export function isEvaluationRetryable(status: SessionEvaluationStatus | null | undefined): boolean {
  return status === "FAILED" || status === "PARTIAL";
}

/**
 * Polls a submitted session until its evaluation settles (DONE / PARTIAL / FAILED, or no evaluation at all).
 * Pass null until the session id is known.
 */
export function useSessionEvaluation(sessionId: string | null) {
  const [session, setSession] = useState<PracticeSessionDetail | null>(null);
  const [timedOut, setTimedOut] = useState(false);
  const [pollFailed, setPollFailed] = useState(false);
  const [retrying, setRetrying] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);
  // Bumped to start a fresh polling run (after a retry, or "check again" once timed out).
  const [run, setRun] = useState(0);

  useEffect(() => {
    if (!sessionId) return;
    let alive = true;
    let timer: number | undefined;
    const startedAt = Date.now();

    const tick = async () => {
      try {
        const detail = await fetchPracticeSession(sessionId);
        if (!alive) return;
        setPollFailed(false);
        setSession(detail);
        if (!detail || !isEvaluationInProgress(detail.evaluationStatus)) return;
      } catch {
        // A dropped request is not fatal; keep polling until the timeout.
        if (!alive) return;
        setPollFailed(true);
      }
      if (Date.now() - startedAt >= POLL_TIMEOUT_MS) {
        setTimedOut(true);
        return;
      }
      timer = window.setTimeout(tick, POLL_INTERVAL_MS);
    };
    void tick();

    return () => {
      alive = false;
      window.clearTimeout(timer);
    };
  }, [sessionId, run]);

  const checkAgain = useCallback(() => {
    setTimedOut(false);
    setRun((n) => n + 1);
  }, []);

  const retry = useCallback(async () => {
    if (!sessionId) return;
    setRetrying(true);
    setRetryError(null);
    try {
      const evaluationStatus = await retryPracticeEvaluation(sessionId);
      setSession((prev) => (prev ? { ...prev, evaluationStatus } : prev));
      setTimedOut(false);
      setRun((n) => n + 1);
    } catch (err) {
      setRetryError(err instanceof Error ? err.message : "평가를 다시 요청하지 못했습니다.");
    } finally {
      setRetrying(false);
    }
  }, [sessionId]);

  return { session, timedOut, pollFailed, retrying, retryError, checkAgain, retry };
}
