"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, ShieldAlert } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthState } from "@/lib/use-auth-state";
import { ROUTES } from "@/lib/routes";
import { LoadingScreen } from "@/components/common/LoadingScreen";
import { PageShell } from "@/components/common/PageShell";
import { SectionHeader } from "@/components/common/SectionHeader";
import { Button } from "@/components/ui/button";
import { QuestionTab } from "@/features/admin/QuestionTab";
import { TopicTab } from "@/features/admin/TopicTab";
import { SurveyLinkTab } from "@/features/admin/SurveyLinkTab";
import { DailySentenceTab } from "@/features/admin/DailySentenceTab";
import { PromptVersionTab } from "@/features/admin/PromptVersionTab";
import { EvaluationGuideTab } from "@/features/admin/EvaluationGuideTab";
import { UniversalSentenceTab } from "@/features/admin/UniversalSentenceTab";
import { GoodAnswerTab } from "@/features/admin/GoodAnswerTab";
import { OpicCenterTab } from "@/features/admin/OpicCenterTab";
import { OpicScheduleTab } from "@/features/admin/OpicScheduleTab";

type Tab =
  | "topics"
  | "survey-links"
  | "questions"
  | "daily-sentences"
  | "prompts"
  | "evaluation-guides"
  | "universal-sentences"
  | "good-answers"
  | "opic-centers"
  | "opic-schedules";

const TABS: { id: Tab; label: string }[] = [
  { id: "topics", label: "주제 관리" },
  { id: "survey-links", label: "설문 연결" },
  { id: "questions", label: "질문 관리" },
  { id: "daily-sentences", label: "오늘의 문장" },
  { id: "universal-sentences", label: "만능 문장" },
  { id: "good-answers", label: "샘플 답변" },
  { id: "evaluation-guides", label: "평가 가이드" },
  { id: "prompts", label: "프롬프트 버전" },
  { id: "opic-centers", label: "시험센터" },
  { id: "opic-schedules", label: "시험 일정" },
];

export default function AdminPage() {
  const auth = useAuthState();
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<Tab>("topics");

  useEffect(() => {
    if (auth.status === "unauthenticated") {
      router.replace(ROUTES.login);
    }
  }, [auth.status, router]);

  if (auth.status === "loading") return <LoadingScreen />;
  if (auth.status === "unauthenticated") return <LoadingScreen />;

  if (auth.user.role !== "ADMIN") {
    return (
      <div className="flex min-h-dvh items-center justify-center px-4">
        <div className="flex flex-col items-center gap-3 rounded-3xl border bg-card px-10 py-12 text-center shadow-card">
          <ShieldAlert aria-hidden className="size-8 text-destructive" />
          <p className="text-2xl font-bold text-foreground">403</p>
          <p className="text-muted-foreground">접근 권한이 없습니다.</p>
          <Button asChild variant="outline" className="mt-2 rounded-full px-5">
            <a href={ROUTES.home}>홈으로 돌아가기</a>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <PageShell width="lg">
      <SectionHeader
        as="h1"
        title="관리자"
        description={`${auth.user.name} 님`}
        action={
          <Button asChild variant="outline" className="rounded-full px-4">
            <a href={ROUTES.home}>
              <ArrowLeft />
              홈
            </a>
          </Button>
        }
      />

      {/* Section switcher (toggle buttons, horizontally scrollable at 375px) */}
      <nav aria-label="관리 메뉴" className="-mx-4 overflow-x-auto px-4 [scrollbar-width:none] sm:mx-0 sm:px-0">
        <div className="inline-flex min-w-full gap-1 rounded-full border bg-card p-1 shadow-card sm:min-w-0">
          {TABS.map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                aria-pressed={isActive}
                onClick={() => setActiveTab(tab.id)}
                className={cn(
                  "shrink-0 rounded-full px-4 py-2 text-sm font-medium whitespace-nowrap transition-colors",
                  isActive
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                )}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </nav>

      {/* Tab Content */}
      <div>
        {activeTab === "topics" && <TopicTab />}
        {activeTab === "survey-links" && <SurveyLinkTab />}
        {activeTab === "questions" && <QuestionTab />}
        {activeTab === "daily-sentences" && <DailySentenceTab />}
        {activeTab === "universal-sentences" && <UniversalSentenceTab />}
        {activeTab === "good-answers" && <GoodAnswerTab />}
        {activeTab === "evaluation-guides" && <EvaluationGuideTab />}
        {activeTab === "prompts" && <PromptVersionTab />}
        {activeTab === "opic-centers" && <OpicCenterTab />}
        {activeTab === "opic-schedules" && <OpicScheduleTab />}
      </div>
    </PageShell>
  );
}
