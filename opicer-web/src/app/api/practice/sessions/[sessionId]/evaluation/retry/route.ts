import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/backend-proxy";

type Context = { params: Promise<{ sessionId: string }> };

export async function POST(request: NextRequest, context: Context) {
  const { sessionId } = await context.params;
  return forwardToBackend(
    request,
    `/api/practice/sessions/${encodeURIComponent(sessionId)}/evaluation/retry`,
    "POST"
  );
}
