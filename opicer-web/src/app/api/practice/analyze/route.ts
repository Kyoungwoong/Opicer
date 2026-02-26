import { NextRequest, NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

export async function POST(request: NextRequest) {
  const payload = await request.text();

  const res = await fetch(`${baseUrl}/api/practice/analyze`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      cookie: request.headers.get("cookie") ?? "",
    },
    body: payload,
  });

  const bodyText = await res.text();
  if (!bodyText) {
    return NextResponse.json(null, { status: res.status });
  }

  const body = JSON.parse(bodyText) as Record<string, unknown>;
  if (!res.ok) {
    return NextResponse.json(body, { status: res.status });
  }

  const data = (body as { data: Record<string, unknown> }).data;
  return NextResponse.json(data, { status: res.status });
}
