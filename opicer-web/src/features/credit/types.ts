export type CreditOrderResponse = {
  orderId: string;
  userId: string;
  packageId: string;
  amount: number;
  status: string;
  createdAt: string;
};

export type CreditPaymentResponse = {
  paymentId: string;
  orderId: string;
  providerTxId: string;
  status: string;
  createdAt: string;
};

export type CreditPackage = {
  id: string;
  title: string;
  amount: number;
  description: string;
};

export const CREDIT_PACKAGES: CreditPackage[] = [
  { id: "PACK_10", title: "Starter 10", amount: 10000, description: "Credit pack for quick start" },
  { id: "PACK_30", title: "Plus 30", amount: 30000, description: "Credit pack for steady repetition" },
  { id: "PACK_50", title: "Pro 50", amount: 50000, description: "Large credit pack for long-term routine" },
];
