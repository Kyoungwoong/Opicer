import { NextRequest, NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

export async function POST(request: NextRequest) {
  const res = await fetch(`${baseUrl}/api/auth/logout`, {
    method: "POST",
    headers: {
      cookie: request.headers.get("cookie") ?? "",
    },
  });

  return NextResponse.json({}, { status: res.status });
}
