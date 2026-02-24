"use client";

import { useEffect, useState } from "react";

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

  if (auth.status === "loading") return <LoadingScreen />;
  if (auth.status === "unauthenticated") return <LoginPage />;
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
/* Login                                                                */
/* ------------------------------------------------------------------ */
function LoginPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-6 py-16 text-[var(--ink)]">
      <div className="flex w-full max-w-sm flex-col items-center gap-8">

        {/* Wordmark */}
        <div className="flex flex-col items-center gap-2">
          <span className="text-xs uppercase tracking-[0.3em] text-[var(--muted)]">
            AI Speaking Coach
          </span>
          <h1 className="text-4xl font-bold tracking-tight text-[var(--accent-strong)]">
            Opicer
          </h1>
        </div>

        {/* Hero copy */}
        <div className="flex flex-col items-center gap-3 text-center">
          <p className="text-2xl font-semibold leading-snug">
            말하기 실력,
            <br />
            AI가 기억합니다.
          </p>
          <p className="text-sm leading-6 text-[var(--muted)]">
            내가 어떻게 말하는지 분석하고, 매번 더 나은 피드백을 드려요.
            <br />
            OPIC 연습부터 레벨업까지, 나만의 코치가 함께합니다.
          </p>
        </div>

        {/* Feature chips */}
        <div className="flex flex-wrap justify-center gap-2">
          {[
            { icon: "🎯", label: "주제별 맞춤 연습" },
            { icon: "🧠", label: "AI 습관 분석" },
            { icon: "📈", label: "레벨별 피드백" },
          ].map(({ icon, label }) => (
            <span
              key={label}
              className="flex items-center gap-1.5 rounded-full border border-[var(--accent)]/20 bg-[var(--card)] px-3 py-1.5 text-xs font-medium text-[var(--accent-strong)]"
            >
              <span>{icon}</span>
              {label}
            </span>
          ))}
        </div>

        {/* Login card */}
        <div className="w-full rounded-3xl border border-black/5 bg-[var(--card)] p-6 shadow-[0_40px_80px_-40px_rgba(27,124,122,0.35)]">
          <p className="mb-4 text-center text-sm font-medium text-[var(--muted)]">
            SNS 계정으로 바로 시작하세요
          </p>
          <a
            href="/api/auth/kakao"
            className="flex w-full items-center justify-center gap-2.5 rounded-xl py-3.5 text-sm font-bold transition active:scale-[0.98]"
            style={{ backgroundColor: "#FEE500", color: "#3C1E1E" }}
          >
            <KakaoIcon />
            카카오로 시작하기
          </a>
        </div>

        {/* Footer */}
        <p className="text-center text-[11px] leading-5 text-[var(--muted)]">
          로그인 시{" "}
          <a href="#" className="underline underline-offset-2">이용약관</a>
          {" "}및{" "}
          <a href="#" className="underline underline-offset-2">개인정보처리방침</a>
          에 동의하게 됩니다.
        </p>
      </div>
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

/* ------------------------------------------------------------------ */
/* Icons                                                                */
/* ------------------------------------------------------------------ */
function KakaoIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M9 1.5C4.858 1.5 1.5 4.134 1.5 7.383c0 2.088 1.317 3.921 3.315 4.977l-.845 3.152c-.075.278.186.5.432.344l3.71-2.484A9.2 9.2 0 0 0 9 13.266c4.142 0 7.5-2.634 7.5-5.883C16.5 4.134 13.142 1.5 9 1.5z"
        fill="#3C1E1E"
      />
    </svg>
  );
}
