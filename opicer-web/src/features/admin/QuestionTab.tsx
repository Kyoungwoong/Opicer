"use client";

import { useEffect, useState } from "react";
import { questionApi } from "./api";
import type { OpicLevel, Question, QuestionType } from "./types";
import { adminUi } from "@/features/admin/ui";

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
    <div className={adminUi.section}>
      {/* Form */}
      <form
        onSubmit={handleSubmit}
        className={adminUi.card}
      >
        <h3 className={adminUi.cardTitle}>
          {editId ? "질문 수정" : "질문 추가"}
        </h3>
        {error && (
          <p className={adminUi.error}>
            {error}
          </p>
        )}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              Topic
            </span>
            <input
              required
              value={form.topic}
              onChange={(e) => setForm((f) => ({ ...f, topic: e.target.value }))}
              className={adminUi.control}
              placeholder="예: 주거환경"
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              Type
            </span>
            <select
              value={form.type}
              onChange={(e) =>
                setForm((f) => ({ ...f, type: e.target.value as QuestionType }))
              }
              className={adminUi.control}
            >
              {QUESTION_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
        </div>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>
            Prompt Text
          </span>
          <textarea
            required
            rows={3}
            value={form.promptText}
            onChange={(e) =>
              setForm((f) => ({ ...f, promptText: e.target.value }))
            }
            className={adminUi.control}
            placeholder="질문 내용"
          />
        </label>
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              Structural Hint (선택)
            </span>
            <input
              value={form.structuralHint}
              onChange={(e) =>
                setForm((f) => ({ ...f, structuralHint: e.target.value }))
              }
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>
              Key Expressions (쉼표 구분)
            </span>
            <input
              value={form.keyExpressions}
              onChange={(e) =>
                setForm((f) => ({ ...f, keyExpressions: e.target.value }))
              }
              className={adminUi.control}
              placeholder="예: I usually, On weekends"
            />
          </label>
        </div>
        <div className={adminUi.fieldGroup}>
          <p className={adminUi.labelText}>
            Target Levels
          </p>
          <div className={adminUi.actions}>
            {OPIC_LEVELS.map((level) => (
              <label
                key={level}
                className={adminUi.checkInline}
              >
                <input
                  type="checkbox"
                  checked={form.targetLevels.includes(level)}
                  onChange={() => toggleLevel(level)}
                  className={adminUi.checkbox}
                />
                {level}
              </label>
            ))}
          </div>
        </div>
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
              <th className={adminUi.th}>Topic</th>
              <th className={adminUi.th}>Type</th>
              <th className={adminUi.th}>Levels</th>
              <th className={adminUi.th}>Active</th>
              <th className={adminUi.th}>Actions</th>
            </tr>
          </thead>
          <tbody className={adminUi.tbody}>
            {questions.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  className={adminUi.tdEmpty}
                >
                  질문이 없습니다.
                </td>
              </tr>
            )}
            {questions.map((q) => (
              <tr key={q.id} className={adminUi.tr}>
                <td className={adminUi.tdStrong}>{q.topic}</td>
                <td className={adminUi.tdMuted}>{q.type}</td>
                <td className={adminUi.tdMuted}>
                  {q.targetLevels.join(", ")}
                </td>
                <td className={adminUi.td}>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      q.active
                        ? "bg-success-subtle text-success"
                        : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {q.active ? "ON" : "OFF"}
                  </span>
                </td>
                <td className={adminUi.td}>
                  <div className={adminUi.actions}>
                    <button
                      onClick={() => startEdit(q)}
                      className={adminUi.linkButton}
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(q.id)}
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
