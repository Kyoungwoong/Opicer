import Link from "next/link";
import type { User } from "@/types/auth";
import { ROUTES } from "@/lib/routes";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Mascot } from "@/components/brand/Mascot";
import { Button } from "@/components/ui/button";
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
  const greeting = ko.common.homeGreeting;
  const firstName = user.name?.split(" ")[0];
  const headline = firstName ? greeting.headline.replace("{name}", firstName) : greeting.fallback;

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
      <section className="flex flex-wrap items-center gap-8 rounded-3xl border bg-card px-6 py-8 shadow-card sm:px-12 sm:py-10">
        <div className="flex min-w-0 flex-[999_1_420px] flex-col gap-4">
          <span className="w-fit rounded-full bg-accent px-3 py-1.5 text-sm font-bold text-accent-foreground">
            {greeting.badge}
          </span>
          <h1 className="text-3xl leading-tight font-extrabold whitespace-pre-line sm:text-[44px]">{headline}</h1>
          <p className="text-base leading-relaxed text-muted-foreground sm:text-[17px]">{greeting.subHeadline}</p>
          <div className="flex flex-wrap gap-3 pt-1">
            <Button asChild className="h-[52px] rounded-full px-7 text-base font-bold">
              <Link href={ROUTES.practice}>{greeting.primaryCta}</Link>
            </Button>
            <Button asChild variant="outline" className="h-[52px] rounded-full bg-card px-6 text-base font-semibold">
              <Link href="/guide">{greeting.secondaryCta}</Link>
            </Button>
          </div>
        </div>
        <div className="mx-auto w-full max-w-[240px] flex-[1_1_220px]">
          <Mascot />
        </div>
      </section>

      <RecentActivitySection />

      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-2">
        <UniversalSentencesSection />
        <OpicScheduleSection />
      </div>
    </PageShell>
  );
}
