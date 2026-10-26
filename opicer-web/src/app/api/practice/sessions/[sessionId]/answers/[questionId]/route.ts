import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/backend-proxy";

type Context = { params: Promise<{ sessionId: string; questionId: string }> };

export async function PUT(request: NextRequest, context: Context) {
  const { sessionId, questionId } = await context.params;
  return forwardToBackend(
    request,
    `/api/practice/sessions/${encodeURIComponent(sessionId)}/answers/${encodeURIComponent(questionId)}`,
    "PUT"
  );
}
