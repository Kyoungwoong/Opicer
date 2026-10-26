import { SectionHeader } from "@/components/common/SectionHeader";

type Props = {
  id: string;
  title: string;
  subtitle: string;
  children: React.ReactNode;
};

export function GuideSection({ id, title, subtitle, children }: Props) {
  return (
    <section id={id} className="scroll-mt-24 rounded-3xl border bg-card p-5 shadow-card sm:p-8">
      <SectionHeader eyebrow={title} title={subtitle} />
      <div className="mt-6">{children}</div>
    </section>
  );
}
