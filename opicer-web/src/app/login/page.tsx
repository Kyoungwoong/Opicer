export default function LoginPage() {
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
            말하기 실력,
            <br />
            AI가 기억합니다.
          </p>
          <p className="text-sm leading-6 text-[var(--muted)]">
            내가 어떻게 말하는지 분석하고, 매번 더 나은 피드백을 드려요.
            <br />
            OPIC 연습부터 레벨업까지, 나만의 코치가 함께합니다.
          </p>
        </div>

        <div className="flex flex-wrap justify-center gap-2">
          {[
            { icon: "🎯", label: "주제별 맞춤 연습" },
            { icon: "🧠", label: "AI 습관 분석" },
            { icon: "📈", label: "레벨별 피드백" },
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
            SNS 계정으로 바로 시작하세요
          </p>
          <a
            href="/api/auth/login/kakao"
            className="flex w-full items-center justify-center gap-2.5 rounded-xl py-3.5 text-sm font-bold transition active:scale-[0.98]"
            style={{ backgroundColor: "#FEE500", color: "#3C1E1E" }}
          >
            <KakaoIcon />
            카카오로 시작하기
          </a>
        </div>

        <p className="text-center text-[11px] leading-5 text-[var(--muted)]">
          로그인 시{" "}
          <a href="#" className="underline underline-offset-2">이용약관</a>
          {" "}및{" "}
          <a href="#" className="underline underline-offset-2">개인정보처리방침</a>
          에 동의하게 됩니다.
        </p>
      </div>
    </div>
  );
}

function KakaoIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M9 1.5C4.858 1.5 1.5 4.134 1.5 7.383c0 2.088 1.317 3.921 3.315 4.977l-.845 3.152c-.075.278.186.5.432.344l3.71-2.484A9.2 9.2 0 0 0 9 13.266c4.142 0 7.5-2.634 7.5-5.883C16.5 4.134 13.142 1.5 9 1.5z"
        fill="#3C1E1E"
      />
    </svg>
  );
}
