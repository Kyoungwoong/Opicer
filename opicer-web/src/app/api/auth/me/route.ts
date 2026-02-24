import { NextRequest, NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

export async function GET(request: NextRequest) {
  const res = await fetch(`${baseUrl}/api/auth/me`, {
    headers: {
      cookie: request.headers.get("cookie") ?? "",
    },
    cache: "no-store",
  });

  if (res.status === 401) {
    return NextResponse.json({ authenticated: false }, { status: 401 });
  }

  const data = await res.json();
  return NextResponse.json(data, { status: res.status });
}
