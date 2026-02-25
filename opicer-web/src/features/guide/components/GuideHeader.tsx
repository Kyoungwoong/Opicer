type Props = {
  title: string;
  description: string;
};

export function GuideHeader({ title, description }: Props) {
  return (
    <header className="rounded-3xl border border-black/5 bg-[var(--card)] px-6 py-8 shadow-[0_30px_70px_-50px_rgba(20,106,102,0.65)]">
      <p className="text-xs uppercase tracking-[0.35em] text-[var(--muted)]">
        OPIC Guide
      </p>
      <h1 className="mt-3 text-3xl font-semibold">{title}</h1>
      <p className="mt-2 text-sm text-[var(--muted)]">{description}</p>
    </header>
  );
}
