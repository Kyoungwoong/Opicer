import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { TopNav } from "@/components/common/TopNav";
import { fetchTopics, submitTopicSelection } from "@/features/practice/api";
import { ROUTES } from "@/lib/routes";
import type { TopicCategory, TopicItem } from "@/features/practice/types";

type Props = {
  userLabel?: string;
  onLogout?: () => void;
};

const CATEGORY_SELF_INTRO = "자기소개";
const CATEGORY_SURVEY = "설문조사";
const CATEGORY_UNEXPECTED = "돌발질문";

const CATEGORY_META: Record<string, string> = {
  [CATEGORY_SELF_INTRO]: "시험 시작 전 필수 자기소개 대비",
  [CATEGORY_SURVEY]: "Background Survey 기반 주제",
  [CATEGORY_UNEXPECTED]: "돌발/상황형 질문 대비",
};

const LEGACY_UNEXPECTED = new Set(["돌발/상황 대처", CATEGORY_UNEXPECTED]);

const INTRO_KEYWORDS = ["자기소개", "self introduction"];

const SURVEY_GROUPS: Array<{ label: string; keywords: string[] }> = [
  { label: "거주/생활", keywords: ["집", "주거", "동네", "이웃", "기숙사", "룸메이트", "가족"] },
  { label: "직업/학업", keywords: ["직업", "회사", "사업", "재택", "학생", "수업", "학위", "교사"] },
  { label: "여가/취미", keywords: ["음악", "영화", "tv", "독서", "쇼핑", "요리", "공연", "콘서트", "캠핑"] },
  { label: "운동/건강", keywords: ["운동", "헬스", "수영", "자전거", "조깅", "걷기", "요가"] },
  { label: "여행/이동", keywords: ["여행", "출장", "휴가", "해외", "국내", "드라이브"] },
];

function resolvePrimaryCategory(topic: TopicItem): string {
  const title = topic.title.toLowerCase();
  if (INTRO_KEYWORDS.some((keyword) => title.includes(keyword))) return CATEGORY_SELF_INTRO;
  if (LEGACY_UNEXPECTED.has(topic.category)) return CATEGORY_UNEXPECTED;
  return CATEGORY_SURVEY;
}

function resolveSurveyGroup(topic: TopicItem): string {
  const title = topic.title.toLowerCase();
  const englishTitle = topic.englishTitle.toLowerCase();
  const fromBadge = topic.badges?.[0]?.label?.trim();
  if (fromBadge && fromBadge.length > 0) return fromBadge;
  const matched = SURVEY_GROUPS.find((group) =>
    group.keywords.some(
      (keyword) => title.includes(keyword.toLowerCase()) || englishTitle.includes(keyword.toLowerCase())
    )
  );
  return matched?.label ?? "기타";
}

