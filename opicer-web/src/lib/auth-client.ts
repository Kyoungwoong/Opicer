import { ROUTES } from "@/lib/routes";
import type { User } from "@/types/auth";

export async function fetchAuthMe(): Promise<User | null> {
  const res = await fetch(ROUTES.auth.me, { cache: "no-store" });
  if (res.status === 401) return null;
  if (!res.ok) throw new Error("Auth request failed");
  return res.json() as Promise<User>;
}

export async function logout(): Promise<void> {
  await fetch(ROUTES.auth.logout, { method: "POST" });
}
