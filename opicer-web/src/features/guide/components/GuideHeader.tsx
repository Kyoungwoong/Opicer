import { SectionHeader } from "@/components/common/SectionHeader";

type Props = {
  title: string;
  description: string;
};

export function GuideHeader({ title, description }: Props) {
  return (
    <header className="rounded-3xl border bg-card px-6 py-8 shadow-card sm:px-8 sm:py-10">
      <SectionHeader as="h1" title={title} description={description} />
    </header>
  );
}
