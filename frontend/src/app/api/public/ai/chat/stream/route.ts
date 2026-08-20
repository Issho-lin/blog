export const dynamic = "force-dynamic";
export const runtime = "nodejs";
export const maxDuration = 120;

const apiOrigin = process.env.API_PROXY_ORIGIN ?? "http://localhost:8080";

export async function POST(request: Request) {
  const upstream = await fetch(`${apiOrigin}/api/public/ai/chat/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream, application/json",
      cookie: request.headers.get("cookie") ?? "",
    },
    body: await request.text(),
    cache: "no-store",
  });

  return new Response(upstream.body, {
    status: upstream.status,
    headers: {
      "Content-Type": upstream.headers.get("content-type") ?? "text/event-stream; charset=utf-8",
      "Cache-Control": "no-cache, no-transform",
      Connection: "keep-alive",
      "X-Accel-Buffering": "no",
    },
  });
}
