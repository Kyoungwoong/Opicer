import { LoaderCircle } from "lucide-react";
import { ko } from "@/locales/ko";

export function LoadingScreen() {
  return (
    <div role="status" aria-live="polite" className="flex min-h-dvh flex-col items-center justify-center gap-3">
      <LoaderCircle aria-hidden className="size-6 animate-spin text-primary-strong" />
      <span className="text-sm text-muted-foreground">{ko.common.loading}</span>
    </div>
  );
}
