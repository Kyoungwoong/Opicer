export default function AuthSuccessPage() {
  return (
    <main className="min-h-screen px-6 py-20 text-[var(--ink)]">
      <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 rounded-3xl border border-black/5 bg-white/80 p-10 shadow-[0_40px_80px_-60px_rgba(27,124,122,0.8)] backdrop-blur">
        <h1 className="text-3xl font-semibold">Login successful</h1>
        <p className="text-base text-[var(--muted)]">
          Your session is active. You can return to the dashboard.
        </p>
        <a
          className="inline-flex items-center justify-center rounded-full border border-black/10 px-6 py-3 text-sm font-semibold text-[var(--accent-strong)] transition hover:border-transparent hover:bg-[var(--accent)] hover:text-white"
          href="/"
        >
          Go to home
        </a>
      </div>
    </main>
  );
}
