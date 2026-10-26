"use client";

import { useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, Star } from "lucide-react";
import { cn } from "@/lib/utils";
import { GuideSection } from "@/features/guide/components/GuideSection";
import { Button } from "@/components/ui/button";
import { REVIEW_GROUPS } from "@/features/guide/data";

function Rating({ value }: { value: number }) {
  return (
    <span className="inline-flex items-center gap-0.5" aria-label={`5점 만점에 ${value}점`}>
      {Array.from({ length: 5 }, (_, i) => (
        <Star
          key={i}
          aria-hidden
          className={cn("size-3.5", i < value ? "fill-highlight-foreground text-highlight-foreground" : "text-border")}
        />
      ))}
    </span>
  );
}

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
    <GuideSection id="reviews" title="오픽 강의실 별 지원자들 후기" subtitle="시험장 후기 스냅샷">
      <div className="flex flex-wrap gap-2">
        {REVIEW_GROUPS.map((room) => {
          const isActive = activeRoomId === room.id;
          return (
            <button
              key={room.id}
              type="button"
              aria-pressed={isActive}
              onClick={() => {
                setActiveRoomId(room.id);
                setPage(1);
              }}
              className={cn(
                "rounded-full border px-4 py-1.5 text-sm font-medium transition-colors",
                isActive
                  ? "border-primary bg-primary text-primary-foreground"
                  : "bg-card text-muted-foreground hover:border-primary/30 hover:text-foreground"
              )}
            >
              {room.classroom}
            </button>
          );
        })}
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-2">
        {pageReviews.map((review) => (
          <figure key={review.id} className="flex flex-col gap-3 rounded-2xl border bg-secondary p-4">
            <div className="flex items-center justify-between gap-3">
              <figcaption className="text-sm font-semibold">{activeRoom?.classroom}</figcaption>
              <Rating value={Math.min(5, Math.max(0, review.rating))} />
            </div>
            <blockquote className="text-sm leading-6 text-muted-foreground">{review.quote}</blockquote>
          </figure>
        ))}
      </div>

      <div className="mt-4 flex items-center justify-between">
        <span className="text-sm text-muted-foreground">
          {page} / {totalPages}
        </span>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="icon-lg"
            className="rounded-full"
            aria-label="이전"
            onClick={() => setPage((p) => Math.max(1, p - 1))}
            disabled={page === 1}
          >
            <ChevronLeft />
          </Button>
          <Button
            variant="outline"
            size="icon-lg"
            className="rounded-full"
            aria-label="다음"
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
            disabled={page === totalPages}
          >
            <ChevronRight />
          </Button>
        </div>
      </div>
    </GuideSection>
  );
}
