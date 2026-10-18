import { cn } from "@/lib/utils";

/**
 * Small label above a heading. Korean labels stay at normal tracking;
 * pass `latin` for short English labels, which read better spaced out.
 */
export function Eyebrow({
  children,
  latin = false,
  className,
}: {
  children: React.ReactNode;
  latin?: boolean;
  className?: string;
}) {
  return (
    <p
      className={cn(
        "text-xs font-semibold text-primary",
        latin && "uppercase tracking-[0.16em]",
        className
      )}
    >
      {children}
    </p>
  );
}

type Props = {
  eyebrow?: React.ReactNode;
  eyebrowLatin?: boolean;
  title: React.ReactNode;
  description?: React.ReactNode;
  /** h1 for page titles, h2 (default) for sections. */
  as?: "h1" | "h2";
  action?: React.ReactNode;
  className?: string;
};

export function SectionHeader({
  eyebrow,
  eyebrowLatin,
  title,
  description,
  as: Heading = "h2",
  action,
  className,
}: Props) {
  return (
    <div className={cn("flex flex-wrap items-end justify-between gap-4", className)}>
      <div className="flex min-w-0 flex-col gap-2">
        {eyebrow ? <Eyebrow latin={eyebrowLatin}>{eyebrow}</Eyebrow> : null}
        <Heading
          className={cn(
            "font-bold text-foreground",
            Heading === "h1" ? "text-2xl sm:text-3xl" : "text-xl sm:text-2xl"
          )}
        >
          {title}
        </Heading>
        {description ? (
          <p className="text-sm text-muted-foreground sm:text-base">{description}</p>
        ) : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}
