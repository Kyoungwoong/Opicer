"use client";

import { useMemo, useState } from "react";
import { GuideSection } from "@/features/guide/components/GuideSection";
import { REVIEW_GROUPS } from "@/features/guide/data";

export function OpicReviewsSection() {
  const [activeRoomId, setActiveRoomId] = useState(REVIEW_GROUPS[0]?.id ?? "");
  const [page, setPage] = useState(1);
  const perPage = 2;

  const activeRoom = useMemo(
    () => REVIEW_GROUPS.find((room) => room.id === activeRoomId),
    [activeRoomId]
  );

  const totalPages = activeRoom
    ? Math.max(1, Math.ceil(activeRoom.reviews.length / perPage))
    : 1;

  const pageReviews = activeRoom
    ? activeRoom.reviews.slice((page - 1) * perPage, page * perPage)
    : [];

  return (
    <GuideSection
      id="reviews"
      title="오픽 강의실 별 지원자들 후기"
      subtitle="시험장 후기 스냅샷"
    >
      <div className="flex flex-wrap gap-2">
        {REVIEW_GROUPS.map((room) => (
          <button
            key={room.id}
            type="button"
            onClick={() => {
              setActiveRoomId(room.id);
              setPage(1);
            }}
            className={`rounded-full border px-4 py-2 text-xs font-semibold transition ${
              activeRoomId === room.id
                ? "border-primary/40 bg-white text-primary-strong"
                : "border-black/5 bg-white/70 text-muted-foreground hover:border-primary/20 hover:text-primary-strong"
            }`}
          >
            {room.classroom}
          </button>
        ))}
      </div>

      <div className="mt-4 grid gap-4 md:grid-cols-2">
        {pageReviews.map((review) => {
          const safeRating = Math.min(5, Math.max(0, review.rating));
          return (
            <div
              key={review.id}
              className="rounded-2xl border border-black/5 bg-white/80 p-4"
            >
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-semibold">{activeRoom?.classroom}</p>
                <span className="text-xs text-muted-foreground">
                  {"★".repeat(safeRating)}
                </span>
              </div>
              <p className="mt-3 text-sm text-muted-foreground">
                {review.quote}
              </p>
            </div>
          );
        })}
      </div>

      <div className="mt-4 flex items-center justify-between">
        <span className="text-xs text-muted-foreground">
          {page} / {totalPages}
        </span>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(1, p - 1))}
            className="rounded-full border border-black/10 px-3 py-1 text-xs font-semibold text-muted-foreground disabled:opacity-40"
            disabled={page === 1}
          >
            이전
          </button>
          <button
            type="button"
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
            className="rounded-full border border-black/10 px-3 py-1 text-xs font-semibold text-muted-foreground disabled:opacity-40"
            disabled={page === totalPages}
          >
            다음
          </button>
        </div>
      </div>
    </GuideSection>
  );
}
