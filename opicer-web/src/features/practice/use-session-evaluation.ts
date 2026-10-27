"use client";

import { useCallback, useEffect, useState } from "react";
import { fetchPracticeSession, retryPracticeEvaluation } from "@/features/practice/api";
import type { SessionEvaluationStatus } from "@/features/practice/types";

const POLL_INTERVAL_MS = 3000;
/** Past this the page stops polling and points to the history page, where the result lands anyway. */
const POLL_TIMEOUT_MS = 4 * 60 * 1000;

export function isEvaluationInProgress(status: SessionEvaluationStatus | null | undefined): boolean {
  return status === "PENDING" || status === "RUNNING";
}

export function isEvaluationRetryable(status: SessionEvaluationStatus | null | undefined): boolean {
  return status === "FAILED" || status === "PARTIAL";
}

/** Polls a submitted practice session; see {@link useEvaluationPolling}. */
export function useSessionEvaluation(sessionId: string | null) {
  return useEvaluationPolling(sessionId, fetchPracticeSession, retryPracticeEvaluation);
}

/**
 * Polls anything that is evaluated in the background (a practice session, a mock test) until its evaluation
 * settles (DONE / PARTIAL / FAILED, or no evaluation at all). Pass null until the id is known. `load` resolves
 * null for a 404; both functions should be stable (module-level).
 */
export function useEvaluationPolling<T extends { evaluationStatus: SessionEvaluationStatus | null }>(
  sessionId: string | null,
  load: (id: string) => Promise<T | null>,
  requestRetry: (id: string) => Promise<SessionEvaluationStatus>
) {
  const [session, setSession] = useState<T | null>(null);
  // The session is gone (404): deleted, or never saved.
  const [missing, setMissing] = useState(false);
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
        const detail = await load(sessionId);
        if (!alive) return;
        setPollFailed(false);
        setMissing(detail === null);
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
  }, [sessionId, run, load]);

  const checkAgain = useCallback(() => {
    setTimedOut(false);
    setRun((n) => n + 1);
  }, []);

  const retry = useCallback(async () => {
    if (!sessionId) return;
    setRetrying(true);
    setRetryError(null);
    try {
      const evaluationStatus = await requestRetry(sessionId);
      setSession((prev) => (prev ? { ...prev, evaluationStatus } : prev));
      setTimedOut(false);
      setRun((n) => n + 1);
    } catch (err) {
      setRetryError(err instanceof Error ? err.message : "평가를 다시 요청하지 못했습니다.");
    } finally {
      setRetrying(false);
    }
  }, [sessionId, requestRetry]);

  return { session, missing, timedOut, pollFailed, retrying, retryError, checkAgain, retry };
}
