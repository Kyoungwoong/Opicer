"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { questionApi, topicApi } from "./api";
import { parseAdminError } from "./errors";
import type { AdminTopic, OpicLevel, Question, QuestionType } from "./types";
import { adminUi } from "@/features/admin/ui";
import {
  ADVANCED_PREFIX,
  QUESTION_TYPE_LABELS,
  ROLE_PLAY_PREFIX,
  TOPIC_CATEGORIES,
  setName,
  topicGroup,
} from "./content-rules";

const OPIC_LEVELS: OpicLevel[] = ["NL", "IL", "IM", "IH", "AL"];
const QUESTION_TYPES: QuestionType[] = [
  "DESCRIPTION",
  "PAST_EXPERIENCE",
  "OPINION",
  "COMPARE_CONTRAST",
  "ROLE_PLAY",
  "UNEXPECTED_SITUATION",
];

/** Select values that open a name field for a new mock-test set. */
const NEW_ROLE_PLAY = "__new_role_play";
const NEW_ADVANCED = "__new_advanced";
const ALL = "__all";
const UNLINKED = "__unlinked";

const emptyForm = {
  topicChoice: "",
  newSetName: "",
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
  const [topics, setTopics] = useState<AdminTopic[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [editingTopic, setEditingTopic] = useState<string | null>(null);
  const [filter, setFilter] = useState(ALL);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [loadedQuestions, loadedTopics] = await Promise.all([questionApi.list(), topicApi.list()]);
      setQuestions(loadedQuestions);
      setTopics(loadedTopics);
    } catch (err) {
      setError(parseAdminError(err).message);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const topicTitles = useMemo(() => new Set(topics.map((t) => t.title)), [topics]);
  const setNames = useMemo(() => {
    const names = (prefix: string) =>
      [...new Set(questions.filter((q) => q.topic.startsWith(prefix)).map((q) => q.topic))].sort();
    return { roleplay: names(ROLE_PLAY_PREFIX), advanced: names(ADVANCED_PREFIX) };
  }, [questions]);
  const unlinkedTopics = useMemo(
    () => [...new Set(questions.map((q) => q.topic).filter((t) => topicGroup(t, topicTitles) === "unlinked"))].sort(),
    [questions, topicTitles]
  );

  const chosenTopic =
    form.topicChoice === NEW_ROLE_PLAY
      ? `${ROLE_PLAY_PREFIX} ${form.newSetName.trim()}`
      : form.topicChoice === NEW_ADVANCED
        ? `${ADVANCED_PREFIX} ${form.newSetName.trim()}`
        : form.topicChoice;
  const chosenGroup = chosenTopic ? topicGroup(chosenTopic, topicTitles) : null;

  function startEdit(q: Question) {
    setEditId(q.id);
    setEditingTopic(q.topic);
    setError(null);
    setForm({
      topicChoice: q.topic,
      newSetName: "",
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
    setEditingTopic(null);
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
      topic: chosenTopic,
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
      setError(parseAdminError(err).message);
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
      setError(parseAdminError(err).message);
    }
  }

  const shown = questions.filter((q) =>
    filter === ALL ? true : filter === UNLINKED ? topicGroup(q.topic, topicTitles) === "unlinked" : q.topic === filter
  );
  // The topic a question being edited had, kept selectable even when it matches nothing.
  const orphanChoice = editingTopic && topicGroup(editingTopic, topicTitles) === "unlinked" ? editingTopic : null;

  return (
    <div className={adminUi.section}>
      {/* Form */}
      <form onSubmit={handleSubmit} className={adminUi.card}>
        <h3 className={adminUi.cardTitle}>{editId ? "질문 수정" : "질문 추가"}</h3>
        {error && <p className={adminUi.error}>{error}</p>}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>주제</span>
            <select
              required
              value={form.topicChoice}
              onChange={(e) => setForm((f) => ({ ...f, topicChoice: e.target.value, newSetName: "" }))}
              className={adminUi.control}
            >
              <option value="" disabled>
                주제를 고르세요
              </option>
              {TOPIC_CATEGORIES.map((category) => (
                <optgroup key={category} label={category}>
                  {topics
                    .filter((t) => t.category === category)
                    .sort((a, b) => a.topicOrder - b.topicOrder || a.title.localeCompare(b.title))
                    .map((t) => (
                      <option key={t.id} value={t.title}>
                        {t.title}
                        {t.active ? "" : " (비활성)"}
                      </option>
                    ))}
                </optgroup>
              ))}
              <optgroup label="롤플레이 세트 (실전 전용)">
                {setNames.roleplay.map((name) => (
                  <option key={name} value={name}>
                    {setName(name, ROLE_PLAY_PREFIX)}
                  </option>
                ))}
                <option value={NEW_ROLE_PLAY}>+ 새 롤플레이 세트…</option>
              </optgroup>
              <optgroup label="고난도 세트 (실전 전용)">
                {setNames.advanced.map((name) => (
                  <option key={name} value={name}>
                    {setName(name, ADVANCED_PREFIX)}
                  </option>
                ))}
                <option value={NEW_ADVANCED}>+ 새 고난도 세트…</option>
              </optgroup>
              {orphanChoice ? (
                <optgroup label="등록되지 않은 주제">
                  <option value={orphanChoice}>{orphanChoice} (연결 안 됨)</option>
                </optgroup>
              ) : null}
            </select>
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>유형</span>
            <select
              value={form.type}
              onChange={(e) => setForm((f) => ({ ...f, type: e.target.value as QuestionType }))}
              className={adminUi.control}
            >
              {QUESTION_TYPES.map((t) => (
                <option key={t} value={t}>
                  {QUESTION_TYPE_LABELS[t]} ({t})
                </option>
              ))}
            </select>
          </label>
        </div>
        {form.topicChoice === NEW_ROLE_PLAY || form.topicChoice === NEW_ADVANCED ? (
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>새 세트 이름</span>
            <input
              required
              maxLength={200}
              value={form.newSetName}
              onChange={(e) => setForm((f) => ({ ...f, newSetName: e.target.value }))}
              className={adminUi.control}
              placeholder={form.topicChoice === NEW_ROLE_PLAY ? "예: 카페 예약" : "예: 기술과 생활 변화"}
            />
            <span className="text-xs text-muted-foreground">저장되는 이름: {chosenTopic}</span>
          </label>
        ) : null}
        {chosenGroup === "roleplay" ? (
          <p className={adminUi.subCard}>
            롤플레이 세트는 3문항이 <strong>등록한 순서대로</strong> 나와요: ① 정보 묻기(롤플레이) → ② 문제 해결(롤플레이) →
            ③ 관련 경험(과거 경험). 세트마다 이 순서로 하나씩 추가하세요.
          </p>
        ) : chosenGroup === "advanced" ? (
          <p className={adminUi.subCard}>
            고난도 세트는 실전 마지막 두 문항(14·15번)으로 나와요. 한 세트에 ① 과거·현재 비교(비교 유형) → ② 관련 이슈나
            의견(의견 유형)을 이 순서로 등록해야 짝으로 나와요. IH·AL 같은 높은 목표 레벨을 주세요.
          </p>
        ) : chosenGroup === "unlinked" ? (
          <p className={adminUi.error}>
            이 주제는 등록돼 있지 않아 어디에도 출제되지 않아요. 주제 관리에서 주제를 만들거나 다른 주제를 고르세요.
          </p>
        ) : null}
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>질문 (영어)</span>
          <textarea
            required
            rows={3}
            value={form.promptText}
            onChange={(e) => setForm((f) => ({ ...f, promptText: e.target.value }))}
            className={adminUi.control}
            placeholder="질문 내용"
          />
        </label>
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>구조 힌트 (선택)</span>
            <input
              value={form.structuralHint}
              onChange={(e) => setForm((f) => ({ ...f, structuralHint: e.target.value }))}
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>핵심 표현 (쉼표 구분)</span>
            <input
              value={form.keyExpressions}
              onChange={(e) => setForm((f) => ({ ...f, keyExpressions: e.target.value }))}
              className={adminUi.control}
              placeholder="예: I usually, On weekends"
            />
          </label>
        </div>
        <div className={adminUi.fieldGroup}>
          <p className={adminUi.labelText}>목표 레벨</p>
          <div className={adminUi.actions}>
            {OPIC_LEVELS.map((level) => (
              <label key={level} className={adminUi.checkInline}>
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
              onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
              className={adminUi.checkbox}
            />
            <span className="text-muted-foreground">활성</span>
          </label>
        )}
        <div className={adminUi.actions}>
          <button type="submit" disabled={loading} className={adminUi.primaryButton}>
            {loading ? "저장 중..." : editId ? "수정" : "추가"}
          </button>
          {editId && (
            <button type="button" onClick={cancelEdit} className={adminUi.secondaryButton}>
              취소
            </button>
          )}
        </div>
      </form>

      {/* Filter */}
      <label className="flex flex-wrap items-center gap-2 text-sm">
        <span className={adminUi.labelText}>주제별 보기</span>
        <select value={filter} onChange={(e) => setFilter(e.target.value)} className={`${adminUi.control} max-w-xs`}>
          <option value={ALL}>전체 ({questions.length})</option>
          {topics.map((t) => (
            <option key={t.id} value={t.title}>
              {t.title}
            </option>
          ))}
          {[...setNames.roleplay, ...setNames.advanced].map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
          {unlinkedTopics.length > 0 ? <option value={UNLINKED}>연결 안 된 질문</option> : null}
        </select>
        {unlinkedTopics.length > 0 ? (
          <span className="text-xs text-destructive">연결 안 된 주제 {unlinkedTopics.length}개: {unlinkedTopics.join(", ")}</span>
        ) : null}
      </label>

      {/* Table */}
      <div className={adminUi.tableWrap}>
        <table className={adminUi.table}>
          <thead className={adminUi.thead}>
            <tr>
              <th className={adminUi.th}>주제</th>
              <th className={adminUi.th}>질문</th>
              <th className={adminUi.th}>유형</th>
              <th className={adminUi.th}>레벨</th>
              <th className={adminUi.th}>활성</th>
              <th className={adminUi.th}>관리</th>
            </tr>
          </thead>
          <tbody className={adminUi.tbody}>
            {shown.length === 0 && (
              <tr>
                <td colSpan={6} className={adminUi.tdEmpty}>
                  질문이 없습니다.
                </td>
              </tr>
            )}
            {shown.map((q) => (
              <tr key={q.id} className={adminUi.tr}>
                <td className={adminUi.tdStrong}>
                  {q.topic}
                  {topicGroup(q.topic, topicTitles) === "unlinked" ? (
                    <span className="ml-1.5 rounded-full bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive">
                      연결 안 됨
                    </span>
                  ) : null}
                </td>
                <td className={`${adminUi.tdMuted} max-w-sm truncate`} title={q.promptText}>
                  {q.promptText}
                </td>
                <td className={adminUi.tdMuted}>{QUESTION_TYPE_LABELS[q.type]}</td>
                <td className={adminUi.tdMuted}>{q.targetLevels.join(", ")}</td>
                <td className={adminUi.td}>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      q.active ? "bg-success-subtle text-success" : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {q.active ? "ON" : "OFF"}
                  </span>
                </td>
                <td className={adminUi.td}>
                  <div className={adminUi.actions}>
                    <button onClick={() => startEdit(q)} className={adminUi.linkButton}>
                      수정
                    </button>
                    <button onClick={() => handleDelete(q.id)} className={adminUi.dangerLink}>
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
