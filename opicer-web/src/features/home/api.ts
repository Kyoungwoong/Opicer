import type { UniversalSentence } from "@/features/home/types";

export async function fetchUniversalSentences(): Promise<UniversalSentence[]> {
  const res = await fetch(`/api/universal-sentences/daily`, {
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error("Failed to load universal sentences");
  }
  return (await res.json()) as UniversalSentence[];
}
