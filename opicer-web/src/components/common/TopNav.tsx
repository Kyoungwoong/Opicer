"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronDown, CreditCard, LogOut, Menu, UserRound } from "lucide-react";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/routes";
import { ko } from "@/locales/ko";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";

type NavItem = {
  title: string;
  href: string;
  disabled?: boolean;
};

const NAV_ITEMS: NavItem[] = [
  { title: ko.common.topNav.items.guide, href: "/guide" },
  { title: ko.common.topNav.items.practice, href: "/practice" },
  { title: ko.common.topNav.items.mock, href: "/mock", disabled: true },
];

type Props = {
  userLabel?: string;
  onLogout?: () => void;
  maxWidthClassName?: string;
};

function isActivePath(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function Wordmark({ className }: { className?: string }) {
  return (
    <Link
      href="/"
      className={cn(
        "inline-flex shrink-0 items-center gap-2 rounded-md font-display text-xl font-bold tracking-tight text-primary-strong",
        className
      )}
    >
      <span aria-hidden className="size-2.5 rounded-full bg-primary shadow-[0_0_0_4px] shadow-primary/15" />
      Opicer
    </Link>
  );
}

export function TopNav({ userLabel, onLogout, maxWidthClassName = "max-w-5xl" }: Props) {
  const pathname = usePathname();
  const isSignedIn = Boolean(userLabel && onLogout);

  return (
    <header className={cn("mx-auto flex w-full items-center justify-between gap-4", maxWidthClassName)}>
      <Wordmark />

      {/* Desktop navigation */}
      <nav aria-label="주요 메뉴" className="hidden items-center gap-1 rounded-full border bg-card/70 p-1 shadow-card backdrop-blur md:flex">
        {NAV_ITEMS.map((item) =>
          item.disabled ? (
            <span
              key={item.title}
              aria-disabled="true"
              className="inline-flex cursor-not-allowed items-center gap-1.5 rounded-full px-4 py-1.5 text-sm font-medium text-muted-foreground/60"
            >
              {item.title}
              <Badge variant="secondary" className="px-1.5 text-[0.7rem] font-medium">
                준비중
              </Badge>
            </span>
          ) : (
            <Link
              key={item.title}
              href={item.href}
              aria-current={isActivePath(pathname, item.href) ? "page" : undefined}
              className="rounded-full px-4 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground aria-[current=page]:bg-primary aria-[current=page]:text-primary-foreground"
            >
              {item.title}
            </Link>
          )
        )}
      </nav>

      <div className="flex items-center gap-2">
        {/* Desktop account menu */}
        <div className="hidden md:block">
          {isSignedIn ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="h-9 gap-2 rounded-full px-3 text-sm">
                  <span className="grid size-6 place-items-center rounded-full bg-accent text-accent-foreground">
                    <UserRound className="size-3.5" />
                  </span>
                  <span className="max-w-32 truncate">{userLabel}</span>
                  <ChevronDown className="size-4 text-muted-foreground" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-44">
                <DropdownMenuItem asChild>
                  <Link href={ROUTES.mypage}>
                    <UserRound />
                    {ko.common.topNav.menu.mypage}
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link href={ROUTES.credit}>
                    <CreditCard />
                    {ko.common.topNav.menu.credit}
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem variant="destructive" onSelect={() => onLogout?.()}>
                  <LogOut />
                  {ko.common.topNav.menu.logout}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <Button asChild size="lg" className="rounded-full px-4">
              <Link href={ROUTES.login}>로그인</Link>
            </Button>
          )}
        </div>

        {/* Mobile menu */}
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="outline" size="icon-lg" className="rounded-full md:hidden" aria-label="메뉴 열기">
              <Menu />
            </Button>
          </SheetTrigger>
          <SheetContent side="right" className="w-[82%] max-w-xs gap-0 p-0">
            <SheetHeader className="border-b p-5">
              <SheetTitle asChild>
                <div>
                  <Wordmark className="text-lg" />
                </div>
              </SheetTitle>
              {isSignedIn ? (
                <p className="mt-1 text-sm text-muted-foreground">{userLabel} 님</p>
              ) : null}
            </SheetHeader>

            <nav aria-label="주요 메뉴" className="flex flex-col gap-1 p-3">
              {NAV_ITEMS.map((item) =>
                item.disabled ? (
                  <span
                    key={item.title}
                    aria-disabled="true"
                    className="flex items-center justify-between rounded-lg px-3 py-2.5 text-base text-muted-foreground/60"
                  >
                    {item.title}
                    <Badge variant="secondary">준비중</Badge>
                  </span>
                ) : (
                  <SheetClose asChild key={item.title}>
                    <Link
                      href={item.href}
                      aria-current={isActivePath(pathname, item.href) ? "page" : undefined}
                      className="rounded-lg px-3 py-2.5 text-base font-medium text-foreground transition-colors hover:bg-accent aria-[current=page]:bg-accent aria-[current=page]:text-accent-foreground"
                    >
                      {item.title}
                    </Link>
                  </SheetClose>
                )
              )}
            </nav>

            <Separator />

            <div className="flex flex-col gap-1 p-3">
              {isSignedIn ? (
                <>
                  <SheetClose asChild>
                    <Link href={ROUTES.mypage} className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-base hover:bg-accent">
                      <UserRound className="size-4 text-muted-foreground" />
                      {ko.common.topNav.menu.mypage}
                    </Link>
                  </SheetClose>
                  <SheetClose asChild>
                    <Link href={ROUTES.credit} className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-base hover:bg-accent">
                      <CreditCard className="size-4 text-muted-foreground" />
                      {ko.common.topNav.menu.credit}
                    </Link>
                  </SheetClose>
                  <button
                    type="button"
                    onClick={() => onLogout?.()}
                    className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-left text-base text-destructive hover:bg-destructive/10"
                  >
                    <LogOut className="size-4" />
                    {ko.common.topNav.menu.logout}
                  </button>
                </>
              ) : (
                <Button asChild size="lg" className="h-11 rounded-full">
                  <Link href={ROUTES.login}>로그인</Link>
                </Button>
              )}
            </div>
          </SheetContent>
        </Sheet>
      </div>
    </header>
  );
}
