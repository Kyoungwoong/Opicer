import type {
  PracticeAnswerInput,
  PracticeQuestion,
  PracticeSessionDetail,
  PracticeSessionSummary,
  PracticeSubmitResult,
  SessionEvaluationStatus,
  TargetLevel,
  TopicItem,
  TopicSelection,
} from "@/features/practice/types";


export async function fetchTopics(): Promise<TopicItem[]> {
  const res = await fetch(`/api/topics`, { cache: "no-store" });
  if (!res.ok) {
    throw new Error("Failed to load topics");
  }
  return (await res.json()) as TopicItem[];
}

export async function submitTopicSelection(
  topicId: string
): Promise<TopicSelection> {
  const res = await fetch(`/api/practice/topic-selections`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ topicId }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    const message = body?.message ?? "Failed to submit selection";
    throw new Error(message);
  }
  return (await res.json()) as TopicSelection;
}

export async function fetchPracticeQuestions(
  topicId: string
): Promise<PracticeQuestion[]> {
  if (!topicId) {
    throw new Error("주제를 선택한 뒤 연습을 시작해주세요.");
  }
  const res = await fetch(`/api/practice/topics/${topicId}/questions`, {
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error("Failed to load practice questions");
  }
  return (await res.json()) as PracticeQuestion[];
}

export async function transcribeAudio(
  audioUrl: string,
  questionText: string
): Promise<string> {
  const audioRes = await fetch(audioUrl);
  const audioBlob = await audioRes.blob();
  const formData = new FormData();
  formData.append("audio", audioBlob, "audio.webm");
  formData.append("questionText", questionText);

  const res = await fetch("/api/practice/transcribe", {
    method: "POST",
    body: formData,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "Transcription failed");
  }
  const body = (await res.json()) as { transcript: string };
  return body.transcript;
}

/** Charges the session once and queues its automatic evaluation against `targetLevel`. */
export async function submitPracticeSession(
  topicSelectionId: string,
  targetLevel: TargetLevel,
  answers: PracticeAnswerInput[] = []
): Promise<PracticeSubmitResult> {
  const res = await fetch("/api/practice/sessions/submit", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ topicSelectionId, targetLevel, answers }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "Practice submit failed");
  }
  return (await res.json()) as PracticeSubmitResult;
}

async function errorMessage(res: Response, fallback: string): Promise<string> {
  const body = await res.json().catch(() => null);
  return body?.message ?? fallback;
}

export async function fetchPracticeSessions(limit = 20): Promise<PracticeSessionSummary[]> {
  const res = await fetch(`/api/practice/sessions?limit=${limit}`, { cache: "no-store" });
  if (!res.ok) throw new Error(await errorMessage(res, "연습 기록을 불러오지 못했습니다."));
  return (await res.json()) as PracticeSessionSummary[];
}

/** Returns null when the session does not exist or belongs to someone else (404). */
export async function fetchPracticeSession(sessionId: string): Promise<PracticeSessionDetail | null> {
  const res = await fetch(`/api/practice/sessions/${encodeURIComponent(sessionId)}`, { cache: "no-store" });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(await errorMessage(res, "연습 기록을 불러오지 못했습니다."));
  return (await res.json()) as PracticeSessionDetail;
}

/** Free retry of a FAILED or PARTIAL evaluation; the server allows a few per session (409 after that). */
export async function retryPracticeEvaluation(sessionId: string): Promise<SessionEvaluationStatus> {
  const res = await fetch(`/api/practice/sessions/${encodeURIComponent(sessionId)}/evaluation/retry`, {
    method: "POST",
  });
  if (!res.ok) throw new Error(await errorMessage(res, "평가를 다시 요청하지 못했습니다."));
  const body = (await res.json()) as { evaluationStatus: SessionEvaluationStatus };
  return body.evaluationStatus;
}

export async function deletePracticeSession(sessionId: string): Promise<void> {
  const res = await fetch(`/api/practice/sessions/${encodeURIComponent(sessionId)}`, { method: "DELETE" });
  if (!res.ok) throw new Error(await errorMessage(res, "연습 기록 삭제에 실패했습니다."));
}