export function TopicPracticeView({ userLabel, onLogout }: Props) {
  const router = useRouter();

  const [topics, setTopics] = useState<TopicItem[]>([]);
  const [activeCategoryId, setActiveCategoryId] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mode, setMode] = useState<"select" | "ready">("select");

  useEffect(() => {
    let mounted = true;
    setIsLoading(true);
    fetchTopics()
      .then((data) => {
        if (!mounted) return;
        setTopics(data);
        const primarySet = new Set(data.map(resolvePrimaryCategory));
        const firstCategory =
          [CATEGORY_SELF_INTRO, CATEGORY_SURVEY, CATEGORY_UNEXPECTED].find((category) =>
            primarySet.has(category)
          ) ?? CATEGORY_SURVEY;
        setActiveCategoryId(firstCategory);
      })
      .catch((err) => {
        if (!mounted) return;
        setError(err.message ?? "주제를 불러오는 중 문제가 발생했습니다.");
      })
      .finally(() => {
        if (mounted) setIsLoading(false);
      });
    return () => { mounted = false; };
  }, []);

  const categories = useMemo<TopicCategory[]>(() => {
    const grouped = new Map<string, TopicItem[]>();
    topics.forEach((topic) => {
      const category = resolvePrimaryCategory(topic);
      if (!grouped.has(category)) grouped.set(category, []);
      grouped.get(category)?.push(topic);
    });
    return [CATEGORY_SELF_INTRO, CATEGORY_SURVEY, CATEGORY_UNEXPECTED]
      .filter((category) => grouped.has(category))
      .map((category, index) => ({
        id: category,
        label: category,
        description: CATEGORY_META[category] ?? "주제별 연습",
        topics: (grouped.get(category) ?? []).sort((a, b) => a.topicOrder - b.topicOrder),
        order: index,
      }))
      .map(({ order, ...rest }) => rest);
  }, [topics]);

  const activeCategory = useMemo<TopicCategory | undefined>(
    () => categories.find((c) => c.id === activeCategoryId),
    [categories, activeCategoryId]
  );

  const filteredTopics = useMemo(() => {
    const base = activeCategory?.topics ?? [];
    const trimmed = query.trim().toLowerCase();
    if (!trimmed) return base;
    return base.filter(
      (topic) =>
        topic.title.toLowerCase().includes(trimmed) ||
        topic.englishTitle.toLowerCase().includes(trimmed)
    );
  }, [activeCategory, query]);

  const groupedSurveyTopics = useMemo(() => {
    if (activeCategoryId !== CATEGORY_SURVEY) return [];
    const grouped = new Map<string, TopicItem[]>();
    filteredTopics.forEach((topic) => {
      const group = resolveSurveyGroup(topic);
      if (!grouped.has(group)) grouped.set(group, []);
      grouped.get(group)?.push(topic);
    });
    return Array.from(grouped.entries())
      .map(([group, items]) => ({ group, items }))
      .sort((a, b) => a.group.localeCompare(b.group, "ko"));
  }, [activeCategoryId, filteredTopics]);

  const selectedTopic = useMemo<TopicItem | undefined>(
    () => topics.find((t) => t.id === selectedId),
    [topics, selectedId]
  );

  const handleSelect = (topic: TopicItem) => {
    setError(null);
    setSelectedId((prev) => (prev === topic.id ? null : topic.id));
  };

  const handleSubmit = async () => {
    if (!selectedId) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await submitTopicSelection(selectedId);
      setMode("ready");
    } catch (err: any) {
      setError(err?.message ?? "선택 저장 중 문제가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleStartPractice = () => {
    if (!selectedId) return;
    router.push(ROUTES.practiceSession(selectedId));
  };

  // ── Ready screen ───────────────────────────────────────────
  if (mode === "ready") {
    return (
      <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
        <div className="mx-auto flex max-w-4xl flex-col gap-8">
          <TopNav userLabel={userLabel} onLogout={onLogout} />

          <div className="flex flex-col items-center justify-center gap-10 py-16">
            <div className="text-center space-y-3">
              <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">
                Topic Practice
              </p>
              <h1 className="text-4xl font-semibold tracking-tight">
                {selectedTopic?.title ?? ""}
              </h1>
              {selectedTopic?.englishTitle ? (
                <p className="text-base text-[var(--muted)]">
                  {selectedTopic.englishTitle}
                </p>
              ) : null}
              {selectedTopic?.badges && selectedTopic.badges.length > 0 ? (
                <div className="flex justify-center gap-2 pt-1">
                  {selectedTopic.badges.map((badge) => (
                    <span
                      key={badge.label}
                      className="rounded-full border border-[var(--accent)]/30 px-3 py-1 text-xs font-semibold text-[var(--accent-strong)]"
                    >
                      {badge.label}
                      {badge.count != null ? ` ${badge.count}` : ""}
                    </span>
                  ))}
                </div>
              ) : null}
            </div>

            <div className="flex flex-col items-center gap-4">
              {error ? <p className="text-sm text-red-600">{error}</p> : null}
              <button
                type="button"
                onClick={handleStartPractice}
                className="rounded-full bg-[var(--accent)] px-10 py-4 text-base font-semibold text-white shadow-lg shadow-[var(--accent)]/20 transition hover:bg-[var(--accent-strong)]"
              >
                {selectedTopic?.title ?? ""}에 맞는 연습 시작하기
              </button>
              <button
                type="button"
                onClick={() => { setMode("select"); setError(null); }}
                className="text-sm text-[var(--muted)] hover:underline"
              >
                ← 주제 다시 선택하기
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ── Select screen ──────────────────────────────────────────
  return (
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <TopNav
          maxWidthClassName="max-w-6xl"
          userLabel={userLabel}
          onLogout={onLogout}
        />

        <section className="rounded-[32px] border border-black/5 bg-white/70 p-8 shadow-sm">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">
                Topic Practice
              </p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight">
                주제별 연습을 시작해볼까요?
              </h1>
              <p className="mt-2 text-sm text-[var(--muted)]">
                하나의 주제를 선택하면 그 주제에 맞는 연습 흐름을 준비합니다.
              </p>
            </div>
            <div className="flex w-full max-w-sm items-center gap-2 rounded-full border border-black/10 bg-white px-4 py-2 shadow-sm">
              <span className="text-sm text-[var(--muted)]">🔎</span>
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="주제 검색 (한국어/English)"
                className="w-full bg-transparent text-sm outline-none placeholder:text-[var(--muted)]"
              />
            </div>
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-[0.65fr_1.35fr]">
          <aside className="flex flex-col gap-4">
            <div className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[var(--muted)]">
                카테고리
              </p>
              <div className="mt-4 flex flex-col gap-2">
                {categories.map((category) => {
                  const isActive = category.id === activeCategoryId;
                  return (
                    <button
                      key={category.id}
                      type="button"
                      onClick={() => {
                        setActiveCategoryId(category.id);
                        setQuery("");
                      }}
                      className={`rounded-2xl border px-4 py-3 text-left transition ${
                        isActive
                          ? "border-transparent bg-[var(--accent)] text-white shadow-sm"
                          : "border-black/10 bg-white text-[var(--ink)] hover:border-[var(--accent)]/40"
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-semibold">{category.label}</span>
                        <span className={`text-xs ${isActive ? "text-white/80" : "text-[var(--muted)]"}`}>
                          {category.topics.length} items
                        </span>
                      </div>
                      <p className={`mt-1 text-xs ${isActive ? "text-white/80" : "text-[var(--muted)]"}`}>
                        {category.description}
                      </p>
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[var(--muted)]">
                선택된 주제
              </p>
              {selectedTopic ? (
                <div className="mt-4 rounded-2xl border border-[var(--accent)]/30 bg-[var(--accent)]/10 p-4">
                  <p className="text-sm font-semibold">{selectedTopic.title}</p>
                  <p className="text-xs text-[var(--muted)]">{selectedTopic.englishTitle}</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {selectedTopic.badges?.map((badge) => (
                      <span
                        key={`${selectedTopic.id}-${badge.label}`}
                        className="rounded-full border border-[var(--accent)]/30 px-2.5 py-1 text-[10px] font-semibold text-[var(--accent-strong)]"
                      >
                        {badge.label}{badge.count != null ? ` ${badge.count}` : ""}
                      </span>
                    ))}
                  </div>
                </div>
              ) : (
                <p className="mt-3 text-sm text-[var(--muted)]">아직 선택된 주제가 없습니다.</p>
              )}
              {error ? <p className="mt-3 text-xs text-red-600">{error}</p> : null}
              <button
                type="button"
                disabled={!selectedId || isSubmitting}
                onClick={handleSubmit}
                className={`mt-4 w-full rounded-full px-4 py-2 text-sm font-semibold transition ${
                  selectedTopic && !isSubmitting
                    ? "bg-[var(--accent)] text-white hover:bg-[var(--accent-strong)]"
                    : "cursor-not-allowed bg-black/10 text-[var(--muted)]"
                }`}
              >
                {isSubmitting ? "저장 중..." : "선택 완료"}
              </button>
            </div>
          </aside>

          <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Topics</p>
                <h2 className="mt-1 text-2xl font-semibold">
                  {activeCategory?.label ?? "주제 목록"}
                </h2>
              </div>
              <span className="text-xs text-[var(--muted)]">{filteredTopics.length} topics</span>
            </div>

            {isLoading ? (
              <div className="mt-6 grid gap-4 sm:grid-cols-2">
                {Array.from({ length: 6 }).map((_, i) => (
                  <div key={`skeleton-${i}`} className="h-24 rounded-3xl border border-black/10 bg-white/60 animate-pulse" />
                ))}
              </div>
            ) : activeCategoryId === CATEGORY_SURVEY ? (
              <div className="mt-6 space-y-5">
                {groupedSurveyTopics.length === 0 ? (
                  <p className="text-sm text-[var(--muted)]">조건에 맞는 설문 항목이 없습니다.</p>
                ) : null}
                {groupedSurveyTopics.map(({ group, items }) => (
                  <div key={group} className="space-y-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--muted)]">
                      {group}
                    </p>
                    <div className="grid gap-4 sm:grid-cols-2">
                      {items.map((topic) => {
                        const isSelected = selectedId === topic.id;
                        return (
                          <button
                            key={topic.id}
                            type="button"
                            onClick={() => handleSelect(topic)}
                            className={`rounded-3xl border p-4 text-left transition ${
                              isSelected
                                ? "border-transparent bg-[var(--accent)] text-white shadow-lg shadow-[var(--accent)]/20"
                                : "border-black/10 bg-white hover:border-[var(--accent)]/40"
                            }`}
                          >
                            <div className="flex items-start justify-between gap-3">
                              <div>
                                <p className="text-base font-semibold">{topic.title}</p>
                                <p className={`mt-1 text-xs ${isSelected ? "text-white/80" : "text-[var(--muted)]"}`}>
                                  {topic.englishTitle}
                                </p>
                              </div>
                              <span className={`rounded-full px-2.5 py-1 text-[10px] font-semibold ${
                                isSelected ? "bg-white/20 text-white" : "bg-[var(--accent)]/10 text-[var(--accent-strong)]"
                              }`}>
                                {isSelected ? "선택됨" : "선택"}
                              </span>
                            </div>
                            {topic.badges && topic.badges.length > 0 ? (
                              <div className="mt-3 flex flex-wrap gap-2">
                                {topic.badges.map((badge) => (
                                  <span
                                    key={`${topic.id}-${badge.label}`}
                                    className={`rounded-full border px-2.5 py-1 text-[10px] font-semibold ${
                                      isSelected
                                        ? "border-white/30 text-white/90"
                                        : "border-[var(--accent)]/20 text-[var(--accent-strong)]"
                                    }`}
                                  >
                                    {badge.label}{badge.count != null ? ` ${badge.count}` : ""}
                                  </span>
                                ))}
                              </div>
                            ) : null}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="mt-6 grid gap-4 sm:grid-cols-2">
                {filteredTopics.map((topic) => {
                  const isSelected = selectedId === topic.id;
                  return (
                    <button
                      key={topic.id}
                      type="button"
                      onClick={() => handleSelect(topic)}
                      className={`rounded-3xl border p-4 text-left transition ${
                        isSelected
                          ? "border-transparent bg-[var(--accent)] text-white shadow-lg shadow-[var(--accent)]/20"
                          : "border-black/10 bg-white hover:border-[var(--accent)]/40"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="text-base font-semibold">{topic.title}</p>
                          <p className={`mt-1 text-xs ${isSelected ? "text-white/80" : "text-[var(--muted)]"}`}>
                            {topic.englishTitle}
                          </p>
                        </div>
                        <span className={`rounded-full px-2.5 py-1 text-[10px] font-semibold ${
                          isSelected ? "bg-white/20 text-white" : "bg-[var(--accent)]/10 text-[var(--accent-strong)]"
                        }`}>
                          {isSelected ? "선택됨" : "선택"}
                        </span>
                      </div>
                      {topic.badges && topic.badges.length > 0 ? (
                        <div className="mt-3 flex flex-wrap gap-2">
                          {topic.badges.map((badge) => (
                            <span
                              key={`${topic.id}-${badge.label}`}
                              className={`rounded-full border px-2.5 py-1 text-[10px] font-semibold ${
                                isSelected
                                  ? "border-white/30 text-white/90"
                                  : "border-[var(--accent)]/20 text-[var(--accent-strong)]"
                              }`}
                            >
                              {badge.label}{badge.count != null ? ` ${badge.count}` : ""}
                            </span>
                          ))}
                        </div>
                      ) : null}
                    </button>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
