import Link from "next/link";
import { ROUTES } from "@/lib/routes";

export default function AuthError({
  searchParams,
}: {
  searchParams: { reason?: string };
}) {
  const reason = searchParams.reason ?? "unknown";

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-6 text-[var(--ink)]">
      <div className="w-full max-w-sm text-center">
        <p className="text-xs uppercase tracking-[0.25em] text-[var(--muted)]">
          Login Failed
        </p>
        <h1 className="mt-3 text-2xl font-semibold">
          Kakao login failed
        </h1>
        <p className="mt-2 text-sm text-[var(--muted)]">Error code: {reason}</p>
        <Link
          href={ROUTES.login}
          className="mt-8 inline-block rounded-full bg-[var(--accent)] px-6 py-3 text-sm font-semibold text-white transition hover:bg-[var(--accent-strong)]"
        >
          Try again
        </Link>
      </div>
    </div>
  );
}
