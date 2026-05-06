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

const CATEGORY_SELF_INTRO = "Self Introduction";
const CATEGORY_SURVEY = "Surveytext";
const CATEGORY_UNEXPECTED = "textQuestion";

const CATEGORY_META: Record<string, string> = {
  [CATEGORY_SELF_INTRO]: "text text text text Self Introduction text",
  [CATEGORY_SURVEY]: "Background Survey-based topics",
  [CATEGORY_UNEXPECTED]: "text/text Question text",
};

const LEGACY_UNEXPECTED = new Set(["Unexpected / Situational", CATEGORY_UNEXPECTED]);

const INTRO_KEYWORDS = ["Self Introduction", "self introduction"];

const SURVEY_GROUPS: Array<{ label: string; keywords: string[] }> = [
  { label: "Living", keywords: ["text", "text", "text", "text", "text", "text", "text"] },
  { label: "Work/Study", keywords: ["text", "text", "text", "text", "text", "text", "text", "text"] },
  { label: "Leisure/Hobby", keywords: ["text", "text", "tv", "text", "text", "text", "text", "text", "text"] },
  { label: "text/itemstext", keywords: ["text", "text", "text", "text", "text", "text", "text"] },
  { label: "Travel/Mobility", keywords: ["text", "text", "text", "text", "text", "text"] },
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
  return matched?.label ?? "Others";
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
        setError(err.message ?? "Failed to load topics.");
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
        description: CATEGORY_META[category] ?? "Topictext text",
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
      setError(err?.message ?? "Select Saving...text text.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleStartPractice = () => {
    if (!selectedId) return;
    router.push(ROUTES.practiceSession(selectedId));
  };

  // text Ready screen text
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
                {selectedTopic?.title ?? ""} Start practice for this topic
              </button>
              <button
                type="button"
                onClick={() => { setMode("select"); setError(null); }}
                className="text-sm text-[var(--muted)] hover:underline"
              >
                text Topic text Selecttext
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // text Select screen text
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
                Start topic practice?
              </h1>
              <p className="mt-2 text-sm text-[var(--muted)]">
                text Topictext Selecttext text Topictext text text text text.
              </p>
            </div>
            <div className="flex w-full max-w-sm items-center gap-2 rounded-full border border-black/10 bg-white px-4 py-2 shadow-sm">
              <span className="text-sm text-[var(--muted)]">ðtext</span>
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Topic text (text/English)"
                className="w-full bg-transparent text-sm outline-none placeholder:text-[var(--muted)]"
              />
            </div>
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-[0.65fr_1.35fr]">
          <aside className="flex flex-col gap-4">
            <div className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[var(--muted)]">
                Category
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
                Selected topic
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
                <p className="mt-3 text-sm text-[var(--muted)]">text Selected topictext text.</p>
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
                {isSubmitting ? "Saving..." : "Confirm selection"}
              </button>
            </div>
          </aside>

          <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Topics</p>
                <h2 className="mt-1 text-2xl font-semibold">
                  {activeCategory?.label ?? "Topic list"}
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
                  <p className="text-sm text-[var(--muted)]">textitemstext text Survey text text.</p>
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
                                {isSelected ? "Selected" : "Select"}
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
                          {isSelected ? "Selected" : "Select"}
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
