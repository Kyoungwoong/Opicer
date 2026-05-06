"use client";

import { useEffect, useMemo, useState } from "react";
import { fetchTopics } from "@/features/practice/api";
import type { TopicItem } from "@/features/practice/types";
import { goodAnswerApi } from "./api";
import type { GoodAnswerSample } from "./types";
const SUMMARY_OPTIONS = [
  "text",
  "text/text/text",
  "text",
  "text text",
  "text text",
  "text text",
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
      .catch(() => setError("Topic listtext text text text text."));
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
      setError("Topictext text text Selecttext.");
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
    if (!confirm("Are you sure to delete?")) return;
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
        <h3 className="font-semibold text-[var(--ink)]">Sample Answers Upload</h3>
        {error && (
          <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
            {error}
          </p>
        )}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">Topic</label>
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
            <label className="text-xs font-medium text-[var(--muted)]">Question type</label>
            <select
              value={form.summary}
              onChange={(e) => setForm((f) => ({ ...f, summary: e.target.value }))}
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            >
              <option value="">text Select</option>
              {SUMMARY_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">text (text textmin)</label>
            <input
              value={form.tags}
              onChange={(e) => setForm((f) => ({ ...f, tags: e.target.value }))}
              className="w-full rounded-lg border border-black/10 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--muted)]">text text (text textmin)</label>
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
          <label className="text-xs font-medium text-[var(--muted)]">
            Upload audio files
          </label>
          <label
            className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed p-6 text-sm transition ${
              dragActive
                ? "border-[var(--accent)] bg-[var(--accent)]/10 text-[var(--ink)]"
                : "border-black/10 bg-white/60 text-[var(--muted)] hover:border-[var(--accent)]"
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
            <div className="text-[var(--ink)] font-semibold">
              {form.audio.length > 0 ? `${form.audio.length}items text Selected` : "Drag files or click"}
            </div>
            <div className="mt-1 text-xs text-[var(--muted)]">
              {form.audio.length > 0
                ? form.audio
                    .slice(0, 3)
                    .map((file) => `${file.name} (${(file.size / 1024 / 1024).toFixed(2)}MB)`)
                    .join(" text ")
                : "mp3, wav, m4a, webm supported"}
            </div>
            {form.audio.length > 0 && (
              <button
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  setForm((f) => ({ ...f, audio: [] }));
                }}
                className="mt-3 rounded-full border border-black/10 px-3 py-1 text-xs text-[var(--muted)] hover:bg-black/5"
              >
                Select text
              </button>
            )}
          </label>
          {form.audio.length > 0 && (
            <div className="mt-3 space-y-2">
              {form.audio.map((file, index) => (
                <div
                  key={`${file.name}-${index}`}
                  className="flex items-center justify-between rounded-lg border border-black/10 bg-white/70 px-3 py-2 text-xs text-[var(--muted)]"
                >
                  <span className="truncate">
                    {file.name} text {(file.size / 1024 / 1024).toFixed(2)}MB
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      setForm((f) => ({
                        ...f,
                        audio: f.audio.filter((_, i) => i !== index),
                      }))
                    }
                    className="text-red-600 hover:underline"
                  >
                    Remove
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
            className="rounded-full bg-[var(--accent-strong)] px-4 py-2 text-sm text-white transition hover:opacity-90 disabled:opacity-50"
          >
            {loading ? "Uploading..." : "Upload"}
          </button>
        </div>
      </form>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-[var(--ink)]">
            Sample list {topicLabel && `text ${topicLabel}`}
          </h3>
          <span className="text-xs text-[var(--muted)]">{samples.length}items</span>
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
                  Delete
                </button>
              </div>
              <p className="text-sm text-[var(--muted)]">
                {sample.summary ?? "No summary"}
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
              No samples yet.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
