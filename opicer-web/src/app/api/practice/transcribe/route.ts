import { NextRequest, NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

export async function POST(request: NextRequest) {
  // Forward the raw body with the original Content-Type (includes multipart boundary).
  // Avoid request.formData() → re-serialize, which can corrupt the stream.
  const body = await request.arrayBuffer();
  const contentType = request.headers.get("content-type") ?? "";

  const res = await fetch(`${baseUrl}/api/practice/transcribe`, {
    method: "POST",
    headers: {
      "content-type": contentType,
      cookie: request.headers.get("cookie") ?? "",
    },
    body,
  });

  const resText = await res.text();
  if (!resText) {
    return NextResponse.json(null, { status: res.status });
  }

  const resBody = JSON.parse(resText) as Record<string, unknown>;
  if (!res.ok) {
    return NextResponse.json(resBody, { status: res.status });
  }

  // Unwrap ApiResponse.data
  const data = (resBody as { data: Record<string, unknown> }).data;
  return NextResponse.json(data, { status: res.status });
}
