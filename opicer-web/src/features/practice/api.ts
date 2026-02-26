import type {
  PracticeQuestion,
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

export async function analyzeAnswer(
  topicId: string,
  questionText: string,
  transcript: string
): Promise<string> {
  const res = await fetch("/api/practice/analyze", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ topicId, questionText, transcript }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "Analysis failed");
  }
  const body = (await res.json()) as { analysis: string };
  return body.analysis;
}

export async function improveScript(
  topicId: string,
  questionText: string,
  transcript: string
): Promise<string> {
  const res = await fetch("/api/practice/improve", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ topicId, questionText, transcript }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "Improvement failed");
  }
  const body = (await res.json()) as { improved: string };
  return body.improved;
}
