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
