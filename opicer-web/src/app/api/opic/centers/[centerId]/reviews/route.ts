import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/backend-proxy";

type Context = { params: Promise<{ centerId: string }> };

export async function GET(request: NextRequest, { params }: Context) {
  const { centerId } = await params;
  return forwardToBackend(request, `/api/opic/centers/${encodeURIComponent(centerId)}/reviews`);
}
