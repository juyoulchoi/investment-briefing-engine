import { headers } from "next/headers";
import "server-only";
import type { ApiResult } from "../types";
export async function requestBackend<T>(
  path: string,
  envelope = true,
): Promise<T> {
  const origin = new URL(process.env.BACKEND_URL || "http://127.0.0.1:8081")
    .origin;
  const incoming = await headers();
  const forwarded = new Headers();
  for (const name of ["cookie", "authorization"]) {
    const value = incoming.get(name);
    if (value) forwarded.set(name, value);
  }
  let response: Response;
  try {
    response = await fetch(new URL(path, origin), {
      headers: forwarded,
      cache: "no-store",
      signal: AbortSignal.timeout(15000),
    });
  } catch {
    throw new Error("백엔드 연결에 실패했습니다.");
  }
  if (!response.ok)
    throw new Error(`데이터 조회에 실패했습니다. HTTP ${response.status}`);
  const body = await response.json();
  if (!envelope) return body as T;
  const result = body as ApiResult<T>;
  if (!result.success)
    throw new Error(result.error?.message || "데이터 조회에 실패했습니다.");
  return result.data;
}
