"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

type User = {
  id: string;
  email: string | null;
  name: string | null;
  role: string;
  provider: string;
};

type AuthState =
  | { status: "loading" }
  | { status: "authenticated"; user: User }
  | { status: "unauthenticated" };

export default function Page() {
  const [auth, setAuth] = useState<AuthState>({ status: "loading" });
  const router = useRouter();

  useEffect(() => {
    fetch("/api/auth/me", { cache: "no-store" })
      .then((res) => {
        if (res.status === 401) return null;
        if (!res.ok) throw new Error();
        return res.json() as Promise<User>;
      })
      .then((user) => {
        if (user) setAuth({ status: "authenticated", user });
        else setAuth({ status: "unauthenticated" });
      })
      .catch(() => setAuth({ status: "unauthenticated" }));
  }, []);

  useEffect(() => {
    if (auth.status === "unauthenticated") {
      router.replace("/login");
    }
  }, [auth.status, router]);

  if (auth.status === "loading" || auth.status === "unauthenticated") {
    return <LoadingScreen />;
  }
  return <HomeScreen user={auth.user} />;
}

/* ------------------------------------------------------------------ */
/* Loading                                                              */
/* ------------------------------------------------------------------ */
function LoadingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <span className="text-sm text-[var(--muted)]">불러오는 중...</span>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Home                                                                 */
/* ------------------------------------------------------------------ */
function HomeScreen({ user }: { user: User }) {
  const handleLogout = async () => {
    await fetch("/api/auth/logout", { method: "POST" });
    window.location.href = "/";
  };

  const menus = [
    {
      title: "OPIC 설명",
      description: "실제 OPIC 시험 구조와 레벨 기준을 알아보세요.",
      icon: "📖",
      href: "/guide",
      disabled: false,
    },
    {
      title: "주제별 연습하기",
      description: "원하는 주제를 골라 AI와 함께 말하기 연습을 해보세요.",
      icon: "🎯",
      href: "/practice",
      disabled: true,
    },
    {
      title: "실전 연습하기",
      description: "실제 OPIC 시험처럼 풀코스 모의테스트를 진행합니다.",
      icon: "🏆",
      href: "/mock",
      disabled: true,
    },
  ];

  return (
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      {/* Header */}
      <header className="mx-auto flex max-w-4xl items-center justify-between gap-6">
        <span className="shrink-0 text-xl font-bold tracking-tight text-[var(--accent-strong)]">
          Opicer
        </span>

        {/* Nav */}
        <nav className="flex items-center gap-1">
          {menus.map((menu) =>
            menu.disabled ? (
              <span
                key={menu.title}
                className="rounded-full px-4 py-2 text-sm font-medium text-[var(--muted)]/50 cursor-not-allowed"
              >
                {menu.title}
              </span>
            ) : (
              <a
                key={menu.title}
                href={menu.href}
                className="rounded-full px-4 py-2 text-sm font-medium text-[var(--muted)] transition hover:bg-[var(--accent)]/10 hover:text-[var(--accent-strong)]"
              >
                {menu.title}
              </a>
            )
          )}
        </nav>

        <div className="flex shrink-0 items-center gap-3">
          <span className="text-sm text-[var(--muted)]">
            {user.name ?? user.email ?? "사용자"}
          </span>
          <button
            onClick={handleLogout}
            className="rounded-full border border-black/10 px-4 py-1.5 text-xs font-semibold text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
          >
            로그아웃
          </button>
        </div>
      </header>

      {/* Body */}
      <main className="mx-auto mt-14 flex max-w-2xl flex-col gap-10">
        {/* Welcome */}
        <div className="flex flex-col gap-2">
          <p className="text-xs uppercase tracking-[0.25em] text-[var(--muted)]">
            Welcome back
          </p>
          <h2 className="text-3xl font-semibold leading-tight">
            안녕하세요, {user.name?.split(" ")[0] ?? "반갑습니다"} 👋
          </h2>
          <p className="text-sm text-[var(--muted)]">
            오늘도 말하기 연습을 시작해볼까요?
          </p>
        </div>
      </main>
    </div>
  );
}

function MenuCard({
  title,
  description,
  icon,
  href,
  disabled,
}: {
  title: string;
  description: string;
  icon: string;
  href: string;
  disabled: boolean;
}) {
  const inner = (
    <div
      className={`flex items-center gap-5 rounded-2xl border border-black/5 bg-[var(--card)] p-6 shadow-sm transition ${
        disabled
          ? "opacity-50 cursor-not-allowed"
          : "cursor-pointer hover:shadow-md hover:border-[var(--accent)]/20 hover:-translate-y-0.5"
      }`}
    >
      <span className="text-3xl">{icon}</span>
      <div className="flex flex-col gap-0.5">
        <div className="flex items-center gap-2">
          <span className="text-base font-semibold">{title}</span>
          {disabled && (
            <span className="rounded-full bg-[var(--muted)]/10 px-2 py-0.5 text-[10px] font-medium text-[var(--muted)]">
              준비 중
            </span>
          )}
        </div>
        <span className="text-sm text-[var(--muted)]">{description}</span>
      </div>
      {!disabled && (
        <span className="ml-auto text-[var(--accent)]">→</span>
      )}
    </div>
  );

  if (disabled) return <div>{inner}</div>;
  return <a href={href}>{inner}</a>;
}
