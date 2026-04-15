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
  { id: "PACK_10", title: "Starter 10", amount: 10000, description: "빠른 연습 시작용 크레딧 패키지" },
  { id: "PACK_30", title: "Plus 30", amount: 30000, description: "중간 강도의 반복 학습용 패키지" },
  { id: "PACK_50", title: "Pro 50", amount: 50000, description: "장기 학습 루틴용 대용량 패키지" },
];
