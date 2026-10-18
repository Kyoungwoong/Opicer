"use client";

import { useEffect, useState } from "react";
import { promptApi } from "./api";
import type { PromptUseCase, PromptVersion } from "./types";

const USE_CASES: PromptUseCase[] = [
  "TRANSCRIPT_CLEANING",
  "FEEDBACK",
  "SCRIPT_IMPROVEMENT",
];

const emptyForm = {
  useCase: "FEEDBACK" as PromptUseCase,
  version: 1,
  name: "",
  template: "",
};

export function PromptVersionTab() {
  const [versions, setVersions] = useState<PromptVersion[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    load();
  }, []);

  async function load() {
    try {
      const data = await promptApi.list();
      setVersions(data);
    } catch {
      setError("Failed to load prompt versions.");
    }
  }

  function startEdit(v: PromptVersion) {
    setEditId(v.id);
    setForm({
      useCase: v.useCase,
      version: v.version,
      name: v.name,
      template: v.template,
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
    try {
      if (editId) {
        await promptApi.update(editId, form);
      } else {
        await promptApi.create(form);
      }
      cancelEdit();
      await load();
    } catch (err) {
      setError(String(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleActivate(id: string) {
    try {
      await promptApi.activate(id);
      await load();
    } catch (err) {
      setError(String(err));
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
      await promptApi.delete(id);
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
          {editId ? "프롬프트 수정" : "프롬프트 추가"}
        </h3>
        {error && (
          <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
            {error}
          </p>
        )}
        <div className="grid grid-cols-3 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              Use Case
            </label>
            <select
              value={form.useCase}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  useCase: e.target.value as PromptUseCase,
                }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            >
              {USE_CASES.map((uc) => (
                <option key={uc} value={uc}>
                  {uc}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              Version
            </label>
            <input
              required
              type="number"
              min={1}
              value={form.version}
              onChange={(e) =>
                setForm((f) => ({ ...f, version: Number(e.target.value) }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">
              Name
            </label>
            <input
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
              placeholder="v1-strict"
            />
          </div>
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-[var(--muted)]">
            Template
          </label>
          <textarea
            required
            rows={6}
            value={form.template}
            onChange={(e) =>
              setForm((f) => ({ ...f, template: e.target.value }))
            }
            className="w-full rounded-lg border border-black/10 px-3 py-2 font-mono text-xs focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            placeholder="프롬프트 템플릿 내용..."
          />
        </div>
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
              <th className="px-4 py-3 text-left font-medium">Use Case</th>
              <th className="px-4 py-3 text-left font-medium">v</th>
              <th className="px-4 py-3 text-left font-medium">Name</th>
              <th className="px-4 py-3 text-left font-medium">Active</th>
              <th className="px-4 py-3 text-left font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-black/5 bg-white">
            {versions.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  className="px-4 py-6 text-center text-[var(--muted)]"
                >
                  등록된 프롬프트가 없습니다.
                </td>
              </tr>
            )}
            {versions.map((v) => (
              <tr key={v.id} className="hover:bg-[var(--background)]">
                <td className="px-4 py-3 font-medium">{v.useCase}</td>
                <td className="px-4 py-3 text-[var(--muted)]">{v.version}</td>
                <td className="px-4 py-3 text-[var(--muted)]">{v.name}</td>
                <td className="px-4 py-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      v.active
                        ? "bg-green-100 text-green-700"
                        : "bg-gray-100 text-gray-500"
                    }`}
                  >
                    {v.active ? "ACTIVE" : "INACTIVE"}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    {!v.active && (
                      <button
                        onClick={() => handleActivate(v.id)}
                        className="text-green-600 hover:underline"
                      >
                        활성화
                      </button>
                    )}
                    <button
                      onClick={() => startEdit(v)}
                      className="text-[var(--accent)] hover:underline"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(v.id)}
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
