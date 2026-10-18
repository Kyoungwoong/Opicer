"use client";

import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, Quote } from "lucide-react";
import { cn } from "@/lib/utils";
import { fetchUniversalSentences } from "@/features/home/api";
import { UNIVERSAL_SENTENCES } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { UniversalSentence } from "@/features/home/types";
import { ko } from "@/locales/ko";

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
    fetchUniversalSentences()
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
      description={ko.home.universalSentences.description}
      action={<Badge variant="secondary">{ko.home.universalSentences.actionLabel}</Badge>}
    >
      <div className="relative flex min-h-[220px] flex-col gap-4 overflow-hidden rounded-2xl bg-primary-strong p-5 text-primary-foreground sm:p-7">
        <Quote aria-hidden className="pointer-events-none absolute -top-2 right-4 size-24 text-primary-foreground/10" />

        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="flex flex-col gap-1.5">
            <p className="text-sm font-semibold text-primary-foreground/90">
              {current?.title ?? ko.home.universalSentences.titleLoading}
            </p>
            <span className="w-fit rounded-full bg-primary-foreground/15 px-2.5 py-0.5 text-xs font-medium">
              {current ? TYPE_LABELS[current.type] : "Loading"}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              size="icon-lg"
              onClick={() => goTo(index - 1)}
              className="rounded-full bg-primary-foreground/15 text-primary-foreground hover:bg-primary-foreground/25"
              aria-label="Previous sentence"
            >
              <ChevronLeft />
            </Button>
            <Button
              type="button"
              size="icon-lg"
              onClick={() => goTo(index + 1)}
              className="rounded-full bg-primary-foreground/15 text-primary-foreground hover:bg-primary-foreground/25"
              aria-label="Next sentence"
            >
              <ChevronRight />
            </Button>
          </div>
        </div>

        <p className="text-lg font-semibold leading-relaxed break-words sm:text-xl">
          {current?.sentence ?? ko.home.universalSentences.sentenceLoading}
        </p>

        <div className="flex flex-wrap gap-2">
          {current?.tags?.map((tag) => (
            <span key={tag} className="rounded-full border border-primary-foreground/25 px-2.5 py-0.5 text-xs font-medium">
              #{tag}
            </span>
          ))}
        </div>

        <div className="mt-auto flex items-center gap-1.5 pt-2">
          {sentences.map((item, dotIndex) => (
            <button
              key={item.id}
              type="button"
              onClick={() => goTo(dotIndex)}
              className={cn(
                "h-2 rounded-full transition-all",
                dotIndex === index ? "w-6 bg-highlight" : "w-2 bg-primary-foreground/30 hover:bg-primary-foreground/60"
              )}
              aria-label={`Go to sentence ${dotIndex + 1}`}
              aria-current={dotIndex === index ? "true" : undefined}
            />
          ))}
        </div>
      </div>
    </SectionShell>
  );
}
