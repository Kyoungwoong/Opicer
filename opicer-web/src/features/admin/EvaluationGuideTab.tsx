"use client";

import { useEffect, useMemo, useState } from "react";
import { cn } from "@/lib/utils";
import { evaluationGuideApi } from "./api";
import type { EvaluationGuide, EvaluationGuideKind } from "./types";
import { adminUi } from "@/features/admin/ui";
import { MarkdownPreview } from "@/features/admin/MarkdownPreview";
import { parseAdminError } from "@/features/admin/errors";

/** Mirrors EvaluationGuide.MAX_CONTENT_LENGTH on the server. */
const MAX_CONTENT_LENGTH = 4000;

const KINDS: { kind: EvaluationGuideKind; label: string; use: string; placeholder: string }[] = [
  {
    kind: "SCORING_NOTES",
    label: "채점 보충",
    use: "역량별 레벨을 판단할 때 참고해요. 루브릭을 대신하지 않고 보충해요.",
    placeholder: "## 시제\n- 과거 경험 질문에서 시제 오류가 세 번 넘으면 문법·시제는 IM 이하로 보세요.",
  },
  {
    kind: "TIPS",
    label: "코칭 팁",
    use: "교정 이유와 잘한 점을 쓸 때 반영해요.",
    placeholder: "- 교정 이유에는 바로 따라 할 수 있는 대안 문장을 함께 적어 주세요.",
  },
  {
    kind: "EXPRESSIONS",
    label: "추천 표현",
    use: "추천 표현과 목표 스크립트에 어울리면 우선 써요.",
    placeholder: "- Looking back, …\n- What I liked the most was …",
  },
  {
    kind: "REPORT",
    label: "코멘트 문체",
    use: "모든 한국어 코멘트(역량 코멘트, 교정 이유, 잘한 점, 표현 뜻)의 말투와 길이를 정해요.",
    placeholder: "- 존댓말로, 한 문장에 한 가지만 말해 주세요.",
  },
];

type Editor =
  | { mode: "new"; kind: EvaluationGuideKind; title: string; contentMd: string; basedOn?: number }
  | { mode: "edit"; id: string; kind: EvaluationGuideKind; version: number; title: string; contentMd: string };

const emptyEditor = (kind: EvaluationGuideKind): Editor => ({ mode: "new", kind, title: "", contentMd: "" });

/** The server's message, with a plain explanation for the frozen-active-version conflict. */
function messageOf(err: unknown): string {
  const { code, message } = parseAdminError(err);
  if (code === "EVALUATION_GUIDE_ACTIVE") {
    return "활성 버전은 고치거나 지울 수 없어요. 복제해서 새 버전을 만든 뒤 활성화하세요.";
  }
  return message;
}

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(iso));
}

