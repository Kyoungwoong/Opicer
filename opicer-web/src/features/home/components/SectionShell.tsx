import { cn } from "@/lib/utils";
import { SectionHeader } from "@/components/common/SectionHeader";

type Props = {
  title: string;
  children: React.ReactNode;
  action?: React.ReactNode;
  /** "highlight" = soft honey background without border, for the one featured block. */
  tone?: "default" | "highlight";
  className?: string;
};

/** Home dashboard block: heading + optional action + content. */
export function SectionShell({ title, children, action, tone = "default", className }: Props) {
  return (
    <section
      className={cn(
        "flex flex-col gap-5 rounded-3xl p-6 sm:p-8",
        tone === "highlight" ? "bg-highlight-subtle" : "border bg-card shadow-card",
        className
      )}
    >
      <SectionHeader title={title} action={action} />
      <div className="flex min-w-0 flex-1 flex-col">{children}</div>
    </section>
  );
}
