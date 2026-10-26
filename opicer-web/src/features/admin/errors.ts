/**
 * The admin request helper throws "<status>: <body>". Returns the server's `{ code, message }` when the body is
 * JSON, so tabs can show a friendly message per code; `message` falls back to the raw text.
 */
export function parseAdminError(err: unknown): { code: string | null; message: string } {
  const raw = err instanceof Error ? err.message : String(err);
  const json = raw.slice(raw.indexOf(":") + 1).trim();
  try {
    const body = JSON.parse(json) as { message?: string; code?: string };
    return { code: body.code ?? null, message: body.message ?? raw };
  } catch {
    return { code: null, message: raw };
  }
}
