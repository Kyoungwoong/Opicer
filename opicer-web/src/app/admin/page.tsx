"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthState } from "@/lib/use-auth-state";
import { ROUTES } from "@/lib/routes";
import { LoadingScreen } from "@/components/common/LoadingScreen";
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
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center space-y-2">
          <p className="text-2xl font-bold text-[var(--ink)]">403</p>
          <p className="text-[var(--muted)]">접근 권한이 없습니다.</p>
          <a
            href={ROUTES.home}
            className="mt-4 inline-block text-sm text-[var(--accent)] hover:underline"
          >
            홈으로 돌아가기
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen py-10 px-4">
      <div className="mx-auto max-w-5xl space-y-8">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-[var(--ink)]">관리자</h1>
            <p className="text-sm text-[var(--muted)]">{auth.user.name} 님</p>
          </div>
          <a
            href={ROUTES.home}
            className="rounded-full border border-black/10 px-4 py-1.5 text-sm text-[var(--muted)] transition hover:bg-black/5"
          >
            ← 홈
          </a>
        </div>

        {/* Tabs */}
        <div className="border-b border-black/8">
          <nav className="flex gap-0">
            {TABS.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-5 py-3 text-sm font-medium transition border-b-2 -mb-px ${
                  activeTab === tab.id
                    ? "border-[var(--accent)] text-[var(--accent-strong)]"
                    : "border-transparent text-[var(--muted)] hover:text-[var(--ink)]"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {/* Tab Content */}
        <div>
          {activeTab === "questions" && <QuestionTab />}
          {activeTab === "daily-sentences" && <DailySentenceTab />}
          {activeTab === "universal-sentences" && <UniversalSentenceTab />}
          {activeTab === "good-answers" && <GoodAnswerTab />}
          {activeTab === "prompts" && <PromptVersionTab />}
        </div>
      </div>
    </div>
  );
}
