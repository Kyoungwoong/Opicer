import { redirect } from "next/navigation";

export function GET() {
  const apiBase = process.env.OPICER_API_BASE_URL ?? "http://localhost:8080";
  redirect(`${apiBase}/oauth2/authorization/kakao`);
}
