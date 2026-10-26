import { cn } from "@/lib/utils";

/** Small, quiet Korean label above content (e.g. "시험 흐름"). */
export function Eyebrow({ children, className }: { children: React.ReactNode; className?: string }) {
  return <p className={cn("text-sm font-semibold text-muted-foreground", className)}>{children}</p>;
}

type Props = {
  eyebrow?: React.ReactNode;
  title: React.ReactNode;
  description?: React.ReactNode;
  /** h1 for page titles, h2 (default) for sections. */
  as?: "h1" | "h2";
  action?: React.ReactNode;
  className?: string;
};

export function SectionHeader({ eyebrow, title, description, as: Heading = "h2", action, className }: Props) {
  return (
    <div className={cn("flex flex-wrap items-end justify-between gap-4", className)}>
      <div className="flex min-w-0 flex-col gap-1.5">
        {eyebrow ? <Eyebrow>{eyebrow}</Eyebrow> : null}
        <Heading
          className={cn(
            "font-extrabold tracking-tight text-foreground",
            Heading === "h1" ? "text-3xl sm:text-4xl" : "text-xl"
          )}
        >
          {title}
        </Heading>
        {description ? <p className="text-base leading-relaxed text-muted-foreground">{description}</p> : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}
