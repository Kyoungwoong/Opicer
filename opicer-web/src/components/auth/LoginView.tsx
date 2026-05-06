import { ROUTES } from "@/lib/routes";
import { KakaoIcon } from "@/components/auth/KakaoIcon";

export function LoginView() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-6 py-16 text-[var(--ink)]">
      <div className="flex w-full max-w-sm flex-col items-center gap-8">
        <div className="flex flex-col items-center gap-2">
          <span className="text-xs uppercase tracking-[0.3em] text-[var(--muted)]">
            AI Speaking Coach
          </span>
          <h1 className="text-4xl font-bold tracking-tight text-[var(--accent-strong)]">
            Opicer
          </h1>
        </div>

        <div className="flex flex-col items-center gap-3 text-center">
          <p className="text-2xl font-semibold leading-snug">
            Speaking ability,
            <br />
            remembered by AI.
          </p>
          <p className="text-sm leading-6 text-[var(--muted)]">
            text text text mintext, text text text text text.
            <br />
            From OPIC practice to level-up, your personal coach is here.
          </p>
        </div>

        <div className="flex flex-wrap justify-center gap-2">
          {[
            { icon: "ðtext", label: "Topictext text text" },
            { icon: "ðtext", label: "AI text mintext" },
            { icon: "ðtext", label: "Level-based feedback" },
          ].map(({ icon, label }) => (
            <span
              key={label}
              className="flex items-center gap-1.5 rounded-full border border-[var(--accent)]/20 bg-[var(--card)] px-3 py-1.5 text-xs font-medium text-[var(--accent-strong)]"
            >
              <span>{icon}</span>
              {label}
            </span>
          ))}
        </div>

        <div className="w-full rounded-3xl border border-black/5 bg-[var(--card)] p-6 shadow-[0_40px_80px_-40px_rgba(27,124,122,0.35)]">
          <p className="mb-4 text-center text-sm font-medium text-[var(--muted)]">
            Start instantly with social account
          </p>
          <a
            href={ROUTES.auth.loginKakao}
            className="flex w-full items-center justify-center gap-2.5 rounded-xl py-3.5 text-sm font-bold transition active:scale-[0.98]"
            style={{ backgroundColor: "#FEE500", color: "#3C1E1E" }}
          >
            <KakaoIcon />
            Continue with Kakao
          </a>
        </div>

        <p className="text-center text-[11px] leading-5 text-[var(--muted)]">
          By logging in,{" "}
          <a href="#" className="underline underline-offset-2">Terms of Service</a>
          {" "}text{" "}
          <a href="#" className="underline underline-offset-2">itemstext</a>
          you agree to these policies.
        </p>
      </div>
    </div>
  );
}
