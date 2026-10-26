import {
  CATEGORY_LABELS,
  CONFIDENCE_LABELS,
  DIMENSION_LABELS,
  DIMENSION_ORDER,
  LEVEL_DISCLAIMER,
  gapLabel,
} from "@/features/practice/evaluation-labels";
import type { PracticeSessionAnswer, PracticeSessionDetail, SessionReport } from "@/features/practice/types";

/**
 * Self-contained HTML report for saving or printing. A fixed template filled from the stored report and
 * evaluations — no server call and no model tokens. Every interpolated value goes through `esc`.
 */
export function renderReportHtml(session: PracticeSessionDetail, generatedAt: Date = new Date()): string {
  const title = `${session.topicTitle} 연습 리포트`;
  const report = session.report;
  const answers = [...session.answers].sort((a, b) => a.questionOrder - b.questionOrder);

  return `<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)}</title>
<style>${STYLE}</style>
</head>
<body>
<main>
  <header>
    <p class="eyebrow">Opicer 연습 리포트</p>
    <h1>${esc(session.topicTitle)}</h1>
    <p class="muted">${esc(formatDate(session.completedAt))} · ${answers.length}문항${
      session.targetLevel ? ` · 목표 ${esc(session.targetLevel)}` : ""
    }</p>
  </header>
  ${report ? reportSection(report) : `<p class="note">이 연습에는 자동 평가 리포트가 없어요.</p>`}
  ${answers.map((answer, index) => answerSection(answer, index + 1, session.targetLevel)).join("\n")}
  <footer>
    <p>${esc(report?.disclaimer || LEVEL_DISCLAIMER)}. 음성 인식 결과(추정)를 바탕으로 한 연습용 결과예요.</p>
    <p>${esc(formatDate(generatedAt.toISOString()))}에 저장함 · 녹음 파일은 포함되지 않아요.</p>
  </footer>
</main>
</body>
</html>
`;
}

/** "opicer-report-20261026.html" (Seoul date of the session). */
export function reportFileName(session: Pick<PracticeSessionDetail, "completedAt">): string {
  const date = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(new Date(session.completedAt));
  return `opicer-report-${date.replaceAll("-", "")}.html`;
}

