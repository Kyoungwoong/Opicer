import { GuideHeader } from "@/features/guide/components/GuideHeader";
import { GuideNav } from "@/features/guide/components/GuideNav";
import { OpicIntroSection } from "@/features/guide/components/OpicIntroSection";
import { OpicScheduleSection } from "@/features/guide/components/OpicScheduleSection";
import { OpicReviewsSection } from "@/features/guide/components/OpicReviewsSection";
import { OpicTipsSection } from "@/features/guide/components/OpicTipsSection";
import { GUIDE_SECTIONS } from "@/features/guide/data";

export default function GuidePage() {
  return (
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      <main className="mx-auto flex max-w-6xl flex-col gap-8">
        <GuideHeader
          title="오픽 가이드"
          description="시험 구조부터 일정, 후기, 꿀팁까지 한 번에 정리했습니다."
        />

        <div className="grid gap-8 lg:grid-cols-[0.8fr_2.2fr]">
          <GuideNav sections={GUIDE_SECTIONS} />
          <div className="space-y-8">
            <OpicIntroSection />
            <OpicScheduleSection />
            <OpicReviewsSection />
            <OpicTipsSection />
          </div>
        </div>
      </main>
    </div>
  );
}
