import { frontPageText } from "@/locales/frontPage";

export function LoadingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <span className="text-sm text-[var(--muted)]">{frontPageText.common.loading}</span>
    </div>
  );
}
