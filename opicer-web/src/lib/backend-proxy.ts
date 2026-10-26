import { NextRequest, NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

/**
 * Forwards a request to the Spring API with the caller's cookies and unwraps the
 * `{ status, message, data }` envelope on success. Error bodies (`{ code, message }`)
 * and statuses are passed through unchanged; 204 stays empty.
 */
export async function forwardToBackend(
  request: NextRequest,
  backendPath: string,
  method: "GET" | "POST" | "PUT" | "DELETE" = "GET"
): Promise<NextResponse> {
  const headers: Record<string, string> = { cookie: request.headers.get("cookie") ?? "" };
  let body: string | undefined;
  if (method === "POST" || method === "PUT") {
    body = await request.text();
    headers["Content-Type"] = "application/json";
  }

  let res: Response;
  try {
    res = await fetch(`${baseUrl}${backendPath}${request.nextUrl.search}`, {
      method,
      headers,
      body,
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { code: "BACKEND_UNAVAILABLE", message: "서버에 연결할 수 없습니다." },
      { status: 502 }
    );
  }

  if (res.status === 204) {
    return new NextResponse(null, { status: 204 });
  }
  const text = await res.text();
  if (!text) {
    return NextResponse.json(null, { status: res.status });
  }
  let json: { data?: unknown };
  try {
    json = JSON.parse(text) as { data?: unknown };
  } catch {
    return NextResponse.json(
      { code: "BAD_GATEWAY", message: "서버 응답을 해석할 수 없습니다." },
      { status: 502 }
    );
  }
  return NextResponse.json(res.ok ? json.data ?? null : json, { status: res.status });
}
