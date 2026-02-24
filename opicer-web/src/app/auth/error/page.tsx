import Link from "next/link";

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
          로그인 실패
        </p>
        <h1 className="mt-3 text-2xl font-semibold">
          카카오 로그인에 실패했습니다
        </h1>
        <p className="mt-2 text-sm text-[var(--muted)]">오류 코드: {reason}</p>
        <Link
          href="/"
          className="mt-8 inline-block rounded-full bg-[var(--accent)] px-6 py-3 text-sm font-semibold text-white transition hover:bg-[var(--accent-strong)]"
        >
          다시 시도하기
        </Link>
      </div>
    </div>
  );
}
