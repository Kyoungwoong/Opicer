"use client";

import { ArrowLeft, ArrowRight } from "lucide-react";
import { SectionHeader } from "@/components/common/SectionHeader";
import { Button } from "@/components/ui/button";
import { SELF_ASSESSMENT_OPTIONS } from "@/features/mock/exam";
import type { SelfAssessment } from "@/features/mock/types";

type Props = {
  value: SelfAssessment | null;
  onChange: (value: SelfAssessment) => void;
  onBack: () => void;
  onNext: () => void;
};

export function SelfAssessmentStep({ value, onChange, onBack, onNext }: Props) {
  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
        <SectionHeader
          as="h1"
          eyebrow="2단계 · Self Assessment"
          title="지금 말하기 실력은 어느 정도인가요?"
          description="높은 단계를 고를수록 어려운 질문이 나와요. 고른 단계가 곧 등급은 아니에요."
        />
      </div>

      <fieldset className="flex flex-col gap-3">
        <legend className="sr-only">자가평가 단계</legend>
        {SELF_ASSESSMENT_OPTIONS.map((option) => (
          <label key={option.value} className="cursor-pointer">
            <input
              type="radio"
              name="self-assessment"
              value={option.value}
              checked={value === option.value}
              onChange={() => onChange(option.value)}
              className="peer sr-only"
            />
            <span className="flex items-center gap-4 rounded-2xl border bg-card px-4 py-4 shadow-card transition-colors hover:bg-secondary peer-checked:border-primary peer-checked:bg-accent peer-focus-visible:ring-[3px] peer-focus-visible:ring-ring/50">
              <span className="grid size-10 shrink-0 place-items-center rounded-full bg-secondary text-lg font-extrabold">
                {option.value}
              </span>
              <span className="text-sm leading-relaxed">{option.description}</span>
            </span>
          </label>
        ))}
      </fieldset>

      <div className="flex items-center justify-between gap-3">
        <Button type="button" variant="ghost" onClick={onBack} className="text-muted-foreground">
          <ArrowLeft />
          설문으로
        </Button>
        <Button type="button" onClick={onNext} disabled={value == null} className="h-11 rounded-full px-6 text-sm font-semibold">
          다음
          <ArrowRight />
        </Button>
      </div>
    </section>
  );
}
