type Props = {
  title: string;
  description: string;
  children: React.ReactNode;
  action?: React.ReactNode;
};

export function SectionShell({ title, description, children, action }: Props) {
  return (
    <section className="rounded-3xl border border-black/5 bg-[var(--card)] p-6 shadow-[0_24px_60px_-48px_rgba(20,106,102,0.7)]">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.25em] text-[var(--muted)]">
            {title}
          </p>
          <h3 className="mt-2 text-xl font-semibold">{description}</h3>
        </div>
        {action}
      </div>
      <div className="mt-6">{children}</div>
    </section>
  );
}
