import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  vus: 20,
  duration: "5s",
};

export function setup() {
  const payload = JSON.stringify({
    userId: "00000000-0000-0000-0000-000000000001",
    packageId: "PACK_10",
    amount: 10000,
  });
  const res = http.post(`${BASE_URL}/api/credits/orders`, payload, {
    headers: { "Content-Type": "application/json" },
  });
  const ok = check(res, { "order created": (r) => r.status === 201 });
  if (!ok) {
    throw new Error(`order create failed: ${res.status} ${res.body}`);
  }
  const body = res.json();
  return { orderId: body.data.orderId };
}

export default function (data) {
  const payload = JSON.stringify({
    orderId: data.orderId,
    providerTxId: `TX-${__VU}-${__ITER}-${Date.now()}`,
    simulateTimeout: __ITER === 0,
  });
  const res = http.post(`${BASE_URL}/api/credits/payments/confirm`, payload, {
    headers: { "Content-Type": "application/json" },
  });
  check(res, { "payment confirm ok": (r) => r.status === 200 || r.status === 504 });
  sleep(0.1);
}
