import { NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

type ApiResponse<T> = {
  status: string;
  message: string;
  data: T;
};

export async function GET() {
  const res = await fetch(`${baseUrl}/api/health`, {
    headers: { "Content-Type": "application/json" },
    cache: "no-store",
  });

  const body = (await res.json()) as ApiResponse<unknown>;
  return NextResponse.json(body.data, { status: res.status });
}
