"use client";

import { useEffect, useState } from "react";
import { dailySentenceApi } from "./api";
import type { DailySentence, OpicLevel } from "./types";

const OPIC_LEVELS: OpicLevel[] = ["NL", "IL", "IM", "IH", "AL"];

const emptyForm = {
  date: "",
  text: "",
  level: "IL" as OpicLevel,
  audioUrl: "",
  active: true,
};

export function DailySentenceTab() {
  const [sentences, setSentences] = useState<DailySentence[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    load();
  }, []);

  async function load() {
    try {
      const data = await dailySentenceApi.list();
      setSentences(data.sort((a, b) => b.date.localeCompare(a.date)));
    } catch {
      setError("Failed to load daily sentences.");
    }
  }

  function startEdit(s: DailySentence) {
    setEditId(s.id);
    setForm({
      date: s.date,
      text: s.text,
      level: s.level,
      audioUrl: s.audioUrl ?? "",
      active: s.active,
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
    const payload = {
      date: form.date,
      text: form.text,
      level: form.level,
      audioUrl: form.audioUrl || undefined,
      active: form.active,
    };
    try {
      if (editId) {
        await dailySentenceApi.update(editId, payload);
      } else {
        await dailySentenceApi.create(payload);
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
      await dailySentenceApi.delete(id);
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
          {editId ? "오늘의 문장 수정" : "오늘의 문장 추가"}
        </h3>
        {error && (
          <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
            {error}
          </p>
        )}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              날짜
            </label>
            <input
              required
              type="date"
              value={form.date}
              onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))}
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              Level
            </label>
            <select
              value={form.level}
              onChange={(e) =>
                setForm((f) => ({ ...f, level: e.target.value as OpicLevel }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            >
              {OPIC_LEVELS.map((l) => (
                <option key={l} value={l}>
                  {l}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-[var(--muted)]">
            문장
          </label>
          <textarea
            required
            rows={3}
            value={form.text}
            onChange={(e) => setForm((f) => ({ ...f, text: e.target.value }))}
            className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            placeholder="오늘의 학습 문장"
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

      {/* Table */}
      <div className="overflow-x-auto rounded-xl border border-black/8">
        <table className="w-full text-sm">
          <thead className="bg-[var(--card)] text-[var(--muted)]">
            <tr>
              <th className="px-4 py-3 text-left font-medium">날짜</th>
              <th className="px-4 py-3 text-left font-medium">Level</th>
              <th className="px-4 py-3 text-left font-medium">문장</th>
              <th className="px-4 py-3 text-left font-medium">Active</th>
              <th className="px-4 py-3 text-left font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-black/5 bg-white">
            {sentences.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  className="px-4 py-6 text-center text-[var(--muted)]"
                >
                  등록된 문장이 없습니다.
                </td>
              </tr>
            )}
            {sentences.map((s) => (
              <tr key={s.id} className="hover:bg-[var(--background)]">
                <td className="px-4 py-3 font-medium">{s.date}</td>
                <td className="px-4 py-3">
                  <span className="rounded-full bg-[var(--accent)]/10 px-2 py-0.5 text-xs font-medium text-[var(--accent-strong)]">
                    {s.level}
                  </span>
                </td>
                <td className="max-w-xs px-4 py-3 truncate text-[var(--muted)]">
                  {s.text}
                </td>
                <td className="px-4 py-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      s.active
                        ? "bg-green-100 text-green-700"
                        : "bg-gray-100 text-gray-500"
                    }`}
                  >
                    {s.active ? "ON" : "OFF"}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button
                      onClick={() => startEdit(s)}
                      className="text-[var(--accent)] hover:underline"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(s.id)}
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
