"use client";

import { useMemo, useState } from "react";
import { ArrowLeft, ChevronRight, Clock } from "lucide-react";
import { cn } from "@/lib/utils";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Eyebrow, SectionHeader } from "@/components/common/SectionHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PRACTICE_HISTORY } from "@/features/mypage/data";
import type { PracticeHistory, PracticeHistoryType } from "@/features/mypage/types";

type Props = {
  userLabel?: string;
  onLogout?: () => void;
};

const SECTION_META: Record<PracticeHistoryType, { label: string; helper: string }> = {
  topic: {
    label: "주제별 연습하기",
    helper: "선택한 주제 기반으로 연습한 기록을 확인할 수 있어요.",
  },
  mock: {
    label: "실전 연습하기",
    helper: "시간 제한을 고려한 실전 연습 기록을 모아 보여줘요.",
  },
};

const CARD = "flex flex-col gap-5 rounded-3xl border bg-card/90 p-5 shadow-card sm:p-6";
const PLACEHOLDER = "rounded-2xl border border-dashed bg-background/60 px-4 py-4 text-sm text-muted-foreground";

function CategoryList({
  activeType,
  onChange,
  counts,
}: {
  activeType: PracticeHistoryType;
  onChange: (type: PracticeHistoryType) => void;
  counts: Record<PracticeHistoryType, number>;
}) {
  return (
    <nav aria-label="연습 종류" className="-mx-4 px-4 lg:mx-0 lg:rounded-3xl lg:border lg:bg-card/90 lg:p-5 lg:shadow-card">
      <Eyebrow latin className="mb-3 hidden lg:block">
        Categories
      </Eyebrow>
      <ul className="flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none] lg:flex-col lg:overflow-visible lg:pb-0">
        {(["topic", "mock"] as const).map((type) => {
          const isActive = activeType === type;
          return (
            <li key={type} className="shrink-0 lg:shrink">
              <button
                type="button"
                aria-pressed={isActive}
                onClick={() => onChange(type)}
                className={cn(
                  "flex w-full flex-col gap-1.5 rounded-full border px-4 py-2 text-left whitespace-nowrap transition-colors lg:rounded-2xl lg:py-3.5 lg:whitespace-normal",
                  isActive ? "border-primary bg-accent text-accent-foreground" : "bg-card hover:border-primary/40"
                )}
              >
                <span className="flex items-center justify-between gap-3">
                  <span className="text-sm font-semibold">{SECTION_META[type].label}</span>
                  <Badge variant={isActive ? "default" : "secondary"}>{counts[type]}건</Badge>
                </span>
                <span className="hidden text-sm text-muted-foreground lg:block">{SECTION_META[type].helper}</span>
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

function HistoryList({
  items,
  onSelect,
}: {
  items: PracticeHistory[];
  onSelect: (item: PracticeHistory) => void;
}) {
  if (items.length === 0) {
    return <div className={PLACEHOLDER}>기록이 아직 없습니다.</div>;
  }

  return (
    <ul className="flex flex-col gap-2">
      {items.map((item) => {
        return (
          <li key={item.id}>
            <button
              type="button"
              onClick={() => onSelect(item)}
              className="group flex w-full flex-col gap-3 rounded-2xl border bg-background/60 px-4 py-4 text-left transition-colors hover:border-primary/40 hover:bg-card"
            >
              <span className="flex items-start justify-between gap-4">
                <span className="min-w-0">
                  <span className="block text-sm font-semibold text-foreground">{item.title}</span>
                  <span className="mt-0.5 block text-sm text-muted-foreground">{item.topicLabel}</span>
                </span>
                <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground">
                  <Clock aria-hidden className="size-3.5" />
                  {item.durationMinutes}분
                </span>
              </span>
              <span className="flex items-center justify-between text-xs text-muted-foreground">
                <span>{item.date}</span>
                <span className="inline-flex items-center gap-0.5 font-medium text-primary group-hover:underline">
                  히스토리 보기
                  <ChevronRight aria-hidden className="size-3.5" />
                </span>
              </span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}

function HistoryDetail({
  item,
  onBack,
}: {
  item: PracticeHistory | null;
  onBack: () => void;
}) {
  if (!item) {
    return (
      <div className={CARD}>
        <SectionHeader
          eyebrow="Detail"
          eyebrowLatin
          title="선택된 기록 없음"
          description="가운데 목록에서 히스토리를 선택하면 연습 상세 화면이 나타납니다."
        />
      </div>
    );
  }

  return (
    <div className="flex min-w-0 flex-col gap-6">
      <section className={CARD}>
        <SectionHeader
          eyebrow="Practice Summary"
          eyebrowLatin
          title={item.title}
          description={`${item.topicLabel} · ${item.date} · ${item.durationMinutes}분`}
          action={
            <Button type="button" variant="outline" onClick={onBack} className="rounded-full px-4">
              <ArrowLeft />
              목록으로 돌아가기
            </Button>
          }
        />
        <p className="text-sm leading-6 text-muted-foreground">{item.summary}</p>
      </section>

      <section className={CARD}>
        <SectionHeader
          eyebrow="Analysis"
          eyebrowLatin
          title="연습 완료 화면"
          description="오디오 재생, 스크립트, 수정 기능은 추후 제공됩니다."
          action={<Badge variant="secondary">준비 중</Badge>}
        />
        <div className="grid gap-2">
          <div className={PLACEHOLDER}>녹음 재생 기능 준비 중</div>
          <div className={PLACEHOLDER}>스크립트 보기 기능 준비 중</div>
          <div className={PLACEHOLDER}>스크립트 수정 기능 준비 중</div>
        </div>
      </section>

      <section className={CARD}>
        <SectionHeader eyebrow="Answers" eyebrowLatin title="질문/답변 리스트" />
        <ol className="flex flex-col gap-2">
          {item.answers.map((answer, index) => (
            <li key={answer.id} className="flex flex-col gap-2 rounded-2xl border bg-background/60 p-4">
              <Eyebrow latin>Question {index + 1}</Eyebrow>
              <p className="text-sm font-semibold">{answer.question}</p>
              <div className="rounded-xl border border-dashed px-3 py-2 text-xs text-muted-foreground">
                녹음 미리듣기 준비 중
              </div>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}

export function MyPageView({ userLabel, onLogout }: Props) {
  const topicHistory = useMemo(
    () => PRACTICE_HISTORY.filter((item) => item.type === "topic"),
    []
  );
  const mockHistory = useMemo(
    () => PRACTICE_HISTORY.filter((item) => item.type === "mock"),
    []
  );

  const [activeType, setActiveType] = useState<PracticeHistoryType>("topic");
  const [selected, setSelected] = useState<PracticeHistory | null>(null);

  const items = activeType === "topic" ? topicHistory : mockHistory;

  const handleCategoryChange = (type: PracticeHistoryType) => {
    setActiveType(type);
    setSelected(null);
  };

  return (
    <PageShell nav={<TopNav userLabel={userLabel} onLogout={onLogout} maxWidthClassName="max-w-6xl" />}>
      <section className="rounded-3xl border bg-card/90 p-6 shadow-card sm:p-8">
        <SectionHeader
          as="h1"
          eyebrow="My Page"
          eyebrowLatin
          title="연습 히스토리"
          description="주제별/실전 연습 기록을 확인하고 연습 상세 화면으로 이동할 수 있어요."
        />
      </section>

      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-[240px_minmax(0,1fr)] lg:gap-8">
        <aside>
          <CategoryList
            activeType={activeType}
            onChange={handleCategoryChange}
            counts={{ topic: topicHistory.length, mock: mockHistory.length }}
          />
        </aside>

        {selected ? (
          <HistoryDetail item={selected} onBack={() => setSelected(null)} />
        ) : (
          <section className={CARD}>
            <SectionHeader
              eyebrow="History"
              eyebrowLatin
              title={SECTION_META[activeType].label}
              description={SECTION_META[activeType].helper}
              action={<span className="text-sm text-muted-foreground">{items.length}건</span>}
            />
            <HistoryList items={items} onSelect={setSelected} />
          </section>
        )}
      </div>
    </PageShell>
  );
}
