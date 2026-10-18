import { Brain, Sparkles, Target, TrendingUp } from "lucide-react";
import { ROUTES } from "@/lib/routes";
import { KakaoIcon } from "@/components/auth/KakaoIcon";
import { Wordmark } from "@/components/common/TopNav";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

const FEATURES = [
  { icon: Target, label: "주제별 맞춤 연습" },
  { icon: Brain, label: "AI 습관 분석" },
  { icon: TrendingUp, label: "레벨별 피드백" },
];

/** Illustrative sample of what feedback looks like. Not real user data. */
function FeedbackPreview() {
  return (
    <Card className="w-full max-w-sm gap-4 rounded-2xl bg-card/90 py-5 shadow-raised ring-border backdrop-blur">
      <CardContent className="flex flex-col gap-4 px-5">
        <div className="flex items-center justify-between">
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary">
            <Sparkles className="size-3.5" />
            예시 피드백
          </span>
          <Badge variant="secondary">예상 레벨 IM2</Badge>
        </div>
        <p className="text-sm leading-6 text-foreground">
          “I usually go to the park… <span className="rounded bg-highlight/60 px-1">and, uh, and</span>{" "}
          I like it because it’s quiet.”
        </p>
        <ul className="flex flex-col gap-2 text-sm">
          <li className="flex gap-2 rounded-lg bg-muted px-3 py-2">
            <span className="font-semibold text-primary-strong">습관</span>
            <span className="text-muted-foreground">연결어 ‘and’를 반복해요</span>
          </li>
          <li className="flex gap-2 rounded-lg bg-accent px-3 py-2">
            <span className="font-semibold text-primary-strong">제안</span>
            <span className="text-accent-foreground">‘What I love most is…’로 이어 보세요</span>
          </li>
        </ul>
      </CardContent>
    </Card>
  );
}

export function LoginView() {
  return (
    <div className="flex min-h-dvh items-center justify-center px-4 py-12 sm:px-6">
      <div className="grid w-full max-w-5xl items-center gap-12 lg:grid-cols-[1.1fr_0.9fr]">
        {/* Brand / value proposition */}
        <section className="flex flex-col items-center gap-8 text-center lg:items-start lg:text-left">
          <div className="flex flex-col items-center gap-3 lg:items-start">
            <span className="text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              AI Speaking Coach
            </span>
            <Wordmark className="text-4xl [&>span]:size-3.5" />
          </div>

          <div className="flex flex-col gap-4">
            <h1 className="text-3xl font-bold leading-tight sm:text-4xl">
              말하기 실력,
              <br />
              <span className="text-primary">AI가 기억합니다.</span>
            </h1>
            <p className="max-w-md text-base leading-7 text-muted-foreground">
              내가 어떻게 말하는지 분석하고, 매번 더 나은 피드백을 드려요.
              <br className="hidden sm:block" /> OPIC 연습부터 레벨업까지, 나만의 코치가 함께합니다.
            </p>
          </div>

          <ul className="flex flex-wrap justify-center gap-2 lg:justify-start">
            {FEATURES.map(({ icon: Icon, label }) => (
              <li
                key={label}
                className="inline-flex items-center gap-1.5 rounded-full border bg-card/80 px-3 py-1.5 text-sm font-medium text-primary-strong"
              >
                <Icon aria-hidden className="size-4 text-primary" />
                {label}
              </li>
            ))}
          </ul>

          <div className="hidden lg:block">
            <FeedbackPreview />
          </div>
        </section>

        {/* Sign-in card */}
        <section className="mx-auto w-full max-w-sm">
          <Card className="gap-6 rounded-3xl py-8 shadow-raised ring-border">
            <CardContent className="flex flex-col gap-6 px-6 sm:px-8">
              <div className="flex flex-col gap-1.5 text-center">
                <h2 className="text-xl font-bold">시작하기</h2>
                <p className="text-sm text-muted-foreground">SNS 계정으로 바로 시작하세요</p>
              </div>

              {/* Kakao brand guideline colors: #FEE500 container, #000 85% label */}
              <a
                href={ROUTES.auth.loginKakao}
                className="flex h-12 w-full items-center justify-center gap-2.5 rounded-xl bg-[#FEE500] text-[15px] font-semibold text-black/85 transition hover:brightness-[0.97] active:scale-[0.99]"
              >
                <KakaoIcon />
                카카오로 시작하기
              </a>

              <p className="text-center text-xs leading-5 text-muted-foreground">
                로그인 시{" "}
                <a href="#" className="underline underline-offset-2 hover:text-foreground">
                  이용약관
                </a>{" "}
                및{" "}
                <a href="#" className="underline underline-offset-2 hover:text-foreground">
                  개인정보처리방침
                </a>
                에 동의하게 됩니다.
              </p>
            </CardContent>
          </Card>
        </section>
      </div>
    </div>
  );
}
