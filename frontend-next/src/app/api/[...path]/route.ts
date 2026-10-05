import type { NextRequest } from "next/server";

export const dynamic = "force-dynamic";
const hopHeaders = ["connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length"];

async function forward(request: NextRequest) {
  const backend = new URL(process.env.BACKEND_URL || "http://127.0.0.1:8081");
  const target = new URL(request.nextUrl.pathname + request.nextUrl.search, backend.origin);
  const headers = new Headers(request.headers);
  hopHeaders.forEach((name) => headers.delete(name));
  headers.delete("accept-encoding");
  try {
    const upstream = await fetch(target, {
      method: request.method,
      headers,
      body: ["GET", "HEAD"].includes(request.method) ? undefined : await request.arrayBuffer(),
      cache: "no-store",
      redirect: "manual",
      signal: AbortSignal.timeout(300_000),
    });
    const responseHeaders = new Headers(upstream.headers);
    hopHeaders.forEach((name) => responseHeaders.delete(name));
    responseHeaders.delete("content-encoding");
    responseHeaders.set("Cache-Control", "no-store");
    return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
  } catch {
    return Response.json({ success: false, error: { message: "백엔드 연결에 실패했습니다." } }, { status: 502, headers: { "Cache-Control": "no-store" } });
  }
}

export { forward as GET, forward as HEAD, forward as POST, forward as PUT, forward as PATCH, forward as DELETE, forward as OPTIONS };
