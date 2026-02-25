import type {
  DailySentence,
  OpicLevel,
  PromptUseCase,
  PromptVersion,
  Question,
  QuestionType,
} from "./types";

const BASE = "/api/admin";

type ApiResponse<T> = {
  status: string;
  message: string;
  data: T;
};

async function request<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  const res = await fetch(`${BASE}/${path}`, {
    cache: "no-store",
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`${res.status}: ${body}`);
  }
  const text = await res.text();
  if (!text) return undefined as T;
  const body = JSON.parse(text) as ApiResponse<T>;
  return body.data;
}

// ── Questions ─────────────────────────────────────────────

export const questionApi = {
  list: () => request<Question[]>("questions"),
  create: (data: {
    topic: string;
    type: QuestionType;
    promptText: string;
    promptAudioUrl?: string;
    structuralHint?: string;
    targetLevels: OpicLevel[];
    keyExpressions: string[];
  }) =>
    request<Question>("questions", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  update: (
    id: string,
    data: {
      topic: string;
      type: QuestionType;
      promptText: string;
      promptAudioUrl?: string;
      structuralHint?: string;
      targetLevels: OpicLevel[];
      keyExpressions: string[];
      active: boolean;
    }
  ) =>
    request<Question>(`questions/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (id: string) =>
    request<void>(`questions/${id}`, { method: "DELETE" }),
};

// ── Daily Sentences ────────────────────────────────────────

export const dailySentenceApi = {
  list: () => request<DailySentence[]>("daily-sentences"),
  create: (data: {
    date: string;
    text: string;
    level: OpicLevel;
    audioUrl?: string;
  }) =>
    request<DailySentence>("daily-sentences", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  update: (
    id: string,
    data: {
      date: string;
      text: string;
      level: OpicLevel;
      audioUrl?: string;
      active: boolean;
    }
  ) =>
    request<DailySentence>(`daily-sentences/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (id: string) =>
    request<void>(`daily-sentences/${id}`, { method: "DELETE" }),
};

// ── Prompt Versions ────────────────────────────────────────

export const promptApi = {
  list: () => request<PromptVersion[]>("prompts"),
  create: (data: {
    useCase: PromptUseCase;
    version: number;
    name: string;
    template: string;
  }) =>
    request<PromptVersion>("prompts", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  update: (
    id: string,
    data: {
      useCase: PromptUseCase;
      version: number;
      name: string;
      template: string;
    }
  ) =>
    request<PromptVersion>(`prompts/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  activate: (id: string) =>
    request<PromptVersion>(`prompts/${id}/activate`, { method: "POST" }),
  delete: (id: string) =>
    request<void>(`prompts/${id}`, { method: "DELETE" }),
};
