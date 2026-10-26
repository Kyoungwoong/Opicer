"use client";

import { useEffect, useMemo, useState } from "react";
import { opicCenterReviewApi, opicSyncApi } from "./api";
import type { AdminOpicCenterReview, OpicSyncResult } from "./types";
import { adminUi } from "@/features/admin/ui";
import { fetchOpicCenters } from "@/features/opic/api";
import type { OpicCenter } from "@/features/opic/types";

const RATINGS = [5, 4, 3, 2, 1];

const emptyForm = {
  rating: 5,
  content: "",
  sourceLabel: "",
  visible: true,
};

function syncSummary(result: OpicSyncResult): string {
  return [
    `일정 ${result.schedulesUpserted ?? 0}건 반영, ${result.schedulesRemoved ?? 0}건 삭제`,
    `센터 ${result.centersUpserted ?? 0}곳 반영, ${result.centersDeactivated ?? 0}곳 비활성화`,
  ].join(" · ");
}

export function OpicCenterReviewTab() {
  const [centers, setCenters] = useState<OpicCenter[]>([]);
  const [centerId, setCenterId] = useState("");
  const [reviews, setReviews] = useState<AdminOpicCenterReview[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadCenters();
  }, []);

  useEffect(() => {
    if (centerId) loadReviews(centerId);
    else setReviews([]);
    cancelEdit();
  }, [centerId]);

  const centersByRegion = useMemo(() => {
    const groups = new Map<string, OpicCenter[]>();
    for (const center of centers) {
      const region = center.regionName ?? "기타";
      groups.set(region, [...(groups.get(region) ?? []), center]);
    }
    return Array.from(groups.entries());
  }, [centers]);

  async function loadCenters() {
    try {
      const data = await fetchOpicCenters();
      setCenters(data);
      setCenterId((current) => (data.some((c) => c.id === current) ? current : data[0]?.id ?? ""));
    } catch {
      setError("Failed to load test centers.");
    }
  }

  async function loadReviews(id: string) {
    try {
      setReviews(await opicCenterReviewApi.list(id));
    } catch {
      setError("Failed to load reviews.");
    }
  }

  async function handleSync() {
    if (!confirm("opic.or.kr에서 일정과 시험센터를 다시 가져올까요? 수십 초 걸릴 수 있어요.")) return;
    setSyncing(true);
    setSyncMessage(null);
    setError(null);
    try {
      const result = await opicSyncApi.run("all");
      setSyncMessage(syncSummary(result));
      await loadCenters();
    } catch (err) {
      setError(String(err));
    } finally {
      setSyncing(false);
    }
  }

  function startEdit(review: AdminOpicCenterReview) {
    setEditId(review.id);
    setForm({
      rating: review.rating,
      content: review.content,
      sourceLabel: review.sourceLabel ?? "",
      visible: review.visible,
    });
  }

  function cancelEdit() {
    setEditId(null);
    setForm(emptyForm);
    setError(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!centerId) return;
    setLoading(true);
    setError(null);
    const payload = {
      rating: form.rating,
      content: form.content,
      sourceLabel: form.sourceLabel || undefined,
      visible: form.visible,
    };
    try {
      if (editId) {
        await opicCenterReviewApi.update(editId, payload);
      } else {
        await opicCenterReviewApi.create(centerId, payload);
      }
      cancelEdit();
      await loadReviews(centerId);
    } catch (err) {
      setError(String(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
      await opicCenterReviewApi.delete(id);
      await loadReviews(centerId);
    } catch (err) {
      setError(String(err));
    }
  }

  return (
    <div className={adminUi.section}>
      {/* Sync */}
      <div className={adminUi.card}>
        <h3 className={adminUi.cardTitle}>opic.or.kr 동기화</h3>
        <p className="text-sm text-muted-foreground">
          시험 일정(이번 달부터 2개월 뒤까지)과 시험센터 목록을 다시 가져옵니다. 운영 환경에서는 멀티캠퍼스 허가 후에
          사용하세요.
        </p>
        {syncMessage && <p className="text-sm text-primary-strong">{syncMessage}</p>}
        <div className={adminUi.actions}>
          <button type="button" onClick={handleSync} disabled={syncing} className={adminUi.secondaryButton}>
            {syncing ? "동기화 중..." : "지금 동기화"}
          </button>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className={adminUi.card}>
        <h3 className={adminUi.cardTitle}>{editId ? "후기 수정" : "후기 추가"}</h3>
        {error && <p className={adminUi.error}>{error}</p>}
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>시험센터</span>
          <select
            value={centerId}
            onChange={(e) => setCenterId(e.target.value)}
            className={adminUi.control}
            disabled={centers.length === 0}
          >
            {centers.length === 0 && <option value="">센터 없음 — 먼저 동기화하세요</option>}
            {centersByRegion.map(([region, items]) => (
              <optgroup key={region} label={region}>
                {items.map((center) => (
                  <option key={center.id} value={center.id}>
                    {center.name}
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
        </label>
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>평점</span>
            <select
              value={form.rating}
              onChange={(e) => setForm((f) => ({ ...f, rating: Number(e.target.value) }))}
              className={adminUi.control}
            >
              {RATINGS.map((r) => (
                <option key={r} value={r}>
                  {"★".repeat(r)} ({r})
                </option>
              ))}
            </select>
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>출처 (선택)</span>
            <input
              value={form.sourceLabel}
              maxLength={100}
              onChange={(e) => setForm((f) => ({ ...f, sourceLabel: e.target.value }))}
              className={adminUi.control}
              placeholder="예: 응시자 제보"
            />
          </label>
        </div>
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>후기</span>
          <textarea
            required
            rows={3}
            maxLength={2000}
            value={form.content}
            onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
            className={adminUi.control}
            placeholder="시험실 분위기, 소음, 장비, 대기 공간 등"
          />
        </label>
        <label className={adminUi.checkRow}>
          <input
            type="checkbox"
            checked={form.visible}
            onChange={(e) => setForm((f) => ({ ...f, visible: e.target.checked }))}
            className={adminUi.checkbox}
          />
          <span className="text-muted-foreground">공개 (가이드 페이지에 표시)</span>
        </label>
        <div className={adminUi.actions}>
          <button type="submit" disabled={loading || !centerId} className={adminUi.primaryButton}>
            {loading ? "저장 중..." : editId ? "수정" : "추가"}
          </button>
          {editId && (
            <button type="button" onClick={cancelEdit} className={adminUi.secondaryButton}>
              취소
            </button>
          )}
        </div>
      </form>

      {/* Table */}
      <div className={adminUi.tableWrap}>
        <table className={adminUi.table}>
          <thead className={adminUi.thead}>
            <tr>
              <th className={adminUi.th}>평점</th>
              <th className={adminUi.th}>후기</th>
              <th className={adminUi.th}>출처</th>
              <th className={adminUi.th}>공개</th>
              <th className={adminUi.th}>작성일</th>
              <th className={adminUi.th}>Actions</th>
            </tr>
          </thead>
          <tbody className={adminUi.tbody}>
            {reviews.length === 0 && (
              <tr>
                <td colSpan={6} className={adminUi.tdEmpty}>
                  등록된 후기가 없습니다.
                </td>
              </tr>
            )}
            {reviews.map((review) => (
              <tr key={review.id} className={adminUi.tr}>
                <td className={adminUi.tdStrong}>{review.rating}</td>
                <td className={adminUi.td}>
                  <span className="line-clamp-2 max-w-md">{review.content}</span>
                </td>
                <td className={adminUi.tdMuted}>{review.sourceLabel ?? "-"}</td>
                <td className={adminUi.td}>
                  <span className={review.visible ? adminUi.pillOn : adminUi.pillOff}>
                    {review.visible ? "공개" : "숨김"}
                  </span>
                </td>
                <td className={adminUi.tdSmall}>{review.createdAt.slice(0, 10)}</td>
                <td className={adminUi.td}>
                  <div className="flex gap-3">
                    <button type="button" onClick={() => startEdit(review)} className={adminUi.linkButton}>
                      수정
                    </button>
                    <button type="button" onClick={() => handleDelete(review.id)} className={adminUi.dangerLink}>
                      삭제
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
