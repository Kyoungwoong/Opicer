import { NextRequest, NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

export async function POST(request: NextRequest) {
  const res = await fetch(`${baseUrl}/api/auth/logout`, {
    method: "POST",
    headers: {
      cookie: request.headers.get("cookie") ?? "",
    },
  });

  const response = new NextResponse(null, { status: res.status });
  const getSetCookie = (res.headers as Headers & { getSetCookie?: () => string[] })
    .getSetCookie;
  const cookies = getSetCookie ? getSetCookie.call(res.headers) : [];

  if (cookies.length > 0) {
    cookies.forEach((cookie) => response.headers.append("set-cookie", cookie));
  } else {
    const setCookie = res.headers.get("set-cookie");
    if (setCookie) {
      response.headers.set("set-cookie", setCookie);
    }
  }

  return response;
}
