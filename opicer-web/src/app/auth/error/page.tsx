import Link from "next/link";
import { CircleAlert } from "lucide-react";
import { ROUTES } from "@/lib/routes";
import { Button } from "@/components/ui/button";

export default function AuthError({
  searchParams,
}: {
  searchParams: { reason?: string };
}) {
  const reason = searchParams.reason ?? "unknown";

  return (
    <div className="flex min-h-dvh items-center justify-center px-4">
      <div className="flex w-full max-w-sm flex-col items-center gap-3 rounded-3xl border bg-card px-6 py-10 text-center shadow-raised">
        <span className="grid size-12 place-items-center rounded-full bg-destructive/10 text-destructive">
          <CircleAlert aria-hidden className="size-6" />
        </span>
        <p className="text-sm font-semibold text-destructive">로그인 실패</p>
        <h1 className="text-xl font-bold">카카오 로그인에 실패했습니다</h1>
        <p className="text-sm text-muted-foreground">
          오류 코드: <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs">{reason}</code>
        </p>
        <Button asChild className="mt-4 h-11 rounded-full px-6">
          <Link href={ROUTES.login}>다시 시도하기</Link>
        </Button>
      </div>
    </div>
  );
}