export function EvaluationGuideTab() {
  const [guides, setGuides] = useState<EvaluationGuide[]>([]);
  const [kind, setKind] = useState<EvaluationGuideKind>("SCORING_NOTES");
  const [editor, setEditor] = useState<Editor>(emptyEditor("SCORING_NOTES"));
  // Title and content as last loaded, to tell whether the editor holds unsaved changes.
  const [baseline, setBaseline] = useState({ title: "", contentMd: "" });
  // Below lg the editor and the preview share one panel.
  const [pane, setPane] = useState<"write" | "preview">("write");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    evaluationGuideApi
      .list()
      .then((data) => alive && setGuides(data))
      .catch((err) => alive && setError(messageOf(err)));
    return () => {
      alive = false;
    };
  }, []);

  async function load() {
    try {
      setGuides(await evaluationGuideApi.list());
    } catch (err) {
      setError(messageOf(err));
    }
  }

  const meta = KINDS.find((k) => k.kind === kind)!;
  const versions = useMemo(
    () => guides.filter((g) => g.kind === kind).sort((a, b) => b.version - a.version),
    [guides, kind]
  );
  const activeVersion = versions.find((g) => g.active);
  const length = editor.contentMd.length;
  const tooLong = length > MAX_CONTENT_LENGTH;
  const dirty = editor.title !== baseline.title || editor.contentMd !== baseline.contentMd;

  /** Loads the editor without asking; for after a save or delete. */
  function resetEditor(next: Editor) {
    setEditor(next);
    setBaseline({ title: next.title, contentMd: next.contentMd });
    setError(null);
  }

  /** Loads the editor, asking first when it holds unsaved changes. Returns false if the admin keeps them. */
  function replaceEditor(next: Editor): boolean {
    if (dirty && !confirm("저장하지 않은 내용이 사라져요. 계속할까요?")) return false;
    resetEditor(next);
    return true;
  }

  function switchKind(next: EvaluationGuideKind) {
    if (next === kind || !replaceEditor(emptyEditor(next))) return;
    setKind(next);
  }

  function duplicate(guide: EvaluationGuide) {
    replaceEditor({ mode: "new", kind: guide.kind, title: guide.title, contentMd: guide.contentMd, basedOn: guide.version });
  }

  function edit(guide: EvaluationGuide) {
    replaceEditor({
      mode: "edit",
      id: guide.id,
      kind: guide.kind,
      version: guide.version,
      title: guide.title,
      contentMd: guide.contentMd,
    });
  }

  async function run(action: () => Promise<unknown>, after?: () => void) {
    setError(null);
    try {
      await action();
      after?.();
      await load();
    } catch (err) {
      setError(messageOf(err));
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (tooLong) return;
    setSaving(true);
    await run(
      () =>
        editor.mode === "edit"
          ? evaluationGuideApi.update(editor.id, { title: editor.title, contentMd: editor.contentMd })
          : evaluationGuideApi.create({ kind: editor.kind, title: editor.title, contentMd: editor.contentMd }),
      () => resetEditor(emptyEditor(kind))
    );
    setSaving(false);
  }

  function handleDelete(guide: EvaluationGuide) {
    if (!confirm(`v${guide.version} "${guide.title}"을(를) 삭제할까요?`)) return;
    run(() => evaluationGuideApi.delete(guide.id), () => {
      if (editor.mode === "edit" && editor.id === guide.id) resetEditor(emptyEditor(kind));
    });
  }

  const editorTitle =
    editor.mode === "edit"
      ? `v${editor.version} 수정`
      : editor.basedOn
        ? `새 버전 (v${editor.basedOn} 복제)`
        : "새 버전";

  return (
    <div className={adminUi.section}>
      {/* Kind switcher */}
      <div className={adminUi.card}>
        <div className="flex flex-wrap gap-2" role="group" aria-label="가이드 종류">
          {KINDS.map((k) => {
            const active = guides.some((g) => g.kind === k.kind && g.active);
            return (
              <button
                key={k.kind}
                type="button"
                aria-pressed={k.kind === kind}
                onClick={() => switchKind(k.kind)}
                className={cn(
                  "inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm font-medium transition-colors",
                  k.kind === kind ? "border-primary bg-accent text-accent-foreground" : "bg-card hover:bg-secondary"
                )}
              >
                {k.label}
                <span
                  aria-hidden
                  className={cn("size-2 rounded-full", active ? "bg-success" : "bg-muted-foreground/30")}
                />
                <span className="sr-only">{active ? "(사용 중)" : "(사용 안 함)"}</span>
              </button>
            );
          })}
        </div>
        <div className="flex flex-col gap-1 text-sm">
          <p>
            <span className="font-semibold">{meta.label}</span> · {meta.use}
          </p>
          <p className="text-muted-foreground">
            {activeVersion
              ? `지금 평가에 쓰이는 버전: v${activeVersion.version} "${activeVersion.title}"`
              : "활성 버전이 없어서 이 종류는 평가에 쓰이지 않아요."}
          </p>
          <p className="text-xs text-muted-foreground">
            활성 버전 하나만 평가에 쓰여요. 활성 버전은 고칠 수 없으니 복제해서 새 버전을 만든 뒤 활성화하세요.
            가이드는 루브릭, 출력 형식, &lsquo;공식 점수가 아닌 예상치&rsquo; 규칙을 바꿀 수 없어요.
          </p>
        </div>
      </div>

      {/* Editor */}
      <form onSubmit={handleSubmit} className={adminUi.card}>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className={adminUi.cardTitle}>
            {meta.label} · {editorTitle}
          </h3>
          <div className="flex gap-1 rounded-full border p-1 lg:hidden" role="group" aria-label="편집 화면">
            {(["write", "preview"] as const).map((p) => (
              <button
                key={p}
                type="button"
                aria-pressed={pane === p}
                onClick={() => setPane(p)}
                className={cn(
                  "rounded-full px-3 py-1 text-xs font-medium",
                  pane === p ? "bg-primary text-primary-foreground" : "text-muted-foreground"
                )}
              >
                {p === "write" ? "작성" : "미리보기"}
              </button>
            ))}
          </div>
        </div>
        {error && <p role="alert" className={adminUi.error}>{error}</p>}
        <label className={adminUi.field}>
          <span className={adminUi.labelText}>제목</span>
          <input
            required
            maxLength={100}
            value={editor.title}
            onChange={(e) => setEditor((ed) => ({ ...ed, title: e.target.value }))}
            className={adminUi.control}
            placeholder="예: 시제 채점 기준 보강"
          />
        </label>
        <div className="grid gap-4 lg:grid-cols-2">
          <label className={cn(adminUi.field, pane === "preview" && "max-lg:hidden")}>
            <span className={adminUi.labelText}>내용 (마크다운)</span>
            <textarea
              required
              rows={16}
              value={editor.contentMd}
              onChange={(e) => setEditor((ed) => ({ ...ed, contentMd: e.target.value }))}
              className={adminUi.controlMono}
              placeholder={meta.placeholder}
              aria-describedby="guide-length"
            />
            <span
              id="guide-length"
              className={cn("self-end text-xs tabular-nums", tooLong ? "text-destructive" : "text-muted-foreground")}
            >
              {length.toLocaleString()} / {MAX_CONTENT_LENGTH.toLocaleString()}자
            </span>
          </label>
          <div className={cn(adminUi.field, pane === "write" && "max-lg:hidden")}>
            <span className={adminUi.labelText}>미리보기</span>
            <div className="min-h-40 rounded-lg border bg-secondary px-4 py-3">
              <MarkdownPreview markdown={editor.contentMd} />
            </div>
          </div>
        </div>
        <div className={adminUi.actions}>
          <button type="submit" disabled={saving || tooLong} className={adminUi.primaryButton}>
            {saving ? "저장 중..." : editor.mode === "edit" ? "수정 저장" : "새 버전으로 저장"}
          </button>
          {(editor.mode === "edit" || editor.basedOn || editor.title || editor.contentMd) && (
            <button type="button" onClick={() => replaceEditor(emptyEditor(kind))} className={adminUi.secondaryButton}>
              취소
            </button>
          )}
        </div>
      </form>

      {/* Versions */}
      {versions.length === 0 ? (
        <p className={adminUi.empty}>아직 {meta.label} 가이드가 없어요.</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {versions.map((guide) => (
            <li key={guide.id} className={adminUi.card}>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex min-w-0 items-center gap-2">
                  <span className="font-mono text-sm text-muted-foreground">v{guide.version}</span>
                  <span className="truncate font-semibold">{guide.title}</span>
                  <span className={guide.active ? adminUi.pillOn : adminUi.pillOff}>
                    {guide.active ? "ACTIVE" : "INACTIVE"}
                  </span>
                </div>
                <span className="text-xs text-muted-foreground">{formatDate(guide.updatedAt)}</span>
              </div>
              <details>
                <summary className="cursor-pointer text-sm text-muted-foreground">
                  내용 보기 ({guide.contentMd.length.toLocaleString()}자)
                </summary>
                <div className="mt-2 rounded-lg border bg-secondary px-4 py-3">
                  <MarkdownPreview markdown={guide.contentMd} />
                </div>
              </details>
              <div className={adminUi.actions}>
                {guide.active ? (
                  <button type="button" onClick={() => run(() => evaluationGuideApi.deactivate(guide.id))} className={adminUi.smallButton}>
                    비활성화
                  </button>
                ) : (
                  <button type="button" onClick={() => run(() => evaluationGuideApi.activate(guide.id))} className={adminUi.smallButton}>
                    활성화
                  </button>
                )}
                <button type="button" onClick={() => duplicate(guide)} className={adminUi.smallButton}>
                  복제해 새 버전
                </button>
                {!guide.active && (
                  <>
                    <button type="button" onClick={() => edit(guide)} className={adminUi.smallButton}>
                      수정
                    </button>
                    <button type="button" onClick={() => handleDelete(guide)} className={adminUi.smallDangerButton}>
                      삭제
                    </button>
                  </>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
