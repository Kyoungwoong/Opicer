"use client";

import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import { questionApi, surveyLinkApi, topicApi } from "./api";
import { parseAdminError } from "./errors";
import type { AdminTopic, Question, SurveyLinkItem, SurveyLinkPart } from "./types";
import { adminUi } from "@/features/admin/ui";

const SURVEY_CATEGORY = "설문조사";
/** A survey combo needs two unused questions of the topic (`MockTestComposer`). */
const COMBO_MIN = 2;

type Filter = "ALL" | "UNLINKED";

/**
 * Which practice topics each Background Survey item points to. The mock test picks its part-1 combos from the
 * topics the learner's choices link to; the chosen residence's topics are the fallback when those run short.
 */
export function SurveyLinkTab() {
  const [parts, setParts] = useState<SurveyLinkPart[]>([]);
  const [topics, setTopics] = useState<AdminTopic[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [filter, setFilter] = useState<Filter>("ALL");
  const [editing, setEditing] = useState<string | null>(null);
  const [draft, setDraft] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [loadedParts, loadedTopics, loadedQuestions] = await Promise.all([
        surveyLinkApi.list(),
        topicApi.list(),
        questionApi.list(),
      ]);
      setParts(loadedParts);
      setTopics(loadedTopics);
      setQuestions(loadedQuestions);
    } catch (err) {
      setError(parseAdminError(err).message);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const surveyTopics = useMemo(
    () =>
      topics
        .filter((t) => t.category === SURVEY_CATEGORY)
        .sort((a, b) => a.topicOrder - b.topicOrder || a.title.localeCompare(b.title)),
    [topics]
  );

  const activeCounts = useMemo(() => {
    const counts = new Map<string, number>();
    for (const q of questions) if (q.active) counts.set(q.topic, (counts.get(q.topic) ?? 0) + 1);
    return counts;
  }, [questions]);

  const allItems = parts.flatMap((p) => p.items);
  const linkedCount = allItems.filter((item) => item.topics.length > 0).length;

  function startEdit(item: SurveyLinkItem) {
    setEditing(item.id);
    setDraft(new Set(item.topics.map((t) => t.id)));
    setError(null);
  }

  function toggle(topicId: string) {
    setDraft((current) => {
      const next = new Set(current);
      if (next.has(topicId)) next.delete(topicId);
      else next.add(topicId);
      return next;
    });
  }

  async function save(itemId: string) {
    setSaving(true);
    setError(null);
    try {
      const updated = await surveyLinkApi.replace(itemId, [...draft]);
      setParts((current) =>
        current.map((part) => ({
          ...part,
          items: part.items.map((item) => (item.id === updated.id ? updated : item)),
        }))
      );
      setEditing(null);
    } catch (err) {
      setError(parseAdminError(err).message);
    } finally {
      setSaving(false);
    }
  }

  const editor = (item: SurveyLinkItem) => (
    <tr>
      <td colSpan={3} className="bg-muted/30 px-4 py-4">
        <fieldset className="flex flex-col gap-3">
          <legend className={`${adminUi.labelText} mb-2`}>&lsquo;{item.label}&rsquo;에 연결할 설문조사 주제</legend>
          {surveyTopics.length === 0 ? (
            <p className="text-sm text-muted-foreground">설문조사 분류의 주제가 없어요. 주제 관리 탭에서 먼저 만들어 주세요.</p>
          ) : (
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {surveyTopics.map((topic) => {
                const count = activeCounts.get(topic.title) ?? 0;
                return (
                  <label key={topic.id} className={adminUi.checkRow}>
                    <input
                      type="checkbox"
                      className={adminUi.checkbox}
                      checked={draft.has(topic.id)}
                      onChange={() => toggle(topic.id)}
                    />
                    <span className={topic.active ? "" : "text-muted-foreground"}>
                      {topic.title}
                      <span className="ml-1 text-xs text-muted-foreground">
                        문항 {count}
                        {topic.active ? "" : " · 비활성"}
                      </span>
                      {topic.active && count < COMBO_MIN ? (
                        <span className="ml-1 text-xs text-destructive">실전 콤보에 부족</span>
                      ) : null}
                    </span>
                  </label>
                );
              })}
            </div>
          )}
          <div className={adminUi.actions}>
            <button type="button" disabled={saving} onClick={() => void save(item.id)} className={adminUi.smallButton}>
              {saving ? "저장 중…" : "저장"}
            </button>
            <button type="button" onClick={() => setEditing(null)} className={adminUi.smallButton}>
              취소
            </button>
          </div>
        </fieldset>
      </td>
    </tr>
  );

  return (
    <section className={adminUi.section}>
      <div className={adminUi.card}>
        <h2 className={adminUi.cardTitle}>설문 연결</h2>
        <p className="text-sm text-muted-foreground">
          실전연습 설문에서 고른 항목에 연결된 주제로 1부 콤보 문제를 내요. Part 3(거주) 항목에 연결된 주제는 고른
          주제의 문항이 부족할 때 대신 쓰여요. 설문조사 분류의 주제만 연결할 수 있고, 비활성 주제는 출제에서 빠져요.
        </p>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm font-medium text-foreground">
            연결된 항목 {linkedCount} / 전체 {allItems.length}
          </p>
          <label className="flex items-center gap-2 text-sm">
            <span className="text-muted-foreground">보기</span>
            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value as Filter)}
              className={`${adminUi.control} w-auto`}
            >
              <option value="ALL">전체</option>
              <option value="UNLINKED">연결 안 됨</option>
            </select>
          </label>
        </div>
        {error ? <p className={adminUi.error}>{error}</p> : null}
      </div>

      {parts.map((part) => {
        const items = part.items.filter((item) => filter === "ALL" || item.topics.length === 0);
        if (items.length === 0) return null;
        return (
          <div key={part.code} className="flex flex-col gap-2">
            <h3 className="text-sm font-semibold text-foreground">{part.title}</h3>
            <div className={adminUi.tableWrap}>
              <table className={adminUi.table}>
                <thead className={adminUi.thead}>
                  <tr>
                    <th className={adminUi.th}>설문 항목</th>
                    <th className={adminUi.th}>연결된 주제</th>
                    <th className={adminUi.th}>작업</th>
                  </tr>
                </thead>
                <tbody className={adminUi.tbody}>
                  {items.map((item) => (
                    <Fragment key={item.id}>
                      <tr className={adminUi.tr}>
                        <td className={adminUi.tdStrong}>
                          {item.label}
                          <span className="block text-xs font-normal text-muted-foreground">{item.id}</span>
                        </td>
                        <td className={adminUi.td}>
                          {item.topics.length === 0 ? (
                            <span className="rounded-full bg-highlight/20 px-2 py-0.5 text-xs font-medium text-foreground">
                              연결 안 됨
                            </span>
                          ) : (
                            <span className="flex flex-wrap gap-1">
                              {item.topics.map((topic) => (
                                <span
                                  key={topic.id}
                                  className={topic.active ? adminUi.pillPrimary : adminUi.pillOff}
                                  title={topic.active ? undefined : "비활성: 출제에 쓰지 않아요"}
                                >
                                  {topic.title}
                                  {topic.active ? "" : " (비활성)"}
                                </span>
                              ))}
                            </span>
                          )}
                        </td>
                        <td className={adminUi.td}>
                          <button type="button" onClick={() => startEdit(item)} className={adminUi.linkButton}>
                            수정
                          </button>
                        </td>
                      </tr>
                      {editing === item.id ? editor(item) : null}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        );
      })}
    </section>
  );
}
