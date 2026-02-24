import type { User } from "@/types/auth";

type MenuItem = {
  title: string;
  description: string;
  icon: string;
  href: string;
  disabled: boolean;
};

const MENUS: MenuItem[] = [
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

export function HomeView({
  user,
  onLogout,
}: {
  user: User;
  onLogout: () => void;
}) {
  return (
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      <header className="mx-auto flex max-w-4xl items-center justify-between gap-6">
        <span className="shrink-0 text-xl font-bold tracking-tight text-[var(--accent-strong)]">
          Opicer
        </span>

        <nav className="flex items-center gap-1">
          {MENUS.map((menu) =>
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
            onClick={onLogout}
            className="rounded-full border border-black/10 px-4 py-1.5 text-xs font-semibold text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
          >
            로그아웃
          </button>
        </div>
      </header>

      <main className="mx-auto mt-14 flex max-w-2xl flex-col gap-10">
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
