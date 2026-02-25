import type { User } from "@/types/auth";
import { TopNav } from "@/components/common/TopNav";
import { UniversalSentencesSection } from "@/features/home/components/UniversalSentencesSection";
import { RecentActivitySection } from "@/features/home/components/RecentActivitySection";
import { OpicScheduleSection } from "@/features/home/components/OpicScheduleSection";

export function HomeView({
  user,
  onLogout,
}: {
  user: User;
  onLogout: () => void;
}) {
  return (
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      <TopNav
        userLabel={user.name ?? user.email ?? "사용자"}
        onLogout={onLogout}
        maxWidthClassName="max-w-5xl"
      />

      <main className="mx-auto mt-12 flex max-w-5xl flex-col gap-8">
        <div className="flex flex-col gap-2">
          <p className="text-xs uppercase tracking-[0.25em] text-[var(--muted)]">
            Welcome back
          </p>
          <h2 className="text-3xl font-semibold leading-tight">
            안녕하세요, {user.name?.split(" ")[0] ?? "반갑습니다"} 👋
          </h2>
          <p className="text-sm text-[var(--muted)]">
            오늘도 말하기 연습을 시작해볼까요?
          </p>
        </div>

        <UniversalSentencesSection />

        <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <RecentActivitySection />
          <OpicScheduleSection />
        </div>
      </main>
    </div>
  );
}
