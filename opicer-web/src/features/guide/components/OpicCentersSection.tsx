"use client";

import { useEffect, useMemo, useState } from "react";
import { ExternalLink, MapPin, Phone, Star, TrainFront } from "lucide-react";
import { cn } from "@/lib/utils";
import { GuideSection } from "@/features/guide/components/GuideSection";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchOpicCenterReviews, fetchOpicCenters } from "@/features/opic/api";
import type { OpicCenter, OpicCenterReview } from "@/features/opic/types";
import { formatShortDate } from "@/features/practice/history-format";

const ALL_REGIONS = "전체";

type Loadable<T> = { status: "loading" } | { status: "error" } | { status: "ready"; data: T };

function Rating({ value }: { value: number }) {
  return (
    <span className="inline-flex items-center gap-0.5" role="img" aria-label={`5점 만점에 ${value}점`}>
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

function RatingSummary({ center }: { center: OpicCenter }) {
  if (center.reviewCount === 0 || center.averageRating === null) {
    return <span className="text-xs text-muted-foreground">후기 없음</span>;
  }
  return (
    <span className="inline-flex items-center gap-1 text-xs font-medium text-highlight-foreground">
      <Star aria-hidden className="size-3.5 fill-current" />
      {center.averageRating.toFixed(1)}
      <span className="text-muted-foreground">({center.reviewCount})</span>
    </span>
  );
}

/** Mounted with `key={center.id}`, so each center starts from a fresh loading state. */
function CenterDetail({ center }: { center: OpicCenter }) {
  const [reviews, setReviews] = useState<Loadable<OpicCenterReview[]>>({ status: "loading" });

  useEffect(() => {
    let alive = true;
    fetchOpicCenterReviews(center.id)
      .then((data) => alive && setReviews({ status: "ready", data }))
      .catch(() => alive && setReviews({ status: "error" }));
    return () => {
      alive = false;
    };
  }, [center.id]);

  return (
    <div className="flex min-w-0 flex-col gap-4 rounded-2xl bg-secondary p-4 sm:p-5">
      <div className="flex flex-col gap-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-lg font-bold">{center.name}</h3>
          <RatingSummary center={center} />
        </div>
        <ul className="flex flex-col gap-1.5 text-sm text-muted-foreground">
          <li className="flex gap-2">
            <MapPin aria-hidden className="mt-0.5 size-4 shrink-0" />
            <span>{center.address}</span>
          </li>
          {center.subwayLines && (
            <li className="flex gap-2">
              <TrainFront aria-hidden className="mt-0.5 size-4 shrink-0" />
              <span>{center.subwayLines}</span>
            </li>
          )}
          {center.phone && (
            <li className="flex gap-2">
              <Phone aria-hidden className="mt-0.5 size-4 shrink-0" />
              <a href={`tel:${center.phone}`} className="hover:text-foreground hover:underline">
                {center.phone}
              </a>
            </li>
          )}
        </ul>
        {center.directions && (
          <p className="line-clamp-4 text-xs leading-5 whitespace-pre-line text-muted-foreground">{center.directions}</p>
        )}
        <div>
          <Button asChild variant="outline" size="sm" className="rounded-full">
            <a
              href={`https://map.naver.com/p/search/${encodeURIComponent(center.address)}`}
              target="_blank"
              rel="noreferrer"
            >
              지도에서 보기
              <ExternalLink />
            </a>
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-2 border-t pt-4">
        <p className="text-sm font-semibold">응시자 후기</p>
        {reviews.status === "loading" ? (
          <Skeleton className="h-20 rounded-xl" />
        ) : reviews.status === "error" ? (
          <p className="text-sm text-muted-foreground">후기를 불러오지 못했어요.</p>
        ) : reviews.data.length === 0 ? (
          <p className="text-sm text-muted-foreground">아직 등록된 후기가 없어요.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {reviews.data.map((review) => (
              <li key={review.id}>
                <figure className="flex flex-col gap-2 rounded-xl bg-card p-3.5">
                  <Rating value={Math.min(5, Math.max(0, review.rating))} />
                  <blockquote className="text-sm leading-6 whitespace-pre-line">{review.content}</blockquote>
                  <figcaption className="text-xs text-muted-foreground">
                    {[review.sourceLabel, formatShortDate(review.createdAt)].filter(Boolean).join(" · ")}
                  </figcaption>
                </figure>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export function OpicCentersSection() {
  const [centers, setCenters] = useState<Loadable<OpicCenter[]>>({ status: "loading" });
  const [region, setRegion] = useState(ALL_REGIONS);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    fetchOpicCenters()
      .then((data) => alive && setCenters({ status: "ready", data }))
      .catch(() => alive && setCenters({ status: "error" }));
    return () => {
      alive = false;
    };
  }, []);

  const all = useMemo(() => (centers.status === "ready" ? centers.data : []), [centers]);
  const regions = useMemo(
    () => [ALL_REGIONS, ...Array.from(new Set(all.map((c) => c.regionName ?? "기타")))],
    [all]
  );
  const visible = region === ALL_REGIONS ? all : all.filter((c) => (c.regionName ?? "기타") === region);
  const selected = visible.find((c) => c.id === selectedId) ?? visible[0] ?? null;

  return (
    <GuideSection id="centers" title="시험센터 & 후기" subtitle="가까운 시험센터와 응시자 후기를 확인하세요">
      {centers.status === "loading" ? (
        <div className="grid gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
          <Skeleton className="h-80 rounded-2xl" />
          <Skeleton className="h-80 rounded-2xl" />
        </div>
      ) : centers.status === "error" ? (
        <p className="rounded-2xl bg-secondary px-4 py-6 text-sm text-muted-foreground">
          시험센터 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
        </p>
      ) : all.length === 0 ? (
        <p className="rounded-2xl bg-secondary px-4 py-6 text-sm text-muted-foreground">
          아직 시험센터 정보가 없어요. opic.or.kr에서 정보를 가져오는 중이에요.
        </p>
      ) : (
        <div className="flex flex-col gap-4">
          <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 [scrollbar-width:none]">
            {regions.map((name) => (
              <button
                key={name}
                type="button"
                aria-pressed={region === name}
                onClick={() => {
                  setRegion(name);
                  setSelectedId(null);
                }}
                className={cn(
                  "shrink-0 rounded-full border px-4 py-1.5 text-sm font-medium transition-colors",
                  region === name
                    ? "border-primary bg-primary text-primary-foreground"
                    : "bg-card text-muted-foreground hover:border-primary/30 hover:text-foreground"
                )}
              >
                {name}
              </button>
            ))}
          </div>

          <div className="grid gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
            <ul className="flex max-h-[32rem] flex-col gap-2 overflow-y-auto pr-1" aria-label="시험센터 목록">
              {visible.map((center) => {
                const isActive = center.id === selected?.id;
                return (
                  <li key={center.id}>
                    <button
                      type="button"
                      aria-pressed={isActive}
                      onClick={() => setSelectedId(center.id)}
                      className={cn(
                        "flex w-full flex-col gap-1 rounded-2xl border px-4 py-3 text-left transition-colors",
                        isActive ? "border-primary bg-accent" : "bg-card hover:border-primary/40"
                      )}
                    >
                      <span className="flex items-center justify-between gap-2">
                        <span className="text-sm font-semibold">{center.name}</span>
                        <RatingSummary center={center} />
                      </span>
                      <span className="truncate text-xs text-muted-foreground">{center.address}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
            {selected && <CenterDetail key={selected.id} center={selected} />}
          </div>

          <p className="text-xs text-muted-foreground">
            센터 정보 출처: opic.or.kr · 후기는 운영자가 수집하고 검토해 등록해요.
          </p>
        </div>
      )}
    </GuideSection>
  );
}
