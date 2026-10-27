"use client";

import { ArrowRight, Clock, Headphones, ListChecks, Repeat, Undo2, Coins } from "lucide-react";
import { Mascot } from "@/components/brand/Mascot";
import { SectionHeader } from "@/components/common/SectionHeader";
import { Button } from "@/components/ui/button";
import { TARGET_LEVELS } from "@/features/practice/evaluation-labels";
import type { TargetLevel } from "@/features/practice/types";
import { PRACTICE_NOTICE, TOTAL_QUESTIONS } from "@/features/mock/exam";

type Props = {
  cost: number | null;
  targetLevel: TargetLevel;
  onTargetLevelChange: (level: TargetLevel) => void;
  onNext: () => void;
};

export function MockIntro({ cost, targetLevel, onTargetLevelChange, onNext }: Props) {
  const rules = [
    { icon: Clock, text: `약 40분 동안 ${TOTAL_QUESTIONS}문항에 답해요. 시간이 끝나면 그때까지의 답변으로 자동 제출돼요.` },
    { icon: ListChecks, text: "시작 전에 Background Survey와 자가평가를 해요. 고른 주제와 난이도로 질문이 나와요." },
    { icon: Headphones, text: "질문은 소리로만 나와요. 듣고 나면 바로 녹음이 시작돼요." },
    { icon: Repeat, text: "질문은 최대 2번 들을 수 있어요. 다시 들으면 그 문항의 녹음은 처음부터 다시 해요." },
    { icon: Undo2, text: "이전 문항으로 돌아갈 수 없어요. 7번 문항 뒤에 남은 질문의 난이도를 한 번 다시 골라요." },
    {
      icon: Coins,
      text: cost == null ? "제출할 때 크레딧이 차감돼요." : `제출할 때 ${cost}크레딧이 한 번 차감돼요. 시작만 하고 나가면 차감되지 않아요.`,
    },
  ];

  return (
    <section className="flex flex-col items-center gap-8 rounded-3xl border bg-card px-5 py-10 text-center shadow-card sm:px-10">
      <Mascot expression="smile" label="" className="max-w-40" />
      <SectionHeader
        as="h1"
        title="실전연습"
        description="실제 OPIc 순서대로 설문부터 15문항까지 한 번에 연습해요."
        className="justify-center"
      />
      <p className="rounded-full bg-secondary px-4 py-2 text-xs font-semibold text-muted-foreground">{PRACTICE_NOTICE}</p>

      <ul className="flex w-full max-w-xl flex-col gap-3 text-left">
        {rules.map(({ icon: Icon, text }) => (
          <li key={text} className="flex items-start gap-3 rounded-2xl bg-secondary/60 px-4 py-3 text-sm leading-relaxed">
            <Icon aria-hidden className="mt-0.5 size-4 shrink-0 text-primary-strong" />
            {text}
          </li>
        ))}
      </ul>

      <fieldset className="flex w-full max-w-sm flex-col gap-3 text-left">
        <legend className="mb-3 text-sm font-semibold">리포트 목표 레벨</legend>
        <div className="grid grid-cols-3 gap-2">
          {TARGET_LEVELS.map((option) => (
            <label key={option.level} className="cursor-pointer">
              <input
                type="radio"
                name="mock-target-level"
                value={option.level}
                checked={option.level === targetLevel}
                onChange={() => onTargetLevelChange(option.level)}
                className="peer sr-only"
              />
              <span className="flex items-center justify-center rounded-2xl border bg-card py-3 text-lg font-extrabold transition-colors hover:bg-secondary peer-checked:border-primary peer-checked:bg-accent peer-checked:text-primary-strong peer-focus-visible:ring-[3px] peer-focus-visible:ring-ring/50">
                {option.level}
              </span>
            </label>
          ))}
        </div>
        <p className="text-sm text-muted-foreground">
          {TARGET_LEVELS.find((option) => option.level === targetLevel)?.description}. 리포트에서 이 레벨과의 차이를 알려 줘요.
        </p>
      </fieldset>

      <Button type="button" onClick={onNext} className="h-11 rounded-full px-8 text-sm font-semibold">
        설문 시작하기
        <ArrowRight />
      </Button>
    </section>
  );
}
