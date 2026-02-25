"use client";

import { useEffect, useState } from "react";
import { fetchUniversalSentences } from "@/features/home/api";
import { UNIVERSAL_SENTENCES } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";
import type { UniversalSentence } from "@/features/home/types";

const TYPE_LABELS: Record<UniversalSentence["type"], string> = {
  OPINION: "Opinion",
  PAST_EXPERIENCE: "Past Experience",
  COMPARE_CONTRAST: "Compare/Contrast",
  UNEXPECTED_SITUATION: "Unexpected Situation",
};

const SLIDE_INTERVAL_MS = 5000;

export function UniversalSentencesSection() {
  const [sentences, setSentences] =
    useState<UniversalSentence[]>(UNIVERSAL_SENTENCES);
  const [index, setIndex] = useState(0);

  useEffect(() => {
    let mounted = true;
    fetchUniversalSentences(4)
      .then((data) => {
        if (!mounted || data.length === 0) return;
        setSentences(data);
        setIndex(0);
      })
      .catch(() => {
        // fallback to local data
      });
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (sentences.length <= 1) return;
    const timer = window.setInterval(() => {
      setIndex((prev) => (prev + 1) % sentences.length);
    }, SLIDE_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [sentences.length]);

  const current = sentences[index] ?? sentences[0];

  const goTo = (next: number) => {
    const total = sentences.length;
    if (total === 0) return;
    const normalized = (next + total) % total;
    setIndex(normalized);
  };

  return (
    <SectionShell
      title="Universal sentences"
      description="OPIC에서 자주 쓰는 만능 문장"
      action={
        <span className="rounded-full border border-[var(--accent)]/20 px-4 py-2 text-xs font-semibold text-[var(--accent-strong)]">
          관리자 연동 예정
        </span>
      }
    >
      <div className="flex flex-col gap-4 rounded-3xl border border-black/5 bg-white/70 p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-[var(--accent-strong)]">
              {current?.title ?? "오늘의 문장을 불러오는 중입니다"}
            </p>
            <p className="mt-1 text-[10px] uppercase tracking-[0.2em] text-[var(--muted)]">
              {current ? TYPE_LABELS[current.type] : "Loading"}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => goTo(index - 1)}
              className="flex h-9 w-9 items-center justify-center rounded-full border border-black/10 text-sm text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
              aria-label="Previous sentence"
            >
              ←
            </button>
            <button
              type="button"
              onClick={() => goTo(index + 1)}
              className="flex h-9 w-9 items-center justify-center rounded-full border border-black/10 text-sm text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
              aria-label="Next sentence"
            >
              →
            </button>
          </div>
        </div>

        <p className="text-lg font-semibold leading-relaxed text-[var(--ink)]">
          {current?.sentence ?? "데이터를 준비 중입니다."}
        </p>

        <div className="flex flex-wrap gap-2">
          {current?.tags?.map((tag) => (
            <span
              key={tag}
              className="rounded-full border border-[var(--accent)]/20 px-2.5 py-1 text-[10px] font-semibold text-[var(--accent-strong)]"
            >
              #{tag}
            </span>
          ))}
        </div>

        <div className="flex items-center gap-2 pt-2">
          {sentences.map((item, dotIndex) => (
            <button
              key={item.id}
              type="button"
              onClick={() => goTo(dotIndex)}
              className={`h-2.5 w-2.5 rounded-full transition ${
                dotIndex === index
                  ? "bg-[var(--accent)]"
                  : "bg-[var(--accent)]/20 hover:bg-[var(--accent)]/50"
              }`}
              aria-label={`Go to sentence ${dotIndex + 1}`}
            />
          ))}
        </div>
      </div>
    </SectionShell>
  );
}
