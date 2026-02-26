"use client";

import { useEffect, useMemo, useState } from "react";
import { fetchTopics } from "@/features/practice/api";
import type { TopicItem } from "@/features/practice/types";
import { goodAnswerApi } from "./api";
import type { GoodAnswerSample, OpicLevel } from "./types";

const OPIC_LEVELS: OpicLevel[] = ["NL", "IL", "IM", "IH", "AL"];

type FormState = {
  topicId: string;
  level: OpicLevel;
  summary: string;
  tags: string;
  keyExpressions: string;
  audio: File | null;
};

const emptyForm: FormState = {
  topicId: "",
  level: "IM",
  summary: "",
  tags: "",
  keyExpressions: "",
  audio: null,
};

export function GoodAnswerTab() {
  const [topics, setTopics] = useState<TopicItem[]>([]);
  const [samples, setSamples] = useState<GoodAnswerSample[]>([]);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    fetchTopics()
      .then((data) => {
        if (!mounted) return;
        setTopics(data);
        if (data.length > 0) {
          setForm((prev) => ({ ...prev, topicId: data[0].id }));
        }
      })
      .catch(() => setError("주제 목록을 불러오는 중 문제가 발생했습니다."));
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    if (!form.topicId) return;
    loadSamples(form.topicId);
  }, [form.topicId]);

  const topicLabel = useMemo(
    () => topics.find((t) => t.id === form.topicId)?.title ?? "",
    [topics, form.topicId]
  );

  async function loadSamples(topicId: string) {
    try {
      const data = await goodAnswerApi.list(topicId);
      setSamples(data);
    } catch (err) {
      setError(String(err));
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.topicId || !form.audio) {
      setError("주제와 음성 파일을 선택해주세요.");
      return;
    }
    setLoading(true);
    try {
      await goodAnswerApi.upload({
        topicId: form.topicId,
        level: form.level,
        audio: form.audio,
        summary: form.summary || undefined,
        tags: form.tags || undefined,
        keyExpressions: form.keyExpressions || undefined,
      });
      setForm((prev) => ({ ...prev, summary: "", tags: "", keyExpressions: "", audio: null }));
      await loadSamples(form.topicId);
    } catch (err) {
      setError(String(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
      await goodAnswerApi.delete(id);
      await loadSamples(form.topicId);
    } catch (err) {
      setError(String(err));
    }
  }

  return (
    <div className="space-y-6">
      <form
        onSubmit={handleSubmit}
        className="rounded-xl border border-black/8 bg-[var(--card)] p-5 space-y-4"
      >
        <h3 className="font-semibold text-[var(--ink)]">샘플 답변 업로드</h3>
        {error && (
          <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
            {error}
          </p>
        )}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">주제</label>
            <select
              value={form.topicId}
              onChange={(e) => setForm((f) => ({ ...f, topicId: e.target.value }))}
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            >
              {topics.map((topic) => (
                <option key={topic.id} value={topic.id}>
                  {topic.title}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">레벨</label>
            <select
              value={form.level}
              onChange={(e) =>
                setForm((f) => ({ ...f, level: e.target.value as OpicLevel }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            >
              {OPIC_LEVELS.map((level) => (
                <option key={level} value={level}>
                  {level}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-[var(--muted)]">요약 (선택)</label>
          <textarea
            rows={2}
            value={form.summary}
            onChange={(e) => setForm((f) => ({ ...f, summary: e.target.value }))}
            className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            placeholder="샘플 답변 요약"
          />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">태그 (쉼표 구분)</label>
            <input
              value={form.tags}
              onChange={(e) => setForm((f) => ({ ...f, tags: e.target.value }))}
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">핵심 표현 (쉼표 구분)</label>
            <input
              value={form.keyExpressions}
              onChange={(e) =>
                setForm((f) => ({ ...f, keyExpressions: e.target.value }))
              }
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            />
          </div>
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-[var(--muted)]">음성 파일</label>
          <input
            type="file"
            accept="audio/*"
            onChange={(e) =>
              setForm((f) => ({
                ...f,
                audio: e.currentTarget.files?.[0] ?? null,
              }))
            }
            className="block w-full text-sm"
          />
        </div>
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className="rounded-full bg-[var(--accent-strong)] px-4 py-2 text-sm text-white transition hover:opacity-90 disabled:opacity-50"
          >
            {loading ? "업로드 중..." : "업로드"}
          </button>
        </div>
      </form>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-[var(--ink)]">
            샘플 목록 {topicLabel && `· ${topicLabel}`}
          </h3>
          <span className="text-xs text-[var(--muted)]">{samples.length}개</span>
        </div>
        <div className="space-y-3">
          {samples.map((sample) => (
            <div
              key={sample.id}
              className="rounded-xl border border-black/8 bg-white/60 p-4 space-y-2"
            >
              <div className="flex items-center justify-between text-sm">
                <div className="font-semibold text-[var(--ink)]">
                  {sample.level}
                </div>
                <button
                  onClick={() => handleDelete(sample.id)}
                  className="text-xs text-red-600 hover:underline"
                >
                  삭제
                </button>
              </div>
              <p className="text-sm text-[var(--muted)]">
                {sample.summary ?? "요약 없음"}
              </p>
              <p className="text-sm text-[var(--ink)] line-clamp-2">
                {sample.sampleText}
              </p>
              {sample.sampleAudioUrl && (
                <audio controls src={sample.sampleAudioUrl} className="w-full" />
              )}
              <div className="flex flex-wrap gap-2 text-xs text-[var(--muted)]">
                {sample.tags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full bg-black/5 px-2 py-0.5"
                  >
                    #{tag}
                  </span>
                ))}
              </div>
            </div>
          ))}
          {samples.length === 0 && (
            <div className="rounded-xl border border-dashed border-black/10 p-6 text-sm text-[var(--muted)]">
              등록된 샘플이 없습니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
