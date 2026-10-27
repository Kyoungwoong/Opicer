import { CATEGORY_LABELS, LEVEL_DISCLAIMER } from "@/features/practice/evaluation-labels";
import {
  STYLE,
  answerSection,
  downloadHtml,
  esc,
  formatDate,
  reportFileName,
  reportSection,
} from "@/features/practice/report-export";
import { formatClock } from "@/features/mock/exam";
import type { MockTestDetail, MockTestInsights, MockTestItemDetail } from "@/features/mock/types";

/**
 * Self-contained HTML page for a mock test: the level report, the extra feedback and every answer's full
 * evaluation. Same fixed template and escaping as the practice export; no server call and no model tokens.
 */
export function renderMockReportHtml(test: MockTestDetail, generatedAt: Date = new Date()): string {
  const items = [...test.items].sort((a, b) => a.order - b.order);
  return `<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>실전연습 리포트</title>
<style>${STYLE}</style>
</head>
<body>
<main>
  <header>
    <p class="eyebrow">Opicer 실전연습 리포트 · 연습용 모의 시험</p>
    <h1>실전연습 결과</h1>
    <p class="muted">${esc(formatDate(test.submittedAt))} · ${items.length}문항${
      test.targetLevel ? ` · 목표 ${esc(test.targetLevel)}` : ""
    }${test.examSeconds != null ? ` · 시험 시간 ${esc(formatClock(test.examSeconds))}` : ""}</p>
  </header>
  ${test.report ? reportSection(test.report) : `<p class="note">이 시험에는 평가 리포트가 없어요.</p>`}
  ${test.insights ? insightsSection(test.insights) : ""}
  ${items.map((item) => answerSection(asAnswer(item), item.order, test.targetLevel, "full")).join("\n")}
  <footer>
    <p>${esc(test.report?.disclaimer || LEVEL_DISCLAIMER)}. 음성 인식 결과(추정)를 바탕으로 한 연습용 결과예요.</p>
    <p>${esc(formatDate(generatedAt.toISOString()))}에 저장함 · 녹음 파일은 포함되지 않아요.</p>
  </footer>
</main>
</body>
</html>
`;
}

export function downloadMockReportHtml(test: MockTestDetail): void {
  const fileName = reportFileName({ completedAt: test.submittedAt }).replace("opicer-report-", "opicer-mock-report-");
  downloadHtml(renderMockReportHtml(test), fileName);
}

function asAnswer(item: MockTestItemDetail) {
  return {
    questionId: item.id,
    questionOrder: item.order,
    questionText: item.questionText,
    transcript: item.transcript,
    analysis: null,
    improvedScript: null,
    durationSeconds: item.durationSeconds,
    evaluationStatus: item.evaluationStatus,
    overallLevel: item.overallLevel,
    evaluation: item.evaluation,
  };
}

function insightsSection(insights: MockTestInsights): string {
  const types = insights.questionTypes
    .map(
      (t) => `<tr><th>${esc(t.label)} <span class="muted">답변 ${t.answers}개${
        t.topIssue ? ` · 자주 고친 부분 ${esc(CATEGORY_LABELS[t.topIssue])}` : ""
      }</span></th><td>${esc(t.level)}</td></tr>`
    )
    .join("");
  const notes = [...insights.habits.notes, ...insights.time.notes].map((n) => `<li>${esc(n)}</li>`).join("");
  const connectors = insights.habits.connectors.map((c) => `${esc(c.word)} ×${c.count}`).join(", ");
  const repeated = insights.habits.repeatedWords.map((w) => `${esc(w.word)} ×${w.count}`).join(", ");
  const recommendations = insights.recommendations
    .map((r) => `<li><strong>${esc(r.label)}</strong> ${r.topics.map((t) => esc(t.title)).join(", ")}</li>`)
    .join("");
  const time = insights.time;

  return `<section class="card">
    <h2>추가 피드백</h2>
    ${types ? `<h3>유형별 진단</h3><table>${types}</table>` : ""}
    <h3>말하기 습관</h3>
    <p class="muted">평균 문장 길이 ${insights.habits.averageSentenceLength}단어 · 어휘 다양성 ${insights.habits.lexicalDiversity.toFixed(2)}</p>
    ${connectors ? `<p>연결어: ${connectors}</p>` : ""}
    ${repeated ? `<p>자주 반복한 단어: ${repeated}</p>` : ""}
    <h3>시간 관리</h3>
    <p class="muted">사용 ${esc(formatClock(time.usedSeconds))} / ${esc(formatClock(time.limitSeconds))} · 1부 ${esc(
      formatClock(time.part1Seconds)
    )} · 2부 ${esc(formatClock(time.part2Seconds))}</p>
    ${notes ? `<ul>${notes}</ul>` : ""}
    ${recommendations ? `<h3>다음 연습 추천</h3><ul>${recommendations}</ul>` : ""}
  </section>`;
}
