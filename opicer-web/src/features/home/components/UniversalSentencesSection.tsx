"use client";

import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";
import { fetchUniversalSentences } from "@/features/home/api";
import { UNIVERSAL_SENTENCES } from "@/features/home/data";
import { SectionShell } from "@/features/home/components/SectionShell";
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
      tone="highlight"
      title={ko.home.universalSentences.description}
      action={
        <span className="rounded-full bg-card px-3 py-1 text-sm font-bold text-highlight-foreground">
          {current ? TYPE_LABELS[current.type] : "Loading"}
        </span>
      }
    >
      <div className="flex min-h-[200px] flex-1 flex-col gap-4">
        <p className="text-sm font-semibold text-highlight-foreground">
          {current?.title ?? ko.home.universalSentences.titleLoading}
        </p>
        <p className="text-xl leading-relaxed font-bold break-words text-foreground sm:text-2xl">
          “{current?.sentence ?? ko.home.universalSentences.sentenceLoading}”
        </p>

        <div className="flex flex-wrap gap-2">
          {current?.tags?.map((tag) => (
            <span key={tag} className="rounded-full bg-card px-2.5 py-0.5 text-xs font-medium text-highlight-foreground">
              #{tag}
            </span>
          ))}
        </div>

        <div className="mt-auto flex items-center justify-between gap-3 pt-2">
          <div className="flex items-center gap-1.5">
            {sentences.map((item, dotIndex) => (
              <button
                key={item.id}
                type="button"
                onClick={() => goTo(dotIndex)}
                className={cn(
                  "h-2 rounded-full transition-all",
                  dotIndex === index ? "w-6 bg-foreground" : "w-2 bg-foreground/20 hover:bg-foreground/40"
                )}
                aria-label={`Go to sentence ${dotIndex + 1}`}
                aria-current={dotIndex === index ? "true" : undefined}
              />
            ))}
          </div>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="icon-lg"
              onClick={() => goTo(index - 1)}
              className="rounded-full border-highlight/40 bg-card"
              aria-label="Previous sentence"
            >
              <ChevronLeft />
            </Button>
            <Button
              type="button"
              size="icon-lg"
              onClick={() => goTo(index + 1)}
              className="rounded-full bg-foreground text-background hover:bg-foreground/85"
              aria-label="Next sentence"
            >
              <ChevronRight />
            </Button>
          </div>
        </div>
      </div>
    </SectionShell>
  );
}
