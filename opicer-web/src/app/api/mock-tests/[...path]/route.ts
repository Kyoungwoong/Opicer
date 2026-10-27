import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/backend-proxy";

type Context = { params: Promise<{ path: string[] }> };

/** `/survey`, `/{id}`, `/{id}/adjust`, `/{id}/submit`, `/{id}/evaluation/retry` on the signed-in user's tests. */
async function backendPath(context: Context) {
  const { path } = await context.params;
  return `/api/mock-tests/${path.map(encodeURIComponent).join("/")}`;
}

export async function GET(request: NextRequest, context: Context) {
  return forwardToBackend(request, await backendPath(context));
}

export async function POST(request: NextRequest, context: Context) {
  return forwardToBackend(request, await backendPath(context), "POST");
}

export async function DELETE(request: NextRequest, context: Context) {
  return forwardToBackend(request, await backendPath(context), "DELETE");
}
