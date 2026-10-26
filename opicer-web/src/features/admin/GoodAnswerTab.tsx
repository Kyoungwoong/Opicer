"use client";

import { useEffect, useMemo, useState } from "react";
import { fetchTopics } from "@/features/practice/api";
import type { TopicItem } from "@/features/practice/types";
import { goodAnswerApi } from "./api";
import type { GoodAnswerSample } from "./types";
import { adminUi } from "@/features/admin/ui";
const SUMMARY_OPTIONS = [
  "묘사",
  "과거/현재/비교",
  "육하원칙",
  "의견 제시",
  "이유 설명",
  "상황 대처",
];

type FormState = {
  topicId: string;
  summary: string;
  tags: string;
  keyExpressions: string;
  audio: File[];
};

const emptyForm: FormState = {
  topicId: "",
  summary: "",
  tags: "",
  keyExpressions: "",
  audio: [],
};

export function GoodAnswerTab() {
  const [topics, setTopics] = useState<TopicItem[]>([]);
  const [samples, setSamples] = useState<GoodAnswerSample[]>([]);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);

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
    if (!form.topicId || form.audio.length === 0) {
      setError("주제와 음성 파일을 선택해주세요.");
      return;
    }
    setLoading(true);
    try {
      await goodAnswerApi.upload({
        topicId: form.topicId,
        level: "AL",
        audio: form.audio,
        summary: form.summary || undefined,
        tags: form.tags || undefined,
        keyExpressions: form.keyExpressions || undefined,
      });
      setForm((prev) => ({ ...prev, summary: "", tags: "", keyExpressions: "", audio: [] }));
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
    <div className={adminUi.section}>
      <form
        onSubmit={handleSubmit}
        className={adminUi.card}
      >
        <h3 className={adminUi.cardTitle}>샘플 답변 업로드</h3>
        {error && (
          <p className={adminUi.error}>
            {error}
          </p>
        )}
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>주제</span>
            <select
              value={form.topicId}
              onChange={(e) => setForm((f) => ({ ...f, topicId: e.target.value }))}
              className={adminUi.control}
            >
              {topics.map((topic) => (
                <option key={topic.id} value={topic.id}>
                  {topic.title}
                </option>
              ))}
            </select>
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>문제 유형</span>
            <select
              value={form.summary}
              onChange={(e) => setForm((f) => ({ ...f, summary: e.target.value }))}
              className={adminUi.control}
            >
              <option value="">유형 선택</option>
              {SUMMARY_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className={adminUi.grid2}>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>태그 (쉼표 구분)</span>
            <input
              value={form.tags}
              onChange={(e) => setForm((f) => ({ ...f, tags: e.target.value }))}
              className={adminUi.control}
            />
          </label>
          <label className={adminUi.field}>
            <span className={adminUi.labelText}>핵심 표현 (쉼표 구분)</span>
            <input
              value={form.keyExpressions}
              onChange={(e) =>
                setForm((f) => ({ ...f, keyExpressions: e.target.value }))
              }
              className={adminUi.control}
            />
          </label>
        </div>
        <div className={adminUi.field}>
          <p className={adminUi.labelText}>
            음성 파일 업로드
          </p>
          <label
            className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed p-6 text-sm transition ${
              dragActive
                ? "border-primary bg-primary/10 text-foreground"
                : "border-input bg-secondary text-muted-foreground hover:border-primary"
            }`}
              onDragOver={(e) => {
                e.preventDefault();
                setDragActive(true);
              }}
            onDragLeave={() => setDragActive(false)}
              onDrop={(e) => {
                e.preventDefault();
                setDragActive(false);
                const files = Array.from(e.dataTransfer.files ?? []);
                setForm((f) => ({ ...f, audio: [...f.audio, ...files] }));
              }}
            >
            <input
              type="file"
              accept="audio/*"
              className="hidden"
              multiple
              onChange={(e) => {
                const input = e.target as HTMLInputElement | null;
                const files = Array.from(input?.files ?? []);
                setForm((f) => ({ ...f, audio: [...f.audio, ...files] }));
              }}
            />
            <div className="font-semibold text-foreground">
              {form.audio.length > 0 ? `${form.audio.length}개 파일 선택됨` : "파일을 드래그하거나 클릭"}
            </div>
            <div className="mt-1 text-xs text-muted-foreground">
              {form.audio.length > 0
                ? form.audio
                    .slice(0, 3)
                    .map((file) => `${file.name} (${(file.size / 1024 / 1024).toFixed(2)}MB)`)
                    .join(" · ")
                : "mp3, wav, m4a, webm 파일 지원"}
            </div>
            {form.audio.length > 0 && (
              <button
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  setForm((f) => ({ ...f, audio: [] }));
                }}
                className={`mt-3 ${adminUi.smallButton}`}
              >
                선택 해제
              </button>
            )}
          </label>
          {form.audio.length > 0 && (
            <div className="mt-3 space-y-2">
              {form.audio.map((file, index) => (
                <div
                  key={`${file.name}-${index}`}
                  className="flex items-center justify-between rounded-lg border bg-secondary px-3 py-2 text-xs text-muted-foreground"
                >
                  <span className="truncate">
                    {file.name} · {(file.size / 1024 / 1024).toFixed(2)}MB
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      setForm((f) => ({
                        ...f,
                        audio: f.audio.filter((_, i) => i !== index),
                      }))
                    }
                    className={adminUi.dangerLink}
                  >
                    제거
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className={adminUi.primaryButton}
          >
            {loading ? "업로드 중..." : "업로드"}
          </button>
        </div>
      </form>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className={adminUi.cardTitle}>
            샘플 목록 {topicLabel && `· ${topicLabel}`}
          </h3>
          <span className="text-xs text-muted-foreground">{samples.length}개</span>
        </div>
        <div className="space-y-3">
          {samples.map((sample) => (
            <div
              key={sample.id}
              className={adminUi.subCard}
            >
              <div className="flex items-center justify-between text-sm">
                <div className="font-semibold text-foreground">
                  {sample.level}
                </div>
                <button
                  onClick={() => handleDelete(sample.id)}
                  className={`text-xs ${adminUi.dangerLink}`}
                >
                  삭제
                </button>
              </div>
              <p className="text-sm text-muted-foreground">
                {sample.summary ?? "요약 없음"}
              </p>
              <p className="text-sm text-foreground line-clamp-2">
                {sample.sampleText}
              </p>
              {sample.sampleAudioUrl && (
                <audio controls src={sample.sampleAudioUrl} className="w-full" />
              )}
              <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                {sample.tags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full bg-muted px-2 py-0.5"
                  >
                    #{tag}
                  </span>
                ))}
              </div>
            </div>
          ))}
          {samples.length === 0 && (
            <div className={adminUi.empty}>
              등록된 샘플이 없습니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
