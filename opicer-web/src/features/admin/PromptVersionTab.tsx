"use client";

import { useEffect, useState } from "react";
import { promptApi } from "./api";
import type { PromptUseCase, PromptVersion } from "./types";
import { adminUi } from "@/features/admin/ui";

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
    <div className={adminUi.section}>
      <p role="note" className={adminUi.subCard}>
        자동 평가는 이 프롬프트 템플릿을 쓰지 않아요. 평가에 반영할 기준이나 문체는 &lsquo;평가 가이드&rsquo; 탭에서
        관리하세요.
      </p>

      {/* Form */}
      <form
        onSubmit={handleSubmit}
        className={adminUi.card}
      >
        <h3 className={adminUi.cardTitle}>
          {editId ? "프롬프트 수정" : "프롬프트 추가"}
        </h3>
        {error && (
          <p className={adminUi.error}>
            {error}
          </p>
        )}
        <div className={adminUi.grid3}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              Use Case
            </span>
            <select
              value={form.useCase}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  useCase: e.target.value as PromptUseCase,
                }))
              }
              className={adminUi.control}
            >
              {USE_CASES.map((uc) => (
                <option key={uc} value={uc}>
                  {uc}
                </option>
              ))}
            </select>
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              Version
            </span>
            <input
              required
              type="number"
              min={1}
              value={form.version}
              onChange={(e) =>
                setForm((f) => ({ ...f, version: Number(e.target.value) }))
              }
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              Name
            </span>
            <input
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className={adminUi.control}
              placeholder="v1-strict"
            />
          </label>
        </div>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>
            Template
          </span>
          <textarea
            required
            rows={6}
            value={form.template}
            onChange={(e) =>
              setForm((f) => ({ ...f, template: e.target.value }))
            }
            className={adminUi.controlMono}
            placeholder="프롬프트 템플릿 내용..."
          />
        </label>
        <div className={adminUi.actions}>
          <button
            type="submit"
            disabled={loading}
            className={adminUi.primaryButton}
          >
            {loading ? "저장 중..." : editId ? "수정" : "추가"}
          </button>
          {editId && (
            <button
              type="button"
              onClick={cancelEdit}
              className={adminUi.secondaryButton}
            >
              취소
            </button>
          )}
        </div>
      </form>

      {/* Table */}
      <div className={adminUi.tableWrap}>
        <table className={adminUi.table}>
          <thead className={adminUi.thead}>
            <tr>
              <th className={adminUi.th}>Use Case</th>
              <th className={adminUi.th}>v</th>
              <th className={adminUi.th}>Name</th>
              <th className={adminUi.th}>Active</th>
              <th className={adminUi.th}>Actions</th>
            </tr>
          </thead>
          <tbody className={adminUi.tbody}>
            {versions.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  className={adminUi.tdEmpty}
                >
                  등록된 프롬프트가 없습니다.
                </td>
              </tr>
            )}
            {versions.map((v) => (
              <tr key={v.id} className={adminUi.tr}>
                <td className={adminUi.tdStrong}>{v.useCase}</td>
                <td className={adminUi.tdMuted}>{v.version}</td>
                <td className={adminUi.tdMuted}>{v.name}</td>
                <td className={adminUi.td}>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      v.active
                        ? "bg-success-subtle text-success"
                        : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {v.active ? "ACTIVE" : "INACTIVE"}
                  </span>
                </td>
                <td className={adminUi.td}>
                  <div className={adminUi.actions}>
                    {!v.active && (
                      <button
                        onClick={() => handleActivate(v.id)}
                        className="font-medium text-success hover:underline"
                      >
                        활성화
                      </button>
                    )}
                    <button
                      onClick={() => startEdit(v)}
                      className={adminUi.linkButton}
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(v.id)}
                      className={adminUi.dangerLink}
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
