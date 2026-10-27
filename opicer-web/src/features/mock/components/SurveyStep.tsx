"use client";

import { useState } from "react";
import { AlertCircle, ArrowLeft, ArrowRight, Check } from "lucide-react";
import { cn } from "@/lib/utils";
import { SectionHeader } from "@/components/common/SectionHeader";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { isPart4, part4Total, surveyProblems, toggleSelection } from "@/features/mock/survey";
import type { SurveyCatalog, SurveySelections } from "@/features/mock/types";

type Props = {
  catalog: SurveyCatalog;
  selections: SurveySelections;
  onChange: (selections: SurveySelections) => void;
  onBack: () => void;
  onNext: () => void;
};

/** Background Survey: parts 1–3 take one answer each, Part 4 at least 12 across its four groups. */
export function SurveyStep({ catalog, selections, onChange, onBack, onNext }: Props) {
  const [showProblems, setShowProblems] = useState(false);
  const problems = surveyProblems(catalog, selections);
  const total = part4Total(catalog, selections);
  const firstPart4 = catalog.parts.find(isPart4)?.code;

  const handleNext = () => {
    if (problems.length > 0) {
      setShowProblems(true);
      return;
    }
    onNext();
  };

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
        <SectionHeader
          as="h1"
          eyebrow="1단계 · Background Survey"
          title="나에 대해 알려 주세요"
          description="고른 항목에서 질문 주제가 정해져요. 이 브라우저에 저장해 두었다가 다음에 다시 불러와요."
        />
      </div>

      {catalog.parts.map((part) => {
        const chosen = selections[part.code] ?? [];
        const single = part.max === 1;
        return (
          <fieldset key={part.code} className="flex flex-col gap-4 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
            <legend className="sr-only">{part.title}</legend>
            {part.code === firstPart4 ? (
              <p
                aria-live="polite"
                className={cn(
                  "self-start rounded-full px-3 py-1 text-xs font-semibold",
                  total >= catalog.part4MinTotal ? "bg-success-subtle text-success" : "bg-secondary text-muted-foreground"
                )}
              >
                Part 4 합계 {total} / {catalog.part4MinTotal}개 이상
              </p>
            ) : null}
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <p aria-hidden className="text-base font-bold leading-relaxed">
                {part.title}
              </p>
              {!single ? <span className="text-sm text-muted-foreground">{chosen.length}개 선택</span> : null}
            </div>
            <div className="flex flex-wrap gap-2">
              {part.items.map((item) => {
                const checked = chosen.includes(item.id);
                return (
                  <label key={item.id} className="cursor-pointer">
                    <input
                      type={single ? "radio" : "checkbox"}
                      name={`survey-${part.code}`}
                      value={item.id}
                      checked={checked}
                      onChange={() => onChange(toggleSelection(part, selections, item.id))}
                      className="peer sr-only"
                    />
                    <span className="inline-flex items-center gap-1.5 rounded-full border bg-card px-4 py-2 text-sm transition-colors hover:bg-secondary peer-checked:border-primary peer-checked:bg-accent peer-checked:font-semibold peer-checked:text-primary-strong peer-focus-visible:ring-[3px] peer-focus-visible:ring-ring/50">
                      {checked ? <Check aria-hidden className="size-3.5" /> : null}
                      {item.label}
                    </span>
                  </label>
                );
              })}
            </div>
          </fieldset>
        );
      })}

      {showProblems && problems.length > 0 ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertDescription>
            <ul className="list-disc pl-4">
              {problems.map((problem) => (
                <li key={problem}>{problem}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      ) : null}

      <div className="flex items-center justify-between gap-3">
        <Button type="button" variant="ghost" onClick={onBack} className="text-muted-foreground">
          <ArrowLeft />
          안내로
        </Button>
        <Button type="button" onClick={handleNext} className="h-11 rounded-full px-6 text-sm font-semibold">
          다음
          <ArrowRight />
        </Button>
      </div>
    </section>
  );
}
