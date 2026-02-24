import { NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

export async function GET() {
  const res = await fetch(`${baseUrl}/api/health`, {
    headers: { "Content-Type": "application/json" },
    cache: "no-store",
  });

  const data = await res.json();
  return NextResponse.json(data, { status: res.status });
}
