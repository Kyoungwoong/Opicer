import type { CreditOrderResponse, CreditPaymentResponse } from "@/features/credit/types";

type ApiError = {
  message?: string;
};

async function parseError(res: Response): Promise<string> {
  const text = await res.text().catch(() => "");
  if (!text) return `Request failed (${res.status})`;
  try {
    const body = JSON.parse(text) as ApiError;
    return body.message ?? text;
  } catch {
    return text;
  }
}

export async function createCreditOrder(input: {
  packageId: string;
  amount: number;
}): Promise<CreditOrderResponse> {
  const res = await fetch("/api/credits/orders", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });

  if (!res.ok) {
    throw new Error(await parseError(res));
  }

  return (await res.json()) as CreditOrderResponse;
}

export async function confirmCreditPayment(input: {
  orderId: string;
  providerTxId: string;
  idempotencyKey: string;
  simulateTimeout?: boolean;
}): Promise<CreditPaymentResponse> {
  const res = await fetch("/api/credits/payments/confirm", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": input.idempotencyKey,
    },
    body: JSON.stringify({
      orderId: input.orderId,
      providerTxId: input.providerTxId,
      simulateTimeout: input.simulateTimeout ?? false,
    }),
  });

  if (!res.ok) {
    throw new Error(await parseError(res));
  }

  return (await res.json()) as CreditPaymentResponse;
}
