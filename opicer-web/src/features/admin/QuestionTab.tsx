"use client";

import { useEffect, useState } from "react";
import { questionApi } from "./api";
import type { OpicLevel, Question, QuestionType } from "./types";

const OPIC_LEVELS: OpicLevel[] = ["NL", "IL", "IM", "IH", "AL"];
const QUESTION_TYPES: QuestionType[] = [
  "DESCRIPTION",
  "PAST_EXPERIENCE",
  "OPINION",
  "COMPARE_CONTRAST",
  "ROLE_PLAY",
  "UNEXPECTED_SITUATION",
];

const emptyForm = {
  topic: "",
  type: "DESCRIPTION" as QuestionType,
  promptText: "",
  promptAudioUrl: "",
  structuralHint: "",
  targetLevels: [] as OpicLevel[],
  keyExpressions: "",
  active: true,
};

export function QuestionTab() {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    load();
  }, []);

  async function load() {
    try {
      const data = await questionApi.list();
      setQuestions(data);
    } catch {
      setError("Failed to load questions.");
    }
  }

  function startEdit(q: Question) {
    setEditId(q.id);
    setForm({
      topic: q.topic,
      type: q.type,
      promptText: q.promptText,
      promptAudioUrl: q.promptAudioUrl ?? "",
      structuralHint: q.structuralHint ?? "",
      targetLevels: q.targetLevels,
      keyExpressions: q.keyExpressions.join(", "),
      active: q.active,
    });
  }

  function cancelEdit() {
    setEditId(null);
    setForm(emptyForm);
    setError(null);
  }

  function toggleLevel(level: OpicLevel) {
    setForm((f) => ({
      ...f,
      targetLevels: f.targetLevels.includes(level)
        ? f.targetLevels.filter((l) => l !== level)
        : [...f.targetLevels, level],
    }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    const payload = {
      topic: form.topic,
      type: form.type,
      promptText: form.promptText,
      promptAudioUrl: form.promptAudioUrl || undefined,
      structuralHint: form.structuralHint || undefined,
      targetLevels: form.targetLevels,
      keyExpressions: form.keyExpressions
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean),
      active: form.active,
    };
    try {
      if (editId) {
        await questionApi.update(editId, payload);
      } else {
        await questionApi.create(payload);
      }
      cancelEdit();
      await load();
    } catch (err) {
      setError(String(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
      await questionApi.delete(id);
      await load();
    } catch (err) {
      setError(String(err));
    }
  }

  return (
    <div className="space-y-6">
      {/* Form */}
      <form
        onSubmit={handleSubmit}
        className="rounded-xl border border-black/8 bg-[var(--card)] p-5 space-y-4"
      >
        <h3 className="font-semibold text-[var(--ink)]">
          {editId ? "질문 수정" : "질문 추가"}
        </h3>
        {error && (
          <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
            {error}
          </p>
        )}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              Topic
            </label>
            <input
              required
              value={form.topic}
              onChange={(e) => setForm((f) => ({ ...f, topic: e.target.value }))}
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
              placeholder="예: 주거환경"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              Type
            </label>
            <select
              value={form.type}
              onChange={(e) =>
                setForm((f) => ({ ...f, type: e.target.value as QuestionType }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            >
              {QUESTION_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-[var(--muted)]">
            Prompt Text
          </label>
          <textarea
            required
            rows={3}
            value={form.promptText}
            onChange={(e) =>
              setForm((f) => ({ ...f, promptText: e.target.value }))
            }
            className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            placeholder="질문 내용"
          />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              Structural Hint (선택)
            </label>
            <input
              value={form.structuralHint}
              onChange={(e) =>
                setForm((f) => ({ ...f, structuralHint: e.target.value }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              Key Expressions (쉼표 구분)
            </label>
            <input
              value={form.keyExpressions}
              onChange={(e) =>
                setForm((f) => ({ ...f, keyExpressions: e.target.value }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
              placeholder="예: I usually, On weekends"
            />
          </div>
        </div>
        <div className="space-y-2">
          <label className="text-xs font-medium text-[var(--muted)]">
            Target Levels
          </label>
          <div className="flex gap-2">
            {OPIC_LEVELS.map((level) => (
              <label
                key={level}
                className="flex items-center gap-1.5 text-sm cursor-pointer"
              >
                <input
                  type="checkbox"
                  checked={form.targetLevels.includes(level)}
                  onChange={() => toggleLevel(level)}
                  className="accent-[var(--accent)]"
                />
                {level}
              </label>
            ))}
          </div>
        </div>
        {editId && (
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(e) =>
                setForm((f) => ({ ...f, active: e.target.checked }))
              }
              className="accent-[var(--accent)]"
            />
            <span className="text-[var(--muted)]">Active</span>
          </label>
        )}
        <div className="flex gap-2">
          <button
            type="submit"
            disabled={loading}
            className="rounded-full bg-[var(--accent)] px-5 py-2 text-sm font-semibold text-white transition hover:bg-[var(--accent-strong)] disabled:opacity-50"
          >
            {loading ? "저장 중..." : editId ? "수정" : "추가"}
          </button>
          {editId && (
            <button
              type="button"
              onClick={cancelEdit}
              className="rounded-full border border-black/10 px-5 py-2 text-sm font-semibold text-[var(--muted)] transition hover:bg-black/5"
            >
              취소
            </button>
          )}
        </div>
      </form>

      {/* Table */}
      <div className="overflow-x-auto rounded-xl border border-black/8">
        <table className="w-full text-sm">
          <thead className="bg-[var(--card)] text-[var(--muted)]">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Topic</th>
              <th className="px-4 py-3 text-left font-medium">Type</th>
              <th className="px-4 py-3 text-left font-medium">Levels</th>
              <th className="px-4 py-3 text-left font-medium">Active</th>
              <th className="px-4 py-3 text-left font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-black/5 bg-white">
            {questions.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  className="px-4 py-6 text-center text-[var(--muted)]"
                >
                  질문이 없습니다.
                </td>
              </tr>
            )}
            {questions.map((q) => (
              <tr key={q.id} className="hover:bg-[var(--background)]">
                <td className="px-4 py-3 font-medium">{q.topic}</td>
                <td className="px-4 py-3 text-[var(--muted)]">{q.type}</td>
                <td className="px-4 py-3 text-[var(--muted)]">
                  {q.targetLevels.join(", ")}
                </td>
                <td className="px-4 py-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      q.active
                        ? "bg-green-100 text-green-700"
                        : "bg-gray-100 text-gray-500"
                    }`}
                  >
                    {q.active ? "ON" : "OFF"}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button
                      onClick={() => startEdit(q)}
                      className="text-[var(--accent)] hover:underline"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(q.id)}
                      className="text-red-500 hover:underline"
                    >
                      삭제
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