/** Saves the report through a temporary object URL. Browser only. */
export function downloadReportHtml(session: PracticeSessionDetail): void {
  const blob = new Blob([renderReportHtml(session)], { type: "text/html;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = reportFileName(session);
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Revoking in the same tick can cancel the download in some browsers.
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function reportSection(report: SessionReport): string {
  const range =
    report.lowestLevel === report.highestLevel ? report.estimatedLevel : `${report.lowestLevel} ~ ${report.highestLevel}`;
  const dimensions = DIMENSION_ORDER.filter((code) => report.dimensionLevels[code])
    .map((code) => `<tr><th>${esc(DIMENSION_LABELS[code])}</th><td>${esc(report.dimensionLevels[code])}</td></tr>`)
    .join("");
  const issues = report.topIssues
    .map(
      (issue) => `<li><strong>${esc(CATEGORY_LABELS[issue.category])}</strong>
        <span class="muted">답변 ${issue.answerCount}개에서 ${issue.totalCount}번</span>
        ${issue.example ? `<div>${fix(issue.example.original, issue.example.corrected)}</div>` : ""}</li>`
    )
    .join("");
  const tips = report.tips.map((tip) => `<li>${esc(tip)}</li>`).join("");

  return `<section class="card">
    <div class="headline">
      <span class="level">${esc(report.estimatedLevel)}</span>
      <div>
        <p class="eyebrow">예상 레벨(참고용)</p>
        <h2>이번 연습은 ${esc(report.estimatedLevel)} 수준으로 보여요</h2>
        <p class="muted">답변별 범위 ${esc(range)} · 평가한 답변 ${report.evaluatedAnswers}개 · ${esc(
          CONFIDENCE_LABELS[report.confidence]
        )} · ${esc(gapLabel(report.gapToTarget, report.targetLevel))}</p>
      </div>
    </div>
    ${dimensions ? `<h3>역량별 예상 레벨</h3><table>${dimensions}</table>` : ""}
    ${issues ? `<h3>자주 나온 실수</h3><ol>${issues}</ol>` : ""}
    ${tips ? `<h3>다음 레벨을 위한 팁</h3><ul>${tips}</ul>` : ""}
  </section>`;
}

function answerSection(answer: PracticeSessionAnswer, number: number, targetLevel: string | null): string {
  const evaluation = answer.evaluation;
  const transcript = answer.transcript
    ? `<p class="transcript">${esc(answer.transcript)}</p>`
    : `<p class="muted">전사된 답변이 없어요.</p>`;

  let body: string;
  if (evaluation) {
    const dimensions = [...evaluation.dimensions]
      .sort((a, b) => DIMENSION_ORDER.indexOf(a.code) - DIMENSION_ORDER.indexOf(b.code))
      .map((d) => `<li><strong>${esc(DIMENSION_LABELS[d.code])} ${esc(d.level)}</strong> — ${esc(d.commentKo)}</li>`)
      .join("");
    const strengths = evaluation.strengths.map((s) => `<li>${esc(s)}</li>`).join("");
    const corrections = evaluation.corrections
      .map(
        (c) => `<li><span class="tag">${esc(CATEGORY_LABELS[c.category])}</span> ${fix(c.original, c.corrected)}
          <div class="muted">${esc(c.reasonKo)}</div></li>`
      )
      .join("");
    const expressions = evaluation.keyExpressions
      .map((k) => `<li><strong>${esc(k.expression)}</strong> <span class="muted">${esc(k.meaningKo)}</span></li>`)
      .join("");
    body = `<p class="muted">이 답변의 예상 레벨(참고용) <strong>${esc(evaluation.overallLevel)}</strong> · ${
      evaluation.signals.wordCount
    }단어 · ${evaluation.signals.sentenceCount}문장</p>
      <h4>역량별 평가</h4><ul>${dimensions}</ul>
      ${strengths ? `<h4>잘한 점</h4><ul>${strengths}</ul>` : ""}
      ${corrections ? `<h4>고쳐 보면 좋은 부분</h4><ul>${corrections}</ul>` : ""}
      ${
        evaluation.targetScript
          ? `<h4>${targetLevel ? `${esc(targetLevel)} ` : ""}목표 스크립트</h4><p class="script">${esc(evaluation.targetScript)}</p>`
          : ""
      }
      ${expressions ? `<h4>추천 표현</h4><ul>${expressions}</ul>` : ""}`;
  } else {
    // Sessions from before automatic evaluation keep their free-text results.
    body = [
      answer.evaluationStatus === "SKIPPED" ? `<p class="muted">답변이 짧아서 평가하지 않았어요.</p>` : "",
      answer.evaluationStatus === "FAILED" ? `<p class="muted">이 답변은 평가하지 못했어요.</p>` : "",
      answer.analysis ? `<h4>AI 분석</h4><p class="script">${esc(answer.analysis)}</p>` : "",
      answer.improvedScript ? `<h4>개선 스크립트</h4><p class="script">${esc(answer.improvedScript)}</p>` : "",
    ].join("");
  }

  return `<section class="card">
    <p class="eyebrow">질문 ${number}</p>
    <h3>${esc(answer.questionText)}</h3>
    <p class="label">내 답변 · 음성 인식 결과(추정)</p>
    ${transcript}
    ${body}
  </section>`;
}

function fix(original: string, corrected: string): string {
  return `<del>${esc(original)}</del> → <ins>${esc(corrected)}</ins>`;
}

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

function esc(value: unknown): string {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

const STYLE = `
:root{color-scheme:light;--fg:#1a1d1f;--muted:#5f6670;--line:#e4e7eb;--soft:#f5f7f8;--brand:#087a58;--accent:#e7f6f0;--del:#b42318;--ins:#0b7a52}
*{box-sizing:border-box}
body{margin:0;background:#fff;color:var(--fg);font:15px/1.65 -apple-system,BlinkMacSystemFont,"Pretendard","Apple SD Gothic Neo","Malgun Gothic",sans-serif}
main{max-width:760px;margin:0 auto;padding:32px 16px 48px}
h1{font-size:28px;margin:4px 0}h2{font-size:20px;margin:2px 0}h3{font-size:16px;margin:20px 0 8px}h4{font-size:14px;margin:16px 0 6px}
.eyebrow{margin:0;font-size:13px;font-weight:600;color:var(--muted)}
.muted{color:var(--muted);font-size:13px}
.label{margin:12px 0 4px;font-size:12px;font-weight:600;color:var(--brand)}
.card{border:1px solid var(--line);border-radius:16px;padding:20px;margin-top:16px;break-inside:avoid}
.headline{display:flex;gap:16px;align-items:center}
.level{display:grid;place-items:center;min-width:72px;height:72px;border-radius:20px;background:var(--accent);color:var(--brand);font-size:28px;font-weight:800}
table{border-collapse:collapse;width:100%}th,td{text-align:left;padding:6px 0;border-bottom:1px solid var(--line)}td{font-weight:700;text-align:right}
ul,ol{padding-left:20px;margin:0}li{margin:6px 0}
.transcript,.script{background:var(--soft);border-radius:12px;padding:12px;white-space:pre-wrap;margin:0}
.tag{display:inline-block;border:1px solid var(--line);border-radius:999px;padding:0 8px;font-size:12px}
del{color:var(--del)}ins{color:var(--ins);text-decoration:none;font-weight:600}
.note{background:var(--soft);border-radius:12px;padding:12px}
footer{margin-top:24px;color:var(--muted);font-size:12px}
@media print{main{padding:0}.card{border-color:#ccc}}
`;
