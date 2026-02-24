import { NextResponse } from "next/server";

const baseUrl = process.env.OPICER_API_BASE_URL || "http://localhost:8080";

export async function GET() {
  return NextResponse.redirect(`${baseUrl}/oauth2/authorization/kakao`);
}
