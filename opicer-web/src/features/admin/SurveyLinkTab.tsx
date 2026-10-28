"use client";

import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import { questionApi, surveyLinkApi, topicApi } from "./api";
import { parseAdminError } from "./errors";
import type { AdminTopic, Question, SurveyLinkItem, SurveyLinkPart } from "./types";
import { adminUi } from "@/features/admin/ui";

const SURVEY_CATEGORY = "설문조사";
/** A survey combo needs two unused questions of the topic (`MockTestComposer`). */
const COMBO_MIN = 2;
/** Occupation and student status are asked but not normally linked; they stay out of the counts. */
const UNPICKED_PARTS = new Set(["OCCUPATION", "STUDENT"]);

type Filter = "ALL" | "UNLINKED";
type LinkedTopic = SurveyLinkItem["topics"][number];

/** Survey items only link to 설문조사 topics; a link to any other category is a leftover the mock test ignores. */
const isSurveyTopic = (topic: LinkedTopic) => topic.category === SURVEY_CATEGORY;

/** The mock test only picks active 설문조사 topics, so only those make an item count as linked. */
const hasUsableTopic = (item: SurveyLinkItem) => item.topics.some((t) => t.active && isSurveyTopic(t));

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
  /** The item whose save is in flight; other rows cannot be opened until it settles. */
  const [savingId, setSavingId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  /** A failed save belongs to the item it was for, so it never shows in another row. */
  const [saveError, setSaveError] = useState<{ itemId: string; message: string } | null>(null);

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
      setLoadError(null);
    } catch (err) {
      setLoadError(parseAdminError(err).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  function retry() {
    setLoading(true);
    setLoadError(null);
    void load();
  }

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

  const pickingItems = parts.filter((p) => !UNPICKED_PARTS.has(p.code)).flatMap((p) => p.items);
  const linkedCount = pickingItems.filter(hasUsableTopic).length;

  /**
   * The draft keeps every linked 설문조사 topic, inactive ones too, so saving only drops the links that break the
   * rule. Filtering by active as well would silently unlink inactive topics.
   */
  function startEdit(item: SurveyLinkItem) {
    setEditing(item.id);
    setDraft(new Set(item.topics.filter(isSurveyTopic).map((t) => t.id)));
    setSaveError(null);
  }

  function cancelEdit() {
    setEditing(null);
    setSaveError(null);
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
    setSavingId(itemId);
    setSaveError(null);
    try {
      const updated = await surveyLinkApi.replace(itemId, [...draft]);
      setParts((current) =>
        current.map((part) => ({
          ...part,
          items: part.items.map((item) => (item.id === updated.id ? updated : item)),
        }))
      );
      setEditing((current) => (current === itemId ? null : current));
    } catch (err) {
      setSaveError({ itemId, message: parseAdminError(err).message });
    } finally {
      setSavingId(null);
    }
  }

  const editor = (item: SurveyLinkItem) => {
    const misfiled = item.topics.filter((t) => !isSurveyTopic(t));
    const saving = savingId === item.id;
    const error = saveError?.itemId === item.id ? saveError.message : null;
    return (
      <tr>
        <td colSpan={3} className="bg-muted/30 px-4 py-4">
          <fieldset className="flex flex-col gap-3">
            <legend className={`${adminUi.labelText} mb-2`}>&lsquo;{item.label}&rsquo;에 연결할 설문조사 주제</legend>
            {misfiled.length > 0 ? (
              <ul className="flex flex-col gap-1 rounded-lg bg-warning-subtle px-3 py-2 text-sm text-warning">
                {misfiled.map((topic) => (
                  <li key={topic.id}>
                    <span className="font-semibold">{topic.title}</span> ({topic.category}): 설문조사 주제가 아님 —
                    저장하면 연결이 풀려요
                  </li>
                ))}
              </ul>
            ) : null}
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
            {error ? (
              <p role="alert" className={adminUi.error}>
                {error}
              </p>
            ) : null}
            <div className={adminUi.actions}>
              <button type="button" disabled={saving} onClick={() => void save(item.id)} className={adminUi.smallButton}>
                {saving ? "저장 중…" : "저장"}
              </button>
              <button type="button" disabled={saving} onClick={cancelEdit} className={adminUi.smallButton}>
                취소
              </button>
            </div>
          </fieldset>
        </td>
      </tr>
    );
  };

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
            연결된 항목 {linkedCount} / 전체 {pickingItems.length} (Part 3·4)
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
        <p className="text-xs text-muted-foreground">
          활성 설문조사 주제가 하나 이상 연결된 항목만 연결된 것으로 세요.
        </p>
        {loading ? <p className="text-sm text-muted-foreground">불러오는 중…</p> : null}
        {loadError ? (
          <div className="flex flex-wrap items-center gap-2">
            <p role="alert" className={adminUi.error}>
              {loadError}
            </p>
            <button type="button" onClick={retry} className={adminUi.smallButton}>
              다시 시도
            </button>
          </div>
        ) : null}
      </div>

      {parts.map((part) => {
        const unpicked = UNPICKED_PARTS.has(part.code);
        const items = part.items.filter((item) => filter === "ALL" || (!unpicked && !hasUsableTopic(item)));
        if (items.length === 0) return null;
        return (
          <div key={part.code} className="flex flex-col gap-2">
            <h3 className="text-sm font-semibold text-foreground">
              {part.title}
              {unpicked ? (
                <span className="ml-2 text-xs font-normal text-muted-foreground">보통 주제를 연결하지 않아요</span>
              ) : null}
            </h3>
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
                          {item.topics.length === 0 && unpicked ? (
                            <span className="text-xs text-muted-foreground">없음</span>
                          ) : item.topics.length === 0 ? (
                            <span className="rounded-full bg-highlight/20 px-2 py-0.5 text-xs font-medium text-foreground">
                              연결 안 됨
                            </span>
                          ) : (
                            <span className="flex flex-wrap gap-1">
                              {item.topics.map((topic) => (
                                <TopicChip key={topic.id} topic={topic} />
                              ))}
                              {hasUsableTopic(item) ? null : (
                                <span className="rounded-full bg-highlight/20 px-2 py-0.5 text-xs font-medium text-foreground">
                                  출제 가능한 주제 없음
                                </span>
                              )}
                            </span>
                          )}
                        </td>
                        <td className={adminUi.td}>
                          <button
                            type="button"
                            disabled={savingId !== null}
                            title={savingId !== null ? "저장이 끝난 뒤에 수정할 수 있어요" : undefined}
                            onClick={() => startEdit(item)}
                            className={`${adminUi.linkButton} disabled:cursor-not-allowed disabled:opacity-50`}
                          >
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

/** A linked topic. One outside 설문조사, or an inactive one, is styled as not used by the mock test. */
function TopicChip({ topic }: { topic: LinkedTopic }) {
  if (!isSurveyTopic(topic)) {
    return (
      <span className={adminUi.pillWarn} title={`${topic.category} 주제예요. 설문조사 주제가 아니라 출제에 쓰지 않아요`}>
        {topic.title} (분류 불일치)
      </span>
    );
  }
  return (
    <span
      className={topic.active ? adminUi.pillPrimary : adminUi.pillOff}
      title={topic.active ? undefined : "비활성: 출제에 쓰지 않아요"}
    >
      {topic.title}
      {topic.active ? "" : " (비활성)"}
    </span>
  );
}
