import Link from "next/link";
import { cn } from "@/lib/utils";

/** Opicer logotype (Space Grotesk) with the brand dot. Server-safe. */
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
