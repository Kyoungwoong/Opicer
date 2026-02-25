"use client";

import { useMemo, useState } from "react";
import { TopNav } from "@/components/common/TopNav";
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
    <div className="rounded-[28px] border border-black/5 bg-white/70 p-5 shadow-sm">
      <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Categories</p>
      <div className="mt-4 space-y-2">
        {(["topic", "mock"] as const).map((type) => {
          const isActive = activeType === type;
          return (
            <button
              key={type}
              type="button"
              onClick={() => onChange(type)}
              className={`w-full rounded-2xl border px-4 py-4 text-left transition ${
                isActive
                  ? "border-[var(--accent)] bg-[var(--accent)]/10 shadow-md"
                  : "border-black/10 bg-white/70 hover:border-[var(--accent)]/40"
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold">{SECTION_META[type].label}</span>
                <span className="rounded-full border border-black/10 px-2.5 py-1 text-[10px] font-semibold text-[var(--muted)]">
                  {counts[type]}건
                </span>
              </div>
              <p className="mt-2 text-xs text-[var(--muted)]">{SECTION_META[type].helper}</p>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function HistoryList({
  items,
  selectedId,
  onSelect,
}: {
  items: PracticeHistory[];
  selectedId?: string | null;
  onSelect: (item: PracticeHistory) => void;
}) {
  if (items.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-black/10 bg-white/60 px-4 py-6 text-sm text-[var(--muted)]">
        기록이 아직 없습니다.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {items.map((item) => {
        const isActive = item.id === selectedId;
        return (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelect(item)}
            className={`w-full rounded-2xl border px-4 py-4 text-left transition ${
              isActive
                ? "border-[var(--accent)] bg-[var(--accent)]/10 shadow-md"
                : "border-black/10 bg-white/70 hover:border-[var(--accent)]/40"
            }`}
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-[var(--ink)]">{item.title}</p>
                <p className="mt-1 text-xs text-[var(--muted)]">{item.topicLabel}</p>
              </div>
              <span className="rounded-full border border-black/10 px-2.5 py-1 text-[10px] font-semibold text-[var(--muted)]">
                {item.durationMinutes}분
              </span>
            </div>
            <div className="mt-3 flex items-center justify-between text-xs text-[var(--muted)]">
              <span>{item.date}</span>
              <span>히스토리 보기 →</span>
            </div>
          </button>
        );
      })}
    </div>
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
      <div className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
        <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Detail</p>
        <h3 className="mt-2 text-xl font-semibold">선택된 기록 없음</h3>
        <p className="mt-2 text-sm text-[var(--muted)]">
          가운데 목록에서 히스토리를 선택하면 연습 상세 화면이 나타납니다.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
              Practice Summary
            </p>
            <h3 className="mt-2 text-2xl font-semibold">{item.title}</h3>
            <p className="mt-2 text-sm text-[var(--muted)]">
              {item.topicLabel} · {item.date} · {item.durationMinutes}분
            </p>
          </div>
          <button
            type="button"
            onClick={onBack}
            className="rounded-full border border-black/10 px-3 py-1 text-xs font-semibold text-[var(--muted)] hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
          >
            목록으로 돌아가기
          </button>
        </div>
        <p className="mt-4 text-sm text-[var(--muted)]">{item.summary}</p>
      </section>

      <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
        <div className="flex items-center justify-between">
          <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Analysis</p>
          <span className="rounded-full border border-black/10 px-3 py-1 text-[10px] font-semibold text-[var(--muted)]">
            준비 중
          </span>
        </div>
        <h4 className="mt-2 text-xl font-semibold">연습 완료 화면</h4>
        <p className="mt-2 text-sm text-[var(--muted)]">
          오디오 재생, 스크립트, 수정 기능은 추후 제공됩니다.
        </p>

        <div className="mt-4 grid gap-3">
          <div className="rounded-2xl border border-dashed border-black/10 bg-white/60 px-4 py-4 text-sm text-[var(--muted)]">
            녹음 재생 기능 준비 중
          </div>
          <div className="rounded-2xl border border-dashed border-black/10 bg-white/60 px-4 py-4 text-sm text-[var(--muted)]">
            스크립트 보기 기능 준비 중
          </div>
          <div className="rounded-2xl border border-dashed border-black/10 bg-white/60 px-4 py-4 text-sm text-[var(--muted)]">
            스크립트 수정 기능 준비 중
          </div>
        </div>
      </section>

      <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
        <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Answers</p>
        <h4 className="mt-2 text-xl font-semibold">질문/답변 리스트</h4>
        <div className="mt-4 space-y-3">
          {item.answers.map((answer, index) => (
            <div
              key={answer.id}
              className="rounded-2xl border border-black/10 bg-white/70 p-4"
            >
              <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
                Question {index + 1}
              </p>
              <p className="mt-2 text-sm font-semibold">{answer.question}</p>
              <div className="mt-3 rounded-xl border border-dashed border-black/10 bg-white/60 px-3 py-2 text-xs text-[var(--muted)]">
                녹음 미리듣기 준비 중
              </div>
            </div>
          ))}
        </div>
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
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <TopNav userLabel={userLabel} onLogout={onLogout} maxWidthClassName="max-w-6xl" />

        <section className="rounded-[32px] border border-black/5 bg-white/70 p-8 shadow-sm">
          <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">My Page</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight">연습 히스토리</h1>
          <p className="mt-2 text-sm text-[var(--muted)]">
            주제별/실전 연습 기록을 확인하고 연습 상세 화면으로 이동할 수 있어요.
          </p>
        </section>

        <div className="grid gap-8 lg:grid-cols-[220px_minmax(0,1fr)_minmax(0,420px)]">
          <aside className="space-y-6">
            <CategoryList
              activeType={activeType}
              onChange={handleCategoryChange}
              counts={{ topic: topicHistory.length, mock: mockHistory.length }}
            />
          </aside>

          <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
            <div className="flex items-end justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
                  {SECTION_META[activeType].label}
                </p>
                <h2 className="mt-1 text-2xl font-semibold">{SECTION_META[activeType].label}</h2>
                <p className="mt-2 text-sm text-[var(--muted)]">
                  {SECTION_META[activeType].helper}
                </p>
              </div>
              <span className="text-xs text-[var(--muted)]">{items.length}건</span>
            </div>

            <div className="mt-6">
              <HistoryList items={items} selectedId={selected?.id} onSelect={setSelected} />
            </div>
          </section>

          <aside>
            <HistoryDetail item={selected} onBack={() => setSelected(null)} />
          </aside>
        </div>
      </div>
    </div>
  );
}
