"use client";

import { useEffect, useState } from "react";
import { dailySentenceApi } from "./api";
import type { DailySentence, OpicLevel } from "./types";
import { adminUi } from "@/features/admin/ui";

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
    <div className={adminUi.section}>
      {/* Form */}
      <form
        onSubmit={handleSubmit}
        className={adminUi.card}
      >
        <h3 className={adminUi.cardTitle}>
          {editId ? "오늘의 문장 수정" : "오늘의 문장 추가"}
        </h3>
        {error && (
          <p className={adminUi.error}>
            {error}
          </p>
        )}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              날짜
            </span>
            <input
              required
              type="date"
              value={form.date}
              onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))}
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              Level
            </span>
            <select
              value={form.level}
              onChange={(e) =>
                setForm((f) => ({ ...f, level: e.target.value as OpicLevel }))
              }
              className={adminUi.control}
            >
              {OPIC_LEVELS.map((l) => (
                <option key={l} value={l}>
                  {l}
                </option>
              ))}
            </select>
          </label>
        </div>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>
            문장
          </span>
          <textarea
            required
            rows={3}
            value={form.text}
            onChange={(e) => setForm((f) => ({ ...f, text: e.target.value }))}
            className={adminUi.control}
            placeholder="오늘의 학습 문장"
          />
        </label>
        {editId && (
          <label className={adminUi.checkRow}>
            <input
              type="checkbox"
              checked={form.active}
              onChange={(e) =>
                setForm((f) => ({ ...f, active: e.target.checked }))
              }
              className={adminUi.checkbox}
            />
            <span className="text-muted-foreground">Active</span>
          </label>
        )}
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
              <th className={adminUi.th}>날짜</th>
              <th className={adminUi.th}>Level</th>
              <th className={adminUi.th}>문장</th>
              <th className={adminUi.th}>Active</th>
              <th className={adminUi.th}>Actions</th>
            </tr>
          </thead>
          <tbody className={adminUi.tbody}>
            {sentences.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  className={adminUi.tdEmpty}
                >
                  등록된 문장이 없습니다.
                </td>
              </tr>
            )}
            {sentences.map((s) => (
              <tr key={s.id} className={adminUi.tr}>
                <td className={adminUi.tdStrong}>{s.date}</td>
                <td className={adminUi.td}>
                  <span className={adminUi.pillPrimary}>
                    {s.level}
                  </span>
                </td>
                <td className="max-w-xs px-4 py-3 truncate text-muted-foreground">
                  {s.text}
                </td>
                <td className={adminUi.td}>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      s.active
                        ? "bg-success-subtle text-success"
                        : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {s.active ? "ON" : "OFF"}
                  </span>
                </td>
                <td className={adminUi.td}>
                  <div className={adminUi.actions}>
                    <button
                      onClick={() => startEdit(s)}
                      className={adminUi.linkButton}
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(s.id)}
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
