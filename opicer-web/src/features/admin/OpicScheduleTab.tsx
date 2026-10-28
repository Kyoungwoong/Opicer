"use client";

import { useEffect, useMemo, useState } from "react";
import { cn } from "@/lib/utils";
import { opicScheduleApi } from "./api";
import type { AdminOpicSchedule, AdminOpicScheduleInput } from "./types";
import { adminUi } from "@/features/admin/ui";
import { parseAdminError } from "@/features/admin/errors";
import { seoulToday } from "@/features/opic/calendar";

type Filter = "upcoming" | "past" | "all";

const FILTERS: { id: Filter; label: string }[] = [
  { id: "upcoming", label: "다가오는 일정" },
  { id: "past", label: "지난 일정" },
  { id: "all", label: "전체" },
];

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

type Form = {
  examDate: string;
  roundName: string;
  registrationStart: string;
  registrationEnd: string;
  scoreReleaseDate: string;
  earlyRelease: boolean;
};

const emptyForm: Form = {
  examDate: "",
  roundName: "",
  registrationStart: "",
  registrationEnd: "",
  scoreReleaseDate: "",
  earlyRelease: false,
};

/** Same order the server enforces: registration start ≤ end ≤ exam date ≤ score release, skipping blanks. */
function dateProblem(form: Form): string | null {
  const after = (a: string, b: string) => a !== "" && b !== "" && a > b;
  if (after(form.registrationStart, form.registrationEnd)) return "접수 시작일이 접수 마감일보다 늦어요.";
  if (after(form.registrationStart, form.examDate) || after(form.registrationEnd, form.examDate)) {
    return "접수는 시험일까지 끝나야 해요.";
  }
  if (after(form.examDate, form.scoreReleaseDate)) return "성적 발표일이 시험일보다 빨라요.";
  return null;
}

function toInput(form: Form): AdminOpicScheduleInput {
  const optional = (value: string) => value || undefined;
  return {
    examDate: form.examDate,
    roundName: form.roundName.trim(),
    registrationStart: optional(form.registrationStart),
    registrationEnd: optional(form.registrationEnd),
    scoreReleaseDate: optional(form.scoreReleaseDate),
    earlyRelease: form.earlyRelease,
  };
}

function toForm(s: AdminOpicSchedule): Form {
  return {
    examDate: s.examDate,
    roundName: s.roundName,
    registrationStart: s.registrationStart ?? "",
    registrationEnd: s.registrationEnd ?? "",
    scoreReleaseDate: s.scoreReleaseDate ?? "",
    earlyRelease: s.earlyRelease,
  };
}

function saveError(err: unknown): string {
  const { code, message } = parseAdminError(err);
  if (code === "OPIC_SCHEDULE_DUPLICATE") return "같은 시험일에 같은 회차명이 이미 있어요.";
  if (code === "OPIC_SCHEDULE_NOT_FOUND") return "이미 삭제된 일정이에요. 목록을 새로 불러왔어요.";
  return message;
}

/** "2026-11-08" → "2026-11-08 (일)" */
function withWeekday(iso: string): string {
  const [y, m, d] = iso.split("-").map(Number);
  return `${iso} (${WEEKDAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]})`;
}

function shortDate(iso: string | null): string {
  return iso ? iso.slice(5).replace("-", "/") : "미정";
}

