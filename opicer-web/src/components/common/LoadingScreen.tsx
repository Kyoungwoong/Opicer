import { ko } from "@/locales/ko";

export function LoadingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <span className="text-sm text-[var(--muted)]">{ko.common.loading}</span>
    </div>
  );
}
