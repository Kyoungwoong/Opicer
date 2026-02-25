import Link from "next/link";

type NavItem = {
  label: string;
  href: string;
  disabled?: boolean;
};

const NAV_ITEMS: NavItem[] = [
  { label: "홈", href: "/" },
  { label: "OPIC 설명", href: "/guide" },
  { label: "주제별 연습하기", href: "/practice", disabled: true },
  { label: "실전 연습하기", href: "/mock", disabled: true },
];

export function TopNav() {
  return (
    <header className="mx-auto flex w-full max-w-6xl items-center justify-between gap-6">
      <Link
        href="/"
        className="shrink-0 text-xl font-bold tracking-tight text-[var(--accent-strong)]"
      >
        Opicer
      </Link>

      <nav className="flex items-center gap-1">
        {NAV_ITEMS.map((item) =>
          item.disabled ? (
            <span
              key={item.label}
              className="rounded-full px-4 py-2 text-sm font-medium text-[var(--muted)]/50 cursor-not-allowed"
            >
              {item.label}
            </span>
          ) : (
            <Link
              key={item.label}
              href={item.href}
              className="rounded-full px-4 py-2 text-sm font-medium text-[var(--muted)] transition hover:bg-[var(--accent)]/10 hover:text-[var(--accent-strong)]"
            >
              {item.label}
            </Link>
          )
        )}
      </nav>
    </header>
  );
}
