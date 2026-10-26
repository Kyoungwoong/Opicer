import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertCircle, ArrowLeft, LoaderCircle, Mic, Search } from "lucide-react";
import { cn } from "@/lib/utils";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Eyebrow, SectionHeader } from "@/components/common/SectionHeader";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { TopicCard } from "@/features/practice/components/TopicCard";
import { fetchTopics, submitTopicSelection } from "@/features/practice/api";
import { ROUTES } from "@/lib/routes";
import type { TopicCategory, TopicItem } from "@/features/practice/types";
import { Mascot } from "@/components/brand/Mascot";
import { DEFAULT_TARGET_LEVEL, TARGET_LEVELS, isTargetLevel } from "@/features/practice/evaluation-labels";
import type { TargetLevel } from "@/features/practice/types";

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

const TARGET_LEVEL_STORAGE_KEY = "opicer.practice.targetLevel";

function readStoredTargetLevel(): TargetLevel {
  try {
    const stored = window.localStorage.getItem(TARGET_LEVEL_STORAGE_KEY);
    return isTargetLevel(stored) ? stored : DEFAULT_TARGET_LEVEL;
  } catch {
    return DEFAULT_TARGET_LEVEL;
  }
}

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
  const [selectedSelectionId, setSelectedSelectionId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mode, setMode] = useState<"select" | "ready">("select");
  const [targetLevel, setTargetLevel] = useState<TargetLevel>(DEFAULT_TARGET_LEVEL);

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
      .map((category) => ({
        id: category,
        label: category,
        description: CATEGORY_META[category] ?? "주제별 연습",
        topics: (grouped.get(category) ?? []).sort((a, b) => a.topicOrder - b.topicOrder),
      }));
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
    setSelectedSelectionId(null);
    setSelectedId((prev) => (prev === topic.id ? null : topic.id));
  };

  const handleSubmit = async () => {
    if (!selectedId) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const selection = await submitTopicSelection(selectedId);
      setSelectedSelectionId(selection.id);
      setTargetLevel(readStoredTargetLevel());
      setMode("ready");
    } catch (err) {
      setError(err instanceof Error ? err.message : "선택 저장 중 문제가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleStartPractice = () => {
    if (!selectedId || !selectedSelectionId) return;
    try {
      window.localStorage.setItem(TARGET_LEVEL_STORAGE_KEY, targetLevel);
    } catch {
      // Remembering the choice is a convenience only.
    }
    router.push(`${ROUTES.practiceSession(selectedId)}?selectionId=${selectedSelectionId}&target=${targetLevel}`);
  };

  const topicCount = (count: number) => `${count}개 주제`;

  // ── Ready screen ───────────────────────────────────────────
  if (mode === "ready") {
    return (
      <PageShell width="md" nav={<TopNav userLabel={userLabel} onLogout={onLogout} />}>
        <section className="mx-auto flex w-full max-w-xl flex-col items-center gap-8 rounded-3xl border bg-card px-6 py-12 text-center shadow-raised sm:px-10">
          <div className="flex flex-col items-center gap-2">
            <Mascot label="" className="max-w-36" />
            <p className="text-sm text-muted-foreground">
              인터뷰어 오피가 영어로 질문을 읽어 드려요. 질문이 끝나면 바로 녹음이 시작돼요.
            </p>
          </div>
          <div className="flex flex-col items-center gap-3">
            <h1 className="text-3xl font-bold sm:text-4xl">{selectedTopic?.title ?? ""}</h1>
            {selectedTopic?.englishTitle ? (
              <p className="text-base text-muted-foreground">{selectedTopic.englishTitle}</p>
            ) : null}
            {selectedTopic?.badges && selectedTopic.badges.length > 0 ? (
              <div className="flex flex-wrap justify-center gap-2 pt-1">
                {selectedTopic.badges.map((badge) => (
                  <Badge key={badge.label} variant="secondary">
                    {badge.label}
                    {badge.count != null ? ` ${badge.count}` : ""}
                  </Badge>
                ))}
              </div>
            ) : null}
          </div>

          <fieldset className="flex w-full max-w-sm flex-col gap-3 text-left">
            <legend className="mb-3 text-sm font-semibold">목표 레벨</legend>
            <div className="grid grid-cols-3 gap-2">
              {TARGET_LEVELS.map((option) => (
                <label key={option.level} className="cursor-pointer">
                  <input
                    type="radio"
                    name="target-level"
                    value={option.level}
                    checked={option.level === targetLevel}
                    onChange={() => setTargetLevel(option.level)}
                    className="peer sr-only"
                  />
                  <span className="flex items-center justify-center rounded-2xl border bg-card py-3 text-lg font-extrabold transition-colors hover:bg-secondary peer-checked:border-primary peer-checked:bg-accent peer-checked:text-primary-strong peer-focus-visible:ring-[3px] peer-focus-visible:ring-ring/50">
                    {option.level}
                  </span>
                </label>
              ))}
            </div>
            <p className="text-sm text-muted-foreground">
              {TARGET_LEVELS.find((option) => option.level === targetLevel)?.description}. 이 레벨을 기준으로 교정과 목표 스크립트를 만들어요.
            </p>
          </fieldset>

          <div className="flex w-full flex-col items-center gap-3">
            {error ? (
              <Alert variant="destructive" className="text-left">
                <AlertCircle />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            ) : null}
            <Button
              type="button"
              onClick={handleStartPractice}
              disabled={!selectedSelectionId}
              className="h-12 w-full max-w-sm rounded-full text-base font-semibold shadow-raised"
            >
              <Mic />
              {selectedTopic?.title ?? ""}에 맞는 연습 시작하기
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => { setMode("select"); setError(null); }}
              className="text-muted-foreground"
            >
              <ArrowLeft />
              주제 다시 선택하기
            </Button>
          </div>
        </section>
      </PageShell>
    );
  }

  const submitButton = (
    <Button
      type="button"
      disabled={!selectedId || isSubmitting}
      onClick={handleSubmit}
      className="h-11 w-full rounded-full text-sm font-semibold"
    >
      {isSubmitting ? (
        <>
          <LoaderCircle className="animate-spin" />
          저장 중...
        </>
      ) : (
        "선택 완료"
      )}
    </Button>
  );

  // ── Select screen ──────────────────────────────────────────
  return (
    <PageShell
      className="pb-28 lg:pb-0"
      nav={<TopNav maxWidthClassName="max-w-6xl" userLabel={userLabel} onLogout={onLogout} />}
    >
      <section className="flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-8 md:flex-row md:items-end md:justify-between">
        <SectionHeader
          as="h1"
          title="주제별 연습을 시작해볼까요?"
          description="하나의 주제를 선택하면 그 주제에 맞는 연습 흐름을 준비합니다."
        />
        <div className="relative w-full md:max-w-xs">
          <label htmlFor="topic-search" className="sr-only">
            주제 검색
          </label>
          <Search aria-hidden className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            id="topic-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="주제 검색 (한국어/English)"
            className="h-11 rounded-full bg-card pl-10"
          />
        </div>
      </section>

      {error ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="flex flex-col gap-4">
          {/* Categories: scrollable chips on mobile, card list on desktop */}
          <nav aria-label="카테고리" className="-mx-4 px-4 lg:mx-0 lg:rounded-3xl lg:border lg:bg-card lg:p-5 lg:shadow-card">
            <Eyebrow className="mb-3 hidden lg:block">카테고리</Eyebrow>
            <ul className="flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none] lg:flex-col lg:overflow-visible lg:pb-0">
              {categories.map((category) => {
                const isActive = category.id === activeCategoryId;
                return (
                  <li key={category.id} className="shrink-0 lg:shrink">
                    <button
                      type="button"
                      aria-pressed={isActive}
                      onClick={() => {
                        setActiveCategoryId(category.id);
                        setQuery("");
                      }}
                      className={cn(
                        "flex w-full flex-col gap-1 rounded-full border px-4 py-2 text-left whitespace-nowrap transition-colors lg:rounded-2xl lg:px-4 lg:py-3 lg:whitespace-normal",
                        isActive
                          ? "border-primary bg-primary text-primary-foreground"
                          : "bg-card text-foreground hover:border-primary/40"
                      )}
                    >
                      <span className="flex items-center justify-between gap-3">
                        <span className="text-sm font-semibold">{category.label}</span>
                        <span className={cn("text-xs", isActive ? "text-primary-foreground/85" : "text-muted-foreground")}>
                          {topicCount(category.topics.length)}
                        </span>
                      </span>
                      <span className={cn("hidden text-sm lg:block", isActive ? "text-primary-foreground/85" : "text-muted-foreground")}>
                        {category.description}
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </nav>

          {/* Selection summary + CTA (desktop). Mobile uses the sticky bar below. */}
          <div className="hidden flex-col gap-4 rounded-3xl border bg-card p-5 shadow-card lg:sticky lg:top-6 lg:flex">
            <Eyebrow>선택된 주제</Eyebrow>
            {selectedTopic ? (
              <div className="rounded-2xl border border-primary/30 bg-accent p-4">
                <p className="text-sm font-semibold text-accent-foreground">{selectedTopic.title}</p>
                <p className="text-sm text-muted-foreground">{selectedTopic.englishTitle}</p>
                {selectedTopic.badges && selectedTopic.badges.length > 0 ? (
                  <div className="mt-3 flex flex-wrap gap-1.5">
                    {selectedTopic.badges.map((badge) => (
                      <Badge key={`${selectedTopic.id}-${badge.label}`} variant="outline">
                        {badge.label}
                        {badge.count != null ? ` ${badge.count}` : ""}
                      </Badge>
                    ))}
                  </div>
                ) : null}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">아직 선택된 주제가 없습니다.</p>
            )}
            {submitButton}
          </div>
        </aside>

        <section className="flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
          <SectionHeader
            title={activeCategory?.label ?? "주제 목록"}
            action={<span className="text-sm text-muted-foreground">{topicCount(filteredTopics.length)}</span>}
          />

          {isLoading ? (
            <div className="grid gap-3 sm:grid-cols-2" aria-busy="true">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={`skeleton-${i}`} className="h-24 rounded-2xl" />
              ))}
            </div>
          ) : activeCategoryId === CATEGORY_SURVEY ? (
            <div className="flex flex-col gap-6">
              {groupedSurveyTopics.length === 0 ? (
                <p className="rounded-2xl border border-dashed px-4 py-8 text-center text-sm text-muted-foreground">
                  조건에 맞는 설문 항목이 없습니다.
                </p>
              ) : null}
              {groupedSurveyTopics.map(({ group, items }) => (
                <div key={group} className="flex flex-col gap-3">
                  <h3 className="text-sm font-semibold text-muted-foreground">{group}</h3>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {items.map((topic) => (
                      <TopicCard key={topic.id} topic={topic} selected={selectedId === topic.id} onSelect={handleSelect} />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : filteredTopics.length === 0 ? (
            <p className="rounded-2xl border border-dashed px-4 py-8 text-center text-sm text-muted-foreground">
              조건에 맞는 주제가 없습니다.
            </p>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2">
              {filteredTopics.map((topic) => (
                <TopicCard key={topic.id} topic={topic} selected={selectedId === topic.id} onSelect={handleSelect} />
              ))}
            </div>
          )}
        </section>
      </div>

      {/* Mobile sticky CTA */}
      <div className="fixed inset-x-0 bottom-0 z-40 border-t bg-card/95 px-4 py-3 shadow-raised backdrop-blur lg:hidden">
        <div className="mx-auto flex max-w-6xl items-center gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-xs text-muted-foreground">선택된 주제</p>
            <p className="truncate text-sm font-semibold">{selectedTopic?.title ?? "아직 선택된 주제가 없습니다."}</p>
          </div>
          <div className="w-32 shrink-0">{submitButton}</div>
        </div>
      </div>
    </PageShell>
  );
}
