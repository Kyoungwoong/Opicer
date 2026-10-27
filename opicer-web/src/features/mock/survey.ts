import type { SurveyCatalog, SurveyPart, SurveySelections } from "@/features/mock/types";

/** Part 4 (여가, 취미, 운동, 휴가·출장) is every part that allows more than one choice. */
export function isPart4(part: SurveyPart): boolean {
  return part.max > 1;
}

export function part4Total(catalog: SurveyCatalog, selections: SurveySelections): number {
  return catalog.parts.filter(isPart4).reduce((sum, part) => sum + (selections[part.code]?.length ?? 0), 0);
}

/** Same rules as the server: one choice in parts 1–3, each part's minimum, and the Part 4 total. */
export function surveyProblems(catalog: SurveyCatalog, selections: SurveySelections): string[] {
  const problems: string[] = [];
  for (const part of catalog.parts) {
    const count = selections[part.code]?.length ?? 0;
    if (count < part.min) {
      problems.push(part.max === 1 ? `${part.title}: 하나를 골라 주세요.` : `${part.title}: ${part.min}개 이상 골라 주세요.`);
    }
  }
  const total = part4Total(catalog, selections);
  if (total < catalog.part4MinTotal) {
    problems.push(`Part 4에서 모두 ${catalog.part4MinTotal}개 이상 골라 주세요. 지금 ${total}개예요.`);
  }
  return problems;
}

/** A single-choice part replaces its choice; a multiple-choice part toggles the item. */
export function toggleSelection(part: SurveyPart, selections: SurveySelections, itemId: string): SurveySelections {
  const chosen = selections[part.code] ?? [];
  if (part.max === 1) return { ...selections, [part.code]: [itemId] };
  const next = chosen.includes(itemId) ? chosen.filter((id) => id !== itemId) : [...chosen, itemId];
  return { ...selections, [part.code]: next };
}

const STORAGE_KEY = "opicer.mock.survey";

/** The last survey this browser sent, keeping only items the current catalog still has. */
export function readSavedSurvey(catalog: SurveyCatalog): SurveySelections {
  try {
    const saved = JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? "null") as unknown;
    if (!saved || typeof saved !== "object") return {};
    const restored: SurveySelections = {};
    for (const part of catalog.parts) {
      const ids = (saved as Record<string, unknown>)[part.code];
      if (!Array.isArray(ids)) continue;
      const known = ids.filter((id): id is string => part.items.some((item) => item.id === id));
      restored[part.code] = known.slice(0, part.max);
    }
    return restored;
  } catch {
    return {};
  }
}

export function saveSurvey(selections: SurveySelections) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(selections));
  } catch {
    // Remembering the survey is only a convenience.
  }
}
