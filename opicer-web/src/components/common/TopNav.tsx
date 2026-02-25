import Link from "next/link";

type NavItem = {
  title: string;
  href: string;
  disabled?: boolean;
};

const NAV_ITEMS: NavItem[] = [
  { title: "OPIC 설명", href: "/guide" },
  { title: "주제별 연습하기", href: "/practice" },
  { title: "실전 연습하기", href: "/mock", disabled: true },
];

type Props = {
  userLabel?: string;
  onLogout?: () => void;
  maxWidthClassName?: string;
};

export function TopNav({
  userLabel,
  onLogout,
  maxWidthClassName = "max-w-5xl",
}: Props) {
  return (
    <header
      className={`mx-auto flex w-full ${maxWidthClassName} items-center justify-between gap-6`}
    >
      <Link
        href="/"
        className="shrink-0 text-xl font-bold tracking-tight text-[var(--accent-strong)]"
      >
        Opicer
      </Link>

      <nav className="flex items-center gap-1">
        {NAV_ITEMS.map((menu) =>
          menu.disabled ? (
            <span
              key={menu.title}
              className="rounded-full px-4 py-2 text-sm font-medium text-[var(--muted)]/50 cursor-not-allowed"
            >
              {menu.title}
            </span>
          ) : (
            <Link
              key={menu.title}
              href={menu.href}
              className="rounded-full px-4 py-2 text-sm font-medium text-[var(--muted)] transition hover:bg-[var(--accent)]/10 hover:text-[var(--accent-strong)]"
            >
              {menu.title}
            </Link>
          )
        )}
      </nav>

      {userLabel && onLogout ? (
        <div className="flex shrink-0 items-center gap-3">
          <span className="text-sm text-[var(--muted)]">{userLabel}</span>
          <button
            onClick={onLogout}
            className="rounded-full border border-black/10 px-4 py-1.5 text-xs font-semibold text-[var(--muted)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
          >
            로그아웃
          </button>
        </div>
      ) : (
        <div className="w-[104px]" />
      )}
    </header>
  );
}
