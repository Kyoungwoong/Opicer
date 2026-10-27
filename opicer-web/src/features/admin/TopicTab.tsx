"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { questionApi, topicApi } from "./api";
import { parseAdminError } from "./errors";
import type { AdminTopic, AdminTopicInput, Question, QuestionType } from "./types";
import { adminUi } from "@/features/admin/ui";
import {
  ADVANCED_PREFIX,
  QUESTION_TYPE_LABELS,
  ROLE_PLAY_PREFIX,
  TOPIC_CATEGORIES,
  hasAdvancedPair,
  readiness,
  setName,
  typeCounts,
} from "./content-rules";

const emptyForm = {
  title: "",
  englishTitle: "",
  category: "설문조사" as string,
  categoryOrder: 1,
  topicOrder: 1,
  badges: "",
  active: true,
};

const TONE: Record<"ok" | "warn" | "off", string> = {
  ok: "bg-success-subtle text-success",
  warn: "bg-highlight/20 text-foreground",
  off: "bg-muted text-muted-foreground",
};

function CountChips({ counts }: { counts: Partial<Record<QuestionType, number>> }) {
  const entries = Object.entries(counts) as [QuestionType, number][];
  if (entries.length === 0) return <span className="text-xs text-muted-foreground">활성 문항 없음</span>;
  return (
    <span className="flex flex-wrap gap-1">
      {entries.map(([type, count]) => (
        <span key={type} className="rounded-full border px-2 py-0.5 text-xs">
          {QUESTION_TYPE_LABELS[type]} {count}
        </span>
      ))}
    </span>
  );
}

/**
 * Practice topics: questions belong to a topic by its title, and a survey or unexpected topic also feeds the mock
 * test. Renaming carries the questions along; a topic in use can only be deactivated.
 */
