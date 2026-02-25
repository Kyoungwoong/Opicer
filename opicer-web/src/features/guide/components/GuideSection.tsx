type Props = {
  id: string;
  title: string;
  subtitle: string;
  children: React.ReactNode;
};

export function GuideSection({ id, title, subtitle, children }: Props) {
  return (
    <section
      id={id}
      className="rounded-3xl border border-black/5 bg-[var(--card)] p-6 shadow-[0_24px_60px_-48px_rgba(20,106,102,0.7)]"
    >
      <div className="flex flex-col gap-2">
        <p className="text-xs uppercase tracking-[0.3em] text-[var(--muted)]">
          {title}
        </p>
        <h2 className="text-2xl font-semibold">{subtitle}</h2>
      </div>
      <div className="mt-5">{children}</div>
    </section>
  );
}
