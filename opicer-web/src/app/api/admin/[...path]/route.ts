import { NextRequest, NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

type Context = { params: Promise<{ path: string[] }> };

async function proxy(request: NextRequest, path: string[]) {
  const backendUrl = `${baseUrl}/api/admin/${path.join("/")}`;

  const hasBody =
    request.method !== "GET" && request.method !== "DELETE";
  const body = hasBody ? await request.text() : undefined;

  const res = await fetch(backendUrl, {
    method: request.method,
    headers: {
      "Content-Type": "application/json",
      cookie: request.headers.get("cookie") ?? "",
    },
    body,
    cache: "no-store",
  });

  const text = await res.text();
  return new NextResponse(text || null, {
    status: res.status,
    headers: { "Content-Type": "application/json" },
  });
}

export async function GET(request: NextRequest, context: Context) {
  const { path } = await context.params;
  return proxy(request, path);
}

export async function POST(request: NextRequest, context: Context) {
  const { path } = await context.params;
  return proxy(request, path);
}

export async function PUT(request: NextRequest, context: Context) {
  const { path } = await context.params;
  return proxy(request, path);
}

export async function DELETE(request: NextRequest, context: Context) {
  const { path } = await context.params;
  return proxy(request, path);
}
