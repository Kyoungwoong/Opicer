import type { User } from "@/types/auth";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Eyebrow } from "@/components/common/SectionHeader";
import { UniversalSentencesSection } from "@/features/home/components/UniversalSentencesSection";
import { RecentActivitySection } from "@/features/home/components/RecentActivitySection";
import { OpicScheduleSection } from "@/features/home/components/OpicScheduleSection";
import { ko } from "@/locales/ko";

export function HomeView({
  user,
  onLogout,
}: {
  user: User;
  onLogout: () => void;
}) {
  return (
    <PageShell
      width="lg"
      className="gap-6"
      nav={
        <TopNav
          userLabel={user.name ?? user.email ?? ko.common.userFallback}
          onLogout={onLogout}
          maxWidthClassName="max-w-5xl"
        />
      }
    >
      <div className="flex flex-col gap-2 pt-2">
        <Eyebrow latin>Welcome back</Eyebrow>
        <h1 className="text-2xl font-bold leading-tight sm:text-3xl">
          {ko.common.homeGreeting.headline.replace(
            "{name}",
            user.name?.split(" ")[0] ?? ko.common.homeGreeting.fallback
          )}
        </h1>
        <p className="text-base text-muted-foreground">{ko.common.homeGreeting.subHeadline}</p>
      </div>

      <UniversalSentencesSection />

      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
        <RecentActivitySection />
        <OpicScheduleSection />
      </div>
    </PageShell>
  );
}
