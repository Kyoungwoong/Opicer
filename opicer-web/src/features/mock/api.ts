import type {
  DifficultyAdjustment,
  MockAnswerInput,
  MockItem,
  MockSubmitResult,
  StartMockTestInput,
  StartedMockTest,
  SurveyCatalog,
} from "@/features/mock/types";

/** A failed call with the server's error `code` (for example `CREDIT_INSUFFICIENT_BALANCE`) when it sent one. */
export class MockApiError extends Error {
  constructor(message: string, readonly code: string | null, readonly status: number) {
    super(message);
  }
}

async function failure(res: Response, fallback: string): Promise<MockApiError> {
  const body = (await res.json().catch(() => null)) as { code?: string; message?: string } | null;
  return new MockApiError(body?.message ?? fallback, body?.code ?? null, res.status);
}

async function postJson<T>(path: string, payload: unknown, fallback: string): Promise<T> {
  const res = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw await failure(res, fallback);
  return (await res.json()) as T;
}

export async function fetchSurveyCatalog(): Promise<SurveyCatalog> {
  const res = await fetch("/api/mock-tests/survey", { cache: "no-store" });
  if (!res.ok) throw await failure(res, "설문 항목을 불러오지 못했어요.");
  return (await res.json()) as SurveyCatalog;
}

/** Creates the test and returns its first part. Credits are only checked here; they are charged on submit. */
export function startMockTest(input: StartMockTestInput): Promise<StartedMockTest> {
  return postJson("/api/mock-tests", input, "실전연습을 시작하지 못했어요.");
}

/** Re-selects the difficulty once and returns the second part. */
export function adjustMockTest(id: string, difficulty: DifficultyAdjustment): Promise<MockItem[]> {
  return postJson(`/api/mock-tests/${encodeURIComponent(id)}/adjust`, { difficulty }, "다음 문항을 불러오지 못했어요.");
}

/** Charges the test once (a repeated call is not charged again) and queues its evaluation. */
export function submitMockTest(id: string, answers: MockAnswerInput[], examSeconds: number): Promise<MockSubmitResult> {
  return postJson(`/api/mock-tests/${encodeURIComponent(id)}/submit`, { answers, examSeconds }, "제출하지 못했어요.");
}
