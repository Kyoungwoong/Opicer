import { CalendarDays, ExternalLink } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

export const OPIC_REGISTER_URL = "https://www.opic.or.kr/opics/jsp/view/index.jsp";

type Props = {
  /** "compact" for the home card, "comfortable" for the guide page. */
  size?: "compact" | "comfortable";
  className?: string;
};

/**
 * Points to the official schedule. Opicer does not collect schedules from opic.or.kr (its robots.txt refuses
 * automated collection), so dates are always checked at the source.
 */
export function OpicOfficialSchedule({ size = "compact", className }: Props) {
  return (
    <div
      className={cn(
        "flex flex-col gap-4 rounded-2xl bg-secondary sm:flex-row sm:items-center sm:justify-between",
        size === "compact" ? "px-5 py-5" : "px-6 py-6",
        className
      )}
    >
      <div className="flex items-start gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-full bg-accent text-primary-strong">
          <CalendarDays aria-hidden className="size-5" />
        </span>
        <div className="flex flex-col gap-1">
          <p className="font-semibold">시험 일정과 접수는 opic.or.kr에서 확인하세요</p>
          <p className="text-sm text-muted-foreground">
            시험 날짜, 접수 기간, 성적 발표일은 회차마다 달라요. 접수 전에 공식 사이트의 최신 일정을 확인하세요.
          </p>
        </div>
      </div>
      <Button asChild className="shrink-0 rounded-full">
        <a href={OPIC_REGISTER_URL} target="_blank" rel="noreferrer">
          공식 일정 보기
          <ExternalLink />
        </a>
      </Button>
    </div>
  );
}
