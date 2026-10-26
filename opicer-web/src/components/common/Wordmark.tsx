import Link from "next/link";
import { cn } from "@/lib/utils";

/** "opicer" logotype in Jua. Server-safe. */
export function Wordmark({ className }: { className?: string }) {
  return (
    <Link
      href="/"
      aria-label="Opicer 홈"
      className={cn("inline-flex shrink-0 items-center rounded-md font-display text-[28px] leading-none text-primary", className)}
    >
      opicer
    </Link>
  );
}