export function TopicTab() {
  const [topics, setTopics] = useState<AdminTopic[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState<AdminTopic | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [loadedTopics, loadedQuestions] = await Promise.all([topicApi.list(), questionApi.list()]);
      setTopics(loadedTopics);
      setQuestions(loadedQuestions);
    } catch (err) {
      setError(parseAdminError(err).message);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const grouped = useMemo(
    () =>
      TOPIC_CATEGORIES.map((category) => ({
        category,
        topics: topics
          .filter((t) => t.category === category)
          .sort((a, b) => a.topicOrder - b.topicOrder || a.title.localeCompare(b.title)),
      })),
    [topics]
  );
  // Topics saved before the categories were fixed; editing one asks for a valid category.
  const otherCategory = topics.filter((t) => !(TOPIC_CATEGORIES as readonly string[]).includes(t.category));

  const sets = useMemo(() => {
    const names = (prefix: string) =>
      [...new Set(questions.filter((q) => q.topic.startsWith(prefix)).map((q) => q.topic))].sort();
    return { roleplay: names(ROLE_PLAY_PREFIX), advanced: names(ADVANCED_PREFIX) };
  }, [questions]);

  function startEdit(topic: AdminTopic) {
    setEditing(topic);
    setError(null);
    setForm({
      title: topic.title,
      englishTitle: topic.englishTitle,
      category: (TOPIC_CATEGORIES as readonly string[]).includes(topic.category) ? topic.category : "설문조사",
      categoryOrder: topic.categoryOrder,
      topicOrder: topic.topicOrder,
      badges: topic.badges.map((b) => b.label).join(", "),
      active: topic.active,
    });
  }

  function cancelEdit() {
    setEditing(null);
    setForm(emptyForm);
    setError(null);
  }

  function input(overrides: Partial<AdminTopicInput> = {}, base: AdminTopic | null = editing): AdminTopicInput {
    const labels = form.badges.split(",").map((s) => s.trim()).filter(Boolean);
    return {
      title: form.title,
      englishTitle: form.englishTitle,
      category: form.category,
      categoryOrder: form.categoryOrder,
      topicOrder: form.topicOrder,
      // Keep counts of badges that are kept by label.
      badges: labels.map((label) => ({ label, count: base?.badges.find((b) => b.label === label)?.count ?? null })),
      active: form.active,
      ...overrides,
    };
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (editing && editing.title !== form.title.trim()) {
      const count = questions.filter((q) => q.topic === editing.title).length;
      if (count > 0 && !confirm(`이 주제의 문항 ${count}개도 새 이름으로 함께 옮겨요. 계속할까요?`)) return;
    }
    setSaving(true);
    setError(null);
    try {
      if (editing) await topicApi.update(editing.id, input());
      else await topicApi.create(input({}, null));
      cancelEdit();
      await load();
    } catch (err) {
      setError(parseAdminError(err).message);
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(topic: AdminTopic) {
    setError(null);
    try {
      await topicApi.update(topic.id, {
        title: topic.title,
        englishTitle: topic.englishTitle,
        category: topic.category,
        categoryOrder: topic.categoryOrder,
        topicOrder: topic.topicOrder,
        badges: topic.badges,
        active: !topic.active,
      });
      await load();
    } catch (err) {
      setError(parseAdminError(err).message);
    }
  }

  async function handleDelete(topic: AdminTopic) {
    if (!confirm(`'${topic.title}' 주제를 삭제할까요? 되돌릴 수 없어요.`)) return;
    setError(null);
    try {
      await topicApi.delete(topic.id);
      await load();
    } catch (err) {
      setError(parseAdminError(err).message);
    }
  }

  const renderRow = (topic: AdminTopic) => {
    const counts = typeCounts(questions, topic.title);
    const state = readiness(topic, counts);
    return (
      <tr key={topic.id} className={adminUi.tr}>
        <td className={adminUi.tdStrong}>
          {topic.title}
          {topic.mockLinked ? (
            <span className="ml-1.5 rounded-full bg-accent px-2 py-0.5 text-xs font-medium text-accent-foreground">실전 연결</span>
          ) : null}
          <span className="block text-xs font-normal text-muted-foreground">{topic.englishTitle}</span>
        </td>
        <td className={adminUi.td}>
          <CountChips counts={counts} />
        </td>
        <td className={adminUi.td}>
          <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${TONE[state.tone]}`}>{state.text}</span>
        </td>
        <td className={adminUi.td}>
          <div className={adminUi.actions}>
            <button type="button" onClick={() => startEdit(topic)} className={adminUi.linkButton}>
              수정
            </button>
            <button type="button" onClick={() => void toggleActive(topic)} className={adminUi.linkButton}>
              {topic.active ? "비활성화" : "활성화"}
            </button>
            <button type="button" onClick={() => void handleDelete(topic)} className={adminUi.dangerLink}>
              삭제
            </button>
          </div>
        </td>
      </tr>
    );
  };

  return (
    <div className={adminUi.section}>
      <form onSubmit={handleSubmit} className={adminUi.card}>
        <h3 className={adminUi.cardTitle}>{editing ? "주제 수정" : "주제 추가"}</h3>
        <p className="text-sm text-muted-foreground">
          문항은 주제 이름으로 연결돼요. 이름을 바꾸면 문항도 함께 옮겨지고, 문항이나 연습 기록이 있는 주제는 삭제 대신
          비활성화해요. 설문조사·돌발질문 주제는 실전연습에도 쓰여요. 실전 Background Survey 항목과의 연결은 아직
          코드에서 관리해요.
        </p>
        {error && <p className={adminUi.error}>{error}</p>}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>주제 이름</span>
            <input
              required
              maxLength={255}
              value={form.title}
              disabled={editing?.titleLocked}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              className={adminUi.control}
              placeholder="예: 박물관 관람"
            />
            {editing?.titleLocked ? (
              <span className="text-xs text-muted-foreground">
                실전연습 첫 문항을 이 이름으로 찾기 때문에 이름은 바꿀 수 없어요.
              </span>
            ) : null}
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>영문 이름</span>
            <input
              required
              maxLength={255}
              value={form.englishTitle}
              onChange={(e) => setForm((f) => ({ ...f, englishTitle: e.target.value }))}
              className={adminUi.control}
              placeholder="예: Museums"
            />
          </label>
        </div>
        <div className={adminUi.grid3}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>분류</span>
            <select
              value={form.category}
              onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}
              className={adminUi.control}
            >
              {TOPIC_CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>분류 순서</span>
            <input
              type="number"
              min={0}
              required
              value={form.categoryOrder}
              onChange={(e) => setForm((f) => ({ ...f, categoryOrder: Number(e.target.value) }))}
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>주제 순서</span>
            <input
              type="number"
              min={0}
              required
              value={form.topicOrder}
              onChange={(e) => setForm((f) => ({ ...f, topicOrder: Number(e.target.value) }))}
              className={adminUi.control}
            />
          </label>
        </div>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>배지 (쉼표 구분, 선택)</span>
          <input
            value={form.badges}
            onChange={(e) => setForm((f) => ({ ...f, badges: e.target.value }))}
            className={adminUi.control}
            placeholder="예: 인기, 신규"
          />
        </label>
        <label className={adminUi.checkRow}>
          <input
            type="checkbox"
            checked={form.active}
            onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
            className={adminUi.checkbox}
          />
          <span className="text-muted-foreground">활성 (연습 목록과 실전에 나와요)</span>
        </label>
        <div className={adminUi.actions}>
          <button type="submit" disabled={saving} className={adminUi.primaryButton}>
            {saving ? "저장 중..." : editing ? "수정" : "추가"}
          </button>
          {editing && (
            <button type="button" onClick={cancelEdit} className={adminUi.secondaryButton}>
              취소
            </button>
          )}
        </div>
      </form>

      {[...grouped, ...(otherCategory.length > 0 ? [{ category: "기타(분류 다시 지정 필요)", topics: otherCategory }] : [])].map(
        (group) => (
          <section key={group.category} className="flex flex-col gap-2">
            <h3 className={adminUi.cardTitle}>
              {group.category} <span className="text-sm font-normal text-muted-foreground">{group.topics.length}개</span>
            </h3>
            <div className={adminUi.tableWrap}>
              <table className={adminUi.table}>
                <thead className={adminUi.thead}>
                  <tr>
                    <th className={adminUi.th}>주제</th>
                    <th className={adminUi.th}>활성 문항</th>
                    <th className={adminUi.th}>출제 상태</th>
                    <th className={adminUi.th}>관리</th>
                  </tr>
                </thead>
                <tbody className={adminUi.tbody}>
                  {group.topics.length === 0 ? (
                    <tr>
                      <td colSpan={4} className={adminUi.tdEmpty}>
                        주제가 없어요.
                      </td>
                    </tr>
                  ) : (
                    group.topics.map(renderRow)
                  )}
                </tbody>
              </table>
            </div>
          </section>
        )
      )}

      <section className={adminUi.card}>
        <h3 className={adminUi.cardTitle}>실전 전용 세트</h3>
        <p className="text-sm text-muted-foreground">
          주제가 아니라 질문 관리에서 이름으로 묶어요. 롤플레이 세트는 3문항이 등록한 순서대로 나와요. 고난도 세트는
          비교 문항 바로 다음에 의견 문항을 등록해야 14·15번 짝으로 나와요.
        </p>
        <div className={adminUi.grid2}>
          {(
            [
              ["롤플레이", ROLE_PLAY_PREFIX, sets.roleplay],
              ["고난도", ADVANCED_PREFIX, sets.advanced],
            ] as const
          ).map(([label, prefix, names]) => (
            <div key={label} className={adminUi.subCard}>
              <p className="text-sm font-semibold">
                {label} {names.length}세트
              </p>
              {names.length === 0 ? (
                <p className="text-sm text-muted-foreground">아직 없어요.</p>
              ) : (
                <ul className="flex flex-col gap-1.5 text-sm">
                  {names.map((name) => {
                    const counts = typeCounts(questions, name);
                    const total = Object.values(counts).reduce((s, n) => s + (n ?? 0), 0);
                    const problem =
                      prefix === ROLE_PLAY_PREFIX
                        ? total < 3 ? "3문항 필요" : null
                        : hasAdvancedPair(questions, name) ? null : "비교 → 의견 짝 필요";
                    return (
                      <li key={name} className="flex flex-wrap items-center justify-between gap-2">
                        <span>{setName(name, prefix)}</span>
                        <span className={problem ? "text-xs text-destructive" : "text-xs text-muted-foreground"}>
                          활성 {total}문항{problem ? ` · ${problem}` : ""}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
