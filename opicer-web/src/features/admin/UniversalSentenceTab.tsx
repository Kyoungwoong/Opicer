"use client";

import { useEffect, useState } from "react";
import { universalSentenceApi } from "./api";
import type { UniversalSentence, UniversalSentenceType } from "./types";
import { adminUi } from "@/features/admin/ui";

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
    <div className={adminUi.section}>
      <form
        onSubmit={handleSubmit}
        className={adminUi.card}
      >
        <h3 className={adminUi.cardTitle}>
          {editId ? "만능 문장 수정" : "만능 문장 추가"}
        </h3>
        {error && (
          <p className={adminUi.error}>
            {error}
          </p>
        )}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              타입
            </span>
            <select
              value={form.type}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  type: e.target.value as UniversalSentenceType,
                }))
              }
              className={adminUi.control}
            >
              {SENTENCE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              제목
            </span>
            <input
              required
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              className={adminUi.control}
              placeholder="의견 시작"
            />
          </label>
        </div>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>
            문장
          </span>
          <textarea
            required
            rows={3}
            value={form.sentence}
            onChange={(e) =>
              setForm((f) => ({ ...f, sentence: e.target.value }))
            }
            className={adminUi.control}
            placeholder="From my perspective ..."
          />
        </label>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>
            태그 (쉼표로 구분)
          </span>
          <input
            value={form.tags}
            onChange={(e) => setForm((f) => ({ ...f, tags: e.target.value }))}
            className={adminUi.control}
            placeholder="opinion, starter"
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

      <div className={adminUi.tableWrap}>
        <table className={adminUi.table}>
          <thead className={adminUi.thead}>
            <tr>
              <th className={adminUi.th}>타입</th>
              <th className={adminUi.th}>제목</th>
              <th className={adminUi.th}>문장</th>
              <th className={adminUi.th}>태그</th>
              <th className={adminUi.th}>Active</th>
              <th className={adminUi.th}>Actions</th>
            </tr>
          </thead>
          <tbody className={adminUi.tbody}>
            {sentences.length === 0 && (
              <tr>
                <td
                  colSpan={6}
                  className={adminUi.tdEmpty}
                >
                  등록된 문장이 없습니다.
                </td>
              </tr>
            )}
            {sentences.map((sentence) => (
              <tr key={sentence.id}>
                <td className={adminUi.tdSmall}>
                  {sentence.type}
                </td>
                <td className={adminUi.tdStrong}>{sentence.title}</td>
                <td className={adminUi.tdSmall}>
                  {sentence.sentence}
                </td>
                <td className={adminUi.tdSmall}>
                  {sentence.tags.join(", ")}
                </td>
                <td className="px-4 py-3 text-xs">
                  {sentence.active ? "Y" : "N"}
                </td>
                <td className={adminUi.td}>
                  <div className={adminUi.actions}>
                    <button
                      onClick={() => startEdit(sentence)}
                      className={adminUi.smallButton}
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(sentence.id)}
                      className={adminUi.smallDangerButton}
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
