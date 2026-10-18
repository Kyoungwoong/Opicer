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
import { DailySentenceTab } from "@/features/admin/DailySentenceTab";
import { PromptVersionTab } from "@/features/admin/PromptVersionTab";
import { UniversalSentenceTab } from "@/features/admin/UniversalSentenceTab";
import { GoodAnswerTab } from "@/features/admin/GoodAnswerTab";

type Tab =
  | "questions"
  | "daily-sentences"
  | "prompts"
  | "universal-sentences"
  | "good-answers";

const TABS: { id: Tab; label: string }[] = [
  { id: "questions", label: "질문 관리" },
  { id: "daily-sentences", label: "오늘의 문장" },
  { id: "universal-sentences", label: "만능 문장" },
  { id: "good-answers", label: "샘플 답변" },
  { id: "prompts", label: "프롬프트 버전" },
];

export default function AdminPage() {
  const auth = useAuthState();
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<Tab>("questions");

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
        <div className="flex flex-col items-center gap-3 rounded-3xl border bg-card/90 px-10 py-12 text-center shadow-card">
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
        eyebrow="Admin"
        eyebrowLatin
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

      {/* Tabs: horizontally scrollable so all five fit at 375px */}
      <div className="-mx-4 overflow-x-auto px-4 [scrollbar-width:none] sm:mx-0 sm:px-0">
        <div role="tablist" aria-label="관리 메뉴" className="inline-flex min-w-full gap-1 rounded-full border bg-card/80 p-1 shadow-card sm:min-w-0">
          {TABS.map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={isActive}
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
      </div>

      {/* Tab Content */}
      <div role="tabpanel">
        {activeTab === "questions" && <QuestionTab />}
        {activeTab === "daily-sentences" && <DailySentenceTab />}
        {activeTab === "universal-sentences" && <UniversalSentenceTab />}
        {activeTab === "good-answers" && <GoodAnswerTab />}
        {activeTab === "prompts" && <PromptVersionTab />}
      </div>
    </PageShell>
  );
}
