"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertCircle, Check, CheckCircle2, LoaderCircle, RotateCcw } from "lucide-react";
import { cn } from "@/lib/utils";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { SectionHeader } from "@/components/common/SectionHeader";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { createCreditOrder, confirmCreditPayment } from "@/features/credit/api";
import { CREDIT_PACKAGES, type CreditPackage, type CreditPaymentResponse } from "@/features/credit/types";
import { ROUTES } from "@/lib/routes";
import type { User } from "@/types/auth";

type RetryRequest = {
  orderId: string;
  providerTxId: string;
  idempotencyKey: string;
};

export function CreditPurchaseView({
  user,
  onLogout,
}: {
  user: User;
  onLogout: () => void;
}) {
  const router = useRouter();
  const [selectedPackageId, setSelectedPackageId] = useState<string>(CREDIT_PACKAGES[0].id);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [paymentResult, setPaymentResult] = useState<CreditPaymentResponse | null>(null);
  const [lastRetryRequest, setLastRetryRequest] = useState<RetryRequest | null>(null);

  const selectedPackage = useMemo<CreditPackage>(
    () => CREDIT_PACKAGES.find((pkg) => pkg.id === selectedPackageId) ?? CREDIT_PACKAGES[0],
    [selectedPackageId]
  );

  const handlePurchase = async () => {
    if (isSubmitting) return;
    setError(null);
    setPaymentResult(null);
    setIsSubmitting(true);
    setLastRetryRequest(null);

    try {
      const order = await createCreditOrder({
        packageId: selectedPackage.id,
        amount: selectedPackage.amount,
      });

      const retryPayload: RetryRequest = {
        orderId: order.orderId,
        providerTxId: `TX-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
        idempotencyKey: crypto.randomUUID(),
      };

      setLastRetryRequest(retryPayload);
      const payment = await confirmCreditPayment(retryPayload);
      setPaymentResult(payment);
      setLastRetryRequest(null);
      router.replace(ROUTES.home);
    } catch (e) {
      setError(e instanceof Error ? e.message : "구매 처리 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRetry = async () => {
    if (!lastRetryRequest || isSubmitting) return;
    setError(null);
    setIsSubmitting(true);

    try {
      const payment = await confirmCreditPayment(lastRetryRequest);
      setPaymentResult(payment);
      setLastRetryRequest(null);
      router.replace(ROUTES.home);
    } catch (e) {
      setError(e instanceof Error ? e.message : "재시도 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <PageShell
      width="lg"
      nav={
        <TopNav
          userLabel={user.name ?? user.email ?? "사용자"}
          onLogout={onLogout}
          maxWidthClassName="max-w-5xl"
        />
      }
    >
      <section className="rounded-3xl border bg-card p-6 shadow-card sm:p-8">
        <SectionHeader
          as="h1"
          title="결제/크레딧 구매"
          description="패키지를 선택하면 주문 생성 후 결제 확정까지 자동으로 진행됩니다."
        />
      </section>

      <section aria-label="크레딧 패키지" className="grid gap-3 md:grid-cols-3">
        {CREDIT_PACKAGES.map((pkg) => {
          const selected = pkg.id === selectedPackage.id;
          return (
            <button
              key={pkg.id}
              type="button"
              aria-pressed={selected}
              onClick={() => setSelectedPackageId(pkg.id)}
              className={cn(
                "relative flex flex-col gap-2 rounded-2xl border p-5 text-left transition-all",
                selected
                  ? "border-primary bg-accent shadow-raised ring-2 ring-primary/20"
                  : "bg-card hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-card"
              )}
            >
              {selected ? (
                <span className="absolute top-4 right-4 grid size-6 place-items-center rounded-full bg-primary text-primary-foreground">
                  <Check aria-hidden className="size-3.5" />
                </span>
              ) : null}
              <span className="text-xs font-medium text-muted-foreground">{pkg.id}</span>
              <span className="text-xl font-bold">{pkg.title}</span>
              <span className="text-sm text-muted-foreground">{pkg.description}</span>
              <span className="mt-2 text-2xl font-bold text-primary-strong tabular-nums">
                {pkg.amount.toLocaleString()} <span className="text-base font-semibold">P</span>
              </span>
            </button>
          );
        })}
      </section>

      <section className="flex flex-col gap-4 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="text-sm text-muted-foreground">선택된 패키지</p>
            <p className="text-xl font-bold">{selectedPackage.title}</p>
            <p className="text-sm text-muted-foreground tabular-nums">{selectedPackage.amount.toLocaleString()} P</p>
          </div>
          <Button
            type="button"
            onClick={handlePurchase}
            disabled={isSubmitting}
            className="h-11 rounded-full px-8 text-sm font-semibold md:min-w-40"
          >
            {isSubmitting ? <LoaderCircle className="animate-spin" /> : null}
            {isSubmitting ? "처리 중..." : "구매하기"}
          </Button>
        </div>

        {paymentResult ? (
          <Alert className="border-success/30 bg-success-subtle text-success">
            <CheckCircle2 />
            <AlertTitle>결제가 완료되었습니다.</AlertTitle>
            <AlertDescription className="text-success/90">
              <p>paymentId: {paymentResult.paymentId}</p>
              <p>orderId: {paymentResult.orderId}</p>
            </AlertDescription>
          </Alert>
        ) : null}

        {error ? (
          <Alert variant="destructive">
            <AlertCircle />
            <AlertTitle>결제 처리에 실패했습니다.</AlertTitle>
            <AlertDescription>
              <p>{error}</p>
              {lastRetryRequest ? (
                <Button
                  type="button"
                  variant="destructive"
                  size="sm"
                  onClick={handleRetry}
                  disabled={isSubmitting}
                  className="mt-2 rounded-full px-3"
                >
                  <RotateCcw />
                  같은 요청 다시 시도
                </Button>
              ) : null}
            </AlertDescription>
          </Alert>
        ) : null}
      </section>
    </PageShell>
  );
}
