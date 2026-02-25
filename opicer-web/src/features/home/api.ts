import type { UniversalSentence } from "@/features/home/types";

export async function fetchUniversalSentences(
  size: number = 4
): Promise<UniversalSentence[]> {
  const res = await fetch(`/api/universal-sentences/random?size=${size}`, {
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error("Failed to load universal sentences");
  }
  return (await res.json()) as UniversalSentence[];
}
