"use client";

import { useEffect, useState } from "react";
import { universalSentenceApi } from "./api";
import type { UniversalSentence, UniversalSentenceType } from "./types";

const SENTENCE_TYPES: UniversalSentenceType[] = [
  "OPINION",
  "PAST_EXPERIENCE",
  "COMPARE_CONTRAST",
  "UNEXPECTED_SITUATION",
];

const emptyForm = {
  type: "OPINION" as UniversalSentenceType,
  title: "",
  sentence: "",
  tags: "",
  active: true,
};

export function UniversalSentenceTab() {
  const [sentences, setSentences] = useState<UniversalSentence[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    load();
  }, []);

  async function load() {
    try {
      const data = await universalSentenceApi.list();
      setSentences(
        data.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      );
    } catch {
      setError("Failed to load universal sentences.");
    }
  }

  function startEdit(sentence: UniversalSentence) {
    setEditId(sentence.id);
    setForm({
      type: sentence.type,
      title: sentence.title,
      sentence: sentence.sentence,
      tags: sentence.tags.join(", "),
      active: sentence.active,
    });
  }

  function cancelEdit() {
    setEditId(null);
    setForm(emptyForm);
    setError(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    const tags = form.tags
      .split(",")
      .map((t) => t.trim())
      .filter(Boolean);
    const payload = {
      type: form.type,
      title: form.title.trim(),
      sentence: form.sentence.trim(),
      tags,
      active: form.active,
    };
    try {
      if (editId) {
        await universalSentenceApi.update(editId, payload);
      } else {
        await universalSentenceApi.create(payload);
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
      await universalSentenceApi.delete(id);
      await load();
    } catch (err) {
      setError(String(err));
    }
  }

  return (
    <div className="space-y-6">
      <form
        onSubmit={handleSubmit}
        className="rounded-xl border border-black/8 bg-[var(--card)] p-5 space-y-4"
      >
        <h3 className="font-semibold text-[var(--ink)]">
          {editId ? "만능 문장 수정" : "만능 문장 추가"}
        </h3>
        {error && (
          <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
            {error}
          </p>
        )}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              타입
            </label>
            <select
              value={form.type}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  type: e.target.value as UniversalSentenceType,
                }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            >
              {SENTENCE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              제목
            </label>
            <input
              required
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
              placeholder="의견 시작"
            />
          </div>
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-[var(--muted)]">
            문장
          </label>
          <textarea
            required
            rows={3}
            value={form.sentence}
            onChange={(e) =>
              setForm((f) => ({ ...f, sentence: e.target.value }))
            }
            className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            placeholder="From my perspective ..."
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-[var(--muted)]">
            태그 (쉼표로 구분)
          </label>
          <input
            value={form.tags}
            onChange={(e) => setForm((f) => ({ ...f, tags: e.target.value }))}
            className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            placeholder="opinion, starter"
          />
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

      <div className="overflow-x-auto rounded-xl border border-black/8">
        <table className="w-full text-sm">
          <thead className="bg-[var(--card)] text-[var(--muted)]">
            <tr>
              <th className="px-4 py-3 text-left font-medium">타입</th>
              <th className="px-4 py-3 text-left font-medium">제목</th>
              <th className="px-4 py-3 text-left font-medium">문장</th>
              <th className="px-4 py-3 text-left font-medium">태그</th>
              <th className="px-4 py-3 text-left font-medium">Active</th>
              <th className="px-4 py-3 text-left font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-black/5 bg-white">
            {sentences.length === 0 && (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-6 text-center text-[var(--muted)]"
                >
                  등록된 문장이 없습니다.
                </td>
              </tr>
            )}
            {sentences.map((sentence) => (
              <tr key={sentence.id}>
                <td className="px-4 py-3 text-xs text-[var(--muted)]">
                  {sentence.type}
                </td>
                <td className="px-4 py-3 font-medium">{sentence.title}</td>
                <td className="px-4 py-3 text-xs text-[var(--muted)]">
                  {sentence.sentence}
                </td>
                <td className="px-4 py-3 text-xs text-[var(--muted)]">
                  {sentence.tags.join(", ")}
                </td>
                <td className="px-4 py-3 text-xs">
                  {sentence.active ? "Y" : "N"}
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button
                      onClick={() => startEdit(sentence)}
                      className="rounded-full border border-black/10 px-3 py-1 text-xs text-[var(--muted)] transition hover:bg-black/5"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(sentence.id)}
                      className="rounded-full border border-red-200 px-3 py-1 text-xs text-red-500 transition hover:bg-red-50"
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
