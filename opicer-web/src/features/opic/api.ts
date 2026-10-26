import type { OpicCenter, OpicCenterReview, OpicScheduleMonth } from "@/features/opic/types";

async function getJson<T>(url: string, fallbackMessage: string): Promise<T> {
  const res = await fetch(url, { cache: "no-store" });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? fallbackMessage);
  }
  return (await res.json()) as T;
}

/** `month` is YYYY-MM. */
export function fetchOpicSchedules(month: string): Promise<OpicScheduleMonth> {
  return getJson(`/api/opic/schedules?month=${encodeURIComponent(month)}`, "오픽 일정을 불러오지 못했습니다.");
}

export function fetchOpicCenters(): Promise<OpicCenter[]> {
  return getJson("/api/opic/centers", "시험센터 목록을 불러오지 못했습니다.");
}

export function fetchOpicCenterReviews(centerId: string): Promise<OpicCenterReview[]> {
  return getJson(
    `/api/opic/centers/${encodeURIComponent(centerId)}/reviews`,
    "시험센터 후기를 불러오지 못했습니다."
  );
}
