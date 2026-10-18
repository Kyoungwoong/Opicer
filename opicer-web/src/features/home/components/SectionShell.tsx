import { cn } from "@/lib/utils";
import { SectionHeader } from "@/components/common/SectionHeader";

type Props = {
  title: string;
  description: string;
  children: React.ReactNode;
  action?: React.ReactNode;
  className?: string;
};

/** Home dashboard card: English eyebrow + Korean heading + optional action. */
export function SectionShell({ title, description, children, action, className }: Props) {
  return (
    <section className={cn("flex flex-col gap-5 rounded-3xl border bg-card/90 p-5 shadow-card sm:p-6", className)}>
      <SectionHeader eyebrow={title} eyebrowLatin title={description} action={action} />
      <div>{children}</div>
    </section>
  );
}
