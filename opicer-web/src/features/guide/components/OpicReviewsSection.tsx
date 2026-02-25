import { GuideSection } from "@/features/guide/components/GuideSection";
import { REVIEW_ITEMS } from "@/features/guide/data";

export function OpicReviewsSection() {
  return (
    <GuideSection
      id="reviews"
      title="오픽 강의실 별 지원자들 후기"
      subtitle="시험장 후기 스냅샷"
    >
      <div className="grid gap-4 md:grid-cols-2">
        {REVIEW_ITEMS.map((review) => (
          <div
            key={review.id}
            className="rounded-2xl border border-black/5 bg-white/80 p-4"
          >
            <div className="flex items-center justify-between gap-3">
              <p className="text-sm font-semibold">{review.classroom}</p>
              <span className="text-xs text-[var(--muted)]">
                {"★".repeat(review.rating)}
              </span>
            </div>
            <p className="mt-3 text-sm text-[var(--muted)]">
              {review.quote}
            </p>
          </div>
        ))}
      </div>
    </GuideSection>
  );
}
