import { NextRequest, NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

type ApiResponse<T> = {
  status: number;
  message: string;
  data: T;
};

export async function GET(request: NextRequest) {
  const res = await fetch(`${baseUrl}/api/universal-sentences/daily`, {
    headers: {
      cookie: request.headers.get("cookie") ?? "",
    },
    cache: "no-store",
  });

  const bodyText = await res.text();
  if (!bodyText) {
    return NextResponse.json([], { status: res.status });
  }

  const body = JSON.parse(bodyText) as ApiResponse<unknown>;
  if (!res.ok) {
    return NextResponse.json(body, { status: res.status });
  }

  return NextResponse.json(body.data, { status: res.status });
}
