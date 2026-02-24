"use client";

import { useEffect, useState } from "react";

type HealthStatus = {
  status: string;
  timestamp: string;
};

type AuthStatus = {
  id: string;
  email: string | null;
  name: string | null;
  role: string;
  provider: string;
};

type HealthState =
  | { state: "idle" }
  | { state: "loading" }
  | { state: "ok"; data: HealthStatus }
  | { state: "error"; message: string };

type AuthState =
  | { state: "idle" }
  | { state: "loading" }
  | { state: "authenticated"; data: AuthStatus }
  | { state: "unauthenticated" }
  | { state: "error"; message: string };

const initialState: HealthState = { state: "idle" };
const initialAuthState: AuthState = { state: "idle" };

export default function Home() {
  const [health, setHealth] = useState<HealthState>(initialState);
  const [auth, setAuth] = useState<AuthState>(initialAuthState);

  const loadHealth = async () => {
    setHealth({ state: "loading" });
    try {
      const res = await fetch("/api/health", { cache: "no-store" });
      if (!res.ok) {
        throw new Error(`Backend error (${res.status})`);
      }
      const data = (await res.json()) as HealthStatus;
      setHealth({ state: "ok", data });
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Backend unreachable";
      setHealth({ state: "error", message });
    }
  };

  const loadAuth = async () => {
    setAuth({ state: "loading" });
    try {
      const res = await fetch("/api/auth/me", { cache: "no-store" });
      if (res.status === 401) {
        setAuth({ state: "unauthenticated" });
        return;
      }
      if (!res.ok) {
        throw new Error(`Auth error (${res.status})`);
      }
      const data = (await res.json()) as AuthStatus;
      setAuth({ state: "authenticated", data });
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Auth unreachable";
      setAuth({ state: "error", message });
    }
  };

  const handleLogout = async () => {
    await fetch("/api/auth/logout", { method: "POST" });
    await loadAuth();
  };

  useEffect(() => {
    void loadHealth();
    void loadAuth();
  }, []);

  return (
    <div className="min-h-screen px-6 py-16 text-[var(--ink)]">
      <main className="mx-auto flex w-full max-w-4xl flex-col gap-10">
        <header className="flex flex-col gap-4">
          <span className="text-xs uppercase tracking-[0.3em] text-[var(--muted)]">
            Opicer Frontend
          </span>
          <h1 className="text-4xl font-semibold leading-tight md:text-5xl">
            Health check console for the OPIC coaching stack.
          </h1>
          <p className="max-w-2xl text-base leading-7 text-[var(--muted)]">
            This page verifies the backend API connection through a Next.js proxy
            route. If the backend is down, you will see an explicit error.
          </p>
        </header>

        <section className="grid gap-6 rounded-3xl border border-black/5 bg-[var(--card)] p-8 shadow-[0_40px_80px_-60px_rgba(27,124,122,0.8)] backdrop-blur">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <span className="h-2.5 w-2.5 rounded-full bg-[var(--accent)] shadow-[0_0_0_6px_var(--ring)]" />
              <p className="text-sm font-medium tracking-wide text-[var(--muted)]">
                Backend /api/health status
              </p>
            </div>
            <button
              onClick={loadHealth}
              className="rounded-full border border-black/10 px-5 py-2 text-sm font-semibold text-[var(--accent-strong)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
            >
              Refresh
            </button>
          </div>

          <div className="grid gap-3">
            {health.state === "loading" && (
              <p className="text-lg font-semibold text-[var(--muted)]">
                Checking backend...
              </p>
            )}

            {health.state === "ok" && (
              <>
                <div className="flex items-center gap-3">
                  <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-emerald-700">
                    {health.data.status}
                  </span>
                  <p className="text-sm text-[var(--muted)]">
                    {new Date(health.data.timestamp).toLocaleString()}
                  </p>
                </div>
                <p className="text-base text-[var(--muted)]">
                  Backend responded successfully. You can start wiring API calls
                  from here.
                </p>
              </>
            )}

            {health.state === "error" && (
              <>
                <div className="flex items-center gap-3">
                  <span className="rounded-full bg-rose-100 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-rose-700">
                    error
                  </span>
                  <p className="text-sm text-[var(--muted)]">{health.message}</p>
                </div>
                <p className="text-base text-[var(--muted)]">
                  Start the backend at <span className="font-mono">:8080</span>{" "}
                  or update <span className="font-mono">OPICER_API_BASE_URL</span>{" "}
                  in <span className="font-mono">.env</span>.
                </p>
              </>
            )}
          </div>
        </section>

        <section className="grid gap-6 rounded-3xl border border-black/5 bg-white/70 p-8 shadow-[0_30px_70px_-50px_rgba(15,75,73,0.7)] backdrop-blur">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <span className="h-2.5 w-2.5 rounded-full bg-[var(--accent-strong)] shadow-[0_0_0_6px_var(--ring)]" />
              <p className="text-sm font-medium tracking-wide text-[var(--muted)]">
                OAuth session
              </p>
            </div>
            {auth.state === "authenticated" ? (
              <button
                onClick={handleLogout}
                className="rounded-full border border-black/10 px-5 py-2 text-sm font-semibold text-[var(--accent-strong)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
              >
                Logout
              </button>
            ) : (
              <a
                className="rounded-full border border-black/10 px-5 py-2 text-sm font-semibold text-[var(--accent-strong)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
                href="/login"
              >
                Login with Kakao
              </a>
            )}
          </div>

          <div className="grid gap-3">
            {auth.state === "loading" && (
              <p className="text-lg font-semibold text-[var(--muted)]">
                Checking session...
              </p>
            )}

            {auth.state === "unauthenticated" && (
              <p className="text-base text-[var(--muted)]">
                No active session. Login to continue.
              </p>
            )}

            {auth.state === "authenticated" && (
              <div className="grid gap-2 text-sm text-[var(--muted)]">
                <span className="uppercase tracking-[0.2em] text-xs text-[var(--accent-strong)]">
                  Logged in
                </span>
                <div>Name: {auth.data.name ?? "Unknown"}</div>
                <div>Email: {auth.data.email ?? "Unknown"}</div>
                <div>Role: {auth.data.role}</div>
                <div>Provider: {auth.data.provider}</div>
              </div>
            )}

            {auth.state === "error" && (
              <p className="text-base text-[var(--muted)]">{auth.message}</p>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
