"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { TopNav } from "@/components/common/TopNav";
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
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      <TopNav
        userLabel={user.name ?? user.email ?? "사용자"}
        onLogout={onLogout}
        maxWidthClassName="max-w-5xl"
      />

      <main className="mx-auto mt-10 flex w-full max-w-5xl flex-col gap-8">
        <section className="rounded-3xl border border-black/10 bg-white p-8 shadow-sm">
          <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Credit Purchase</p>
          <h2 className="mt-2 text-3xl font-semibold">결제/크레딧 구매</h2>
          <p className="mt-2 text-sm text-[var(--muted)]">
            패키지를 선택하면 주문 생성 후 결제 확정까지 자동으로 진행됩니다.
          </p>
        </section>

        <section className="grid gap-4 md:grid-cols-3">
          {CREDIT_PACKAGES.map((pkg) => {
            const selected = pkg.id === selectedPackage.id;
            return (
              <button
                key={pkg.id}
                type="button"
                onClick={() => setSelectedPackageId(pkg.id)}
                className={`rounded-2xl border p-5 text-left transition ${
                  selected
                    ? "border-[var(--accent-strong)] bg-[var(--accent)]/10"
                    : "border-black/10 bg-white hover:border-[var(--accent)]/50"
                }`}
              >
                <p className="text-xs uppercase tracking-[0.16em] text-[var(--muted)]">{pkg.id}</p>
                <h3 className="mt-2 text-xl font-semibold">{pkg.title}</h3>
                <p className="mt-2 text-sm text-[var(--muted)]">{pkg.description}</p>
                <p className="mt-4 text-lg font-bold">{pkg.amount.toLocaleString()} P</p>
              </button>
            );
          })}
        </section>

        <section className="rounded-2xl border border-black/10 bg-white p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm text-[var(--muted)]">선택된 패키지</p>
              <p className="text-xl font-semibold">{selectedPackage.title}</p>
              <p className="text-sm text-[var(--muted)]">{selectedPackage.amount.toLocaleString()} P</p>
            </div>
            <button
              type="button"
              onClick={handlePurchase}
              disabled={isSubmitting}
              className="rounded-full bg-[var(--accent-strong)] px-6 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSubmitting ? "처리 중..." : "구매하기"}
            </button>
          </div>

          {paymentResult ? (
            <div className="mt-4 rounded-xl border border-emerald-300 bg-emerald-50 p-4 text-sm">
              <p className="font-semibold text-emerald-800">결제가 완료되었습니다.</p>
              <p className="mt-1 text-emerald-700">paymentId: {paymentResult.paymentId}</p>
              <p className="text-emerald-700">orderId: {paymentResult.orderId}</p>
            </div>
          ) : null}

          {error ? (
            <div className="mt-4 rounded-xl border border-rose-300 bg-rose-50 p-4 text-sm">
              <p className="font-semibold text-rose-800">결제 처리에 실패했습니다.</p>
              <p className="mt-1 text-rose-700">{error}</p>
              {lastRetryRequest ? (
                <button
                  type="button"
                  onClick={handleRetry}
                  disabled={isSubmitting}
                  className="mt-3 rounded-full bg-rose-700 px-4 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
                >
                  같은 요청 다시 시도
                </button>
              ) : null}
            </div>
          ) : null}
        </section>
      </main>
    </div>
  );
}