export function OpicScheduleTab() {
  const [schedules, setSchedules] = useState<AdminOpicSchedule[]>([]);
  const [loadState, setLoadState] = useState<"loading" | "error" | "ready">("loading");
  const [form, setForm] = useState<Form>(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>("upcoming");
  const today = seoulToday();

  useEffect(() => {
    load();
  }, []);

  async function load() {
    try {
      setSchedules(await opicScheduleApi.list());
      setLoadState("ready");
    } catch {
      setLoadState("error");
    }
  }

  const upcomingCount = schedules.filter((s) => s.examDate >= today).length;
  const visible = useMemo(() => {
    if (filter === "upcoming") {
      // The list arrives newest first; upcoming rounds read better soonest first.
      return schedules.filter((s) => s.examDate >= today).reverse();
    }
    if (filter === "past") return schedules.filter((s) => s.examDate < today);
    return schedules;
  }, [schedules, filter, today]);

  function set<K extends keyof Form>(key: K, value: Form[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function startEdit(s: AdminOpicSchedule) {
    setEditId(s.id);
    setForm(toForm(s));
    setError(null);
    setNotice(null);
  }

  /** Prefills a new round from an existing one; the admin then changes the dates. */
  function startCopy(s: AdminOpicSchedule) {
    setEditId(null);
    setForm(toForm(s));
    setError(null);
    setNotice("복사한 값으로 채웠어요. 날짜를 바꾼 뒤 추가하세요.");
  }

  function reset() {
    setEditId(null);
    setForm(emptyForm);
    setError(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const problem = dateProblem(form);
    if (problem) {
      setError(problem);
      return;
    }
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      if (editId) {
        await opicScheduleApi.update(editId, toInput(form));
        setNotice("일정을 수정했어요.");
      } else {
        await opicScheduleApi.create(toInput(form));
        setNotice("일정을 추가했어요.");
      }
      setEditId(null);
      setForm(emptyForm);
      await load();
    } catch (err) {
      setError(saveError(err));
      if (parseAdminError(err).code === "OPIC_SCHEDULE_NOT_FOUND") {
        setEditId(null);
        await load();
      }
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(s: AdminOpicSchedule) {
    if (!confirm(`${s.examDate} ${s.roundName} 일정을 삭제할까요?`)) return;
    setError(null);
    setNotice(null);
    try {
      await opicScheduleApi.delete(s.id);
      if (editId === s.id) reset();
      setNotice("일정을 삭제했어요.");
    } catch (err) {
      setError(saveError(err));
    }
    await load();
  }

  return (
    <div className={adminUi.section}>
      <form onSubmit={handleSubmit} className={adminUi.card}>
        <div className="flex flex-col gap-1">
          <h3 className={adminUi.cardTitle}>{editId ? "시험 일정 수정" : "시험 일정 추가"}</h3>
          <p className="text-sm text-muted-foreground">
            공식 사이트(opic.or.kr)에서 확인한 회차를 옮겨 적어 주세요. 시험일과 회차명만 필수이고, 모르는 날짜는 비워
            두면 사용자 화면에 &quot;미정&quot;으로 보여요.
          </p>
        </div>
        {error && <p className={adminUi.error}>{error}</p>}
        {notice && !error && (
          <p className="rounded-lg bg-success-subtle px-3 py-2 text-sm text-success" role="status">
            {notice}
          </p>
        )}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>시험일 *</span>
            <input
              required
              type="date"
              value={form.examDate}
              onChange={(e) => set("examDate", e.target.value)}
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>회차명 *</span>
            <input
              required
              maxLength={255}
              value={form.roundName}
              onChange={(e) => set("roundName", e.target.value)}
              className={adminUi.control}
              placeholder="예: 정기 평가"
            />
          </label>
        </div>
        <div className={adminUi.grid3}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>접수 시작</span>
            <input
              type="date"
              value={form.registrationStart}
              onChange={(e) => set("registrationStart", e.target.value)}
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>접수 마감</span>
            <input
              type="date"
              value={form.registrationEnd}
              onChange={(e) => set("registrationEnd", e.target.value)}
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>성적 발표</span>
            <input
              type="date"
              value={form.scoreReleaseDate}
              onChange={(e) => set("scoreReleaseDate", e.target.value)}
              className={adminUi.control}
            />
          </label>
        </div>
        <label className={adminUi.checkRow}>
          <input
            type="checkbox"
            checked={form.earlyRelease}
            onChange={(e) => set("earlyRelease", e.target.checked)}
            className={adminUi.checkbox}
          />
          <span className="text-muted-foreground">성적 조기 발표 회차</span>
        </label>
        <div className={adminUi.actions}>
          <button type="submit" disabled={saving} className={adminUi.primaryButton}>
            {saving ? "저장 중..." : editId ? "수정" : "추가"}
          </button>
          {(editId || form !== emptyForm) && (
            <button type="button" onClick={reset} disabled={saving} className={adminUi.secondaryButton}>
              {editId ? "취소" : "비우기"}
            </button>
          )}
        </div>
      </form>

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-1.5" role="group" aria-label="일정 필터">
            {FILTERS.map((f) => (
              <button
                key={f.id}
                type="button"
                aria-pressed={filter === f.id}
                onClick={() => setFilter(f.id)}
                className={cn(
                  "rounded-full px-3 py-1.5 text-sm font-medium transition-colors",
                  filter === f.id
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                )}
              >
                {f.label}
              </button>
            ))}
          </div>
          {loadState === "ready" && (
            <p className="text-sm text-muted-foreground">
              다가오는 회차 {upcomingCount}개 · 전체 {schedules.length}개
            </p>
          )}
        </div>

        {loadState === "ready" && upcomingCount === 0 && (
          <p className="rounded-lg bg-warning-subtle px-3 py-2 text-sm text-warning">
            다가오는 일정이 없어요. 사용자 캘린더에는 공식 사이트 안내만 보여요.
          </p>
        )}

        <div className={adminUi.tableWrap}>
          <table className={adminUi.table}>
            <thead className={adminUi.thead}>
              <tr>
                <th className={adminUi.th}>시험일</th>
                <th className={adminUi.th}>회차</th>
                <th className={adminUi.th}>접수 기간</th>
                <th className={adminUi.th}>성적 발표</th>
                <th className={adminUi.th}>출처</th>
                <th className={adminUi.th}>관리</th>
              </tr>
            </thead>
            <tbody className={adminUi.tbody}>
              {loadState === "loading" && (
                <tr>
                  <td colSpan={6} className={adminUi.tdEmpty}>
                    불러오는 중...
                  </td>
                </tr>
              )}
              {loadState === "error" && (
                <tr>
                  <td colSpan={6} className={adminUi.tdEmpty}>
                    일정을 불러오지 못했어요.{" "}
                    <button type="button" onClick={load} className={adminUi.linkButton}>
                      다시 시도
                    </button>
                  </td>
                </tr>
              )}
              {loadState === "ready" && visible.length === 0 && (
                <tr>
                  <td colSpan={6} className={adminUi.tdEmpty}>
                    {filter === "upcoming" ? "다가오는 일정이 없어요." : "등록된 일정이 없어요."}
                  </td>
                </tr>
              )}
              {loadState === "ready" &&
                visible.map((s) => (
                  <tr key={s.id} className={cn(adminUi.tr, editId === s.id && "bg-accent/40")}>
                    <td className={cn(adminUi.tdStrong, "whitespace-nowrap")}>{withWeekday(s.examDate)}</td>
                    <td className={adminUi.td}>
                      <span className="flex flex-wrap items-center gap-1.5">
                        {s.roundName}
                        {s.earlyRelease && <span className={adminUi.pillHighlight}>조기 발표</span>}
                      </span>
                    </td>
                    <td className={cn(adminUi.tdMuted, "whitespace-nowrap")}>
                      {s.registrationStart || s.registrationEnd
                        ? `${shortDate(s.registrationStart)} ~ ${shortDate(s.registrationEnd)}`
                        : "미정"}
                    </td>
                    <td className={cn(adminUi.tdMuted, "whitespace-nowrap")}>{shortDate(s.scoreReleaseDate)}</td>
                    <td className={adminUi.td}>
                      <span className={s.source === "manual" ? adminUi.pillOff : adminUi.pillPrimary}>
                        {s.source === "manual" ? "직접 입력" : s.source}
                      </span>
                    </td>
                    <td className={adminUi.td}>
                      <div className={cn(adminUi.actions, "flex-nowrap")}>
                        <button type="button" onClick={() => startEdit(s)} className={adminUi.linkButton}>
                          수정
                        </button>
                        <button type="button" onClick={() => startCopy(s)} className={adminUi.linkButton}>
                          복사
                        </button>
                        <button type="button" onClick={() => handleDelete(s)} className={adminUi.dangerLink}>
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
    </div>
  );
}
