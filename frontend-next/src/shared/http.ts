import type { ApiResult } from "./types";
export async function requestEnvelope<T>(
  url: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(url, { ...init, cache: "no-store" });
  const body = (await response.json()) as ApiResult<T>;
  if (!response.ok || !body.success)
    throw new Error(body.error?.message || `HTTP ${response.status}`);
  return body.data;
}
export async function requestJson<T>(
  url: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(url, { ...init, cache: "no-store" });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json() as Promise<T>;
}
export async function requestFlexible<T>(
  url: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(url, { ...init, cache: "no-store" });
  const body = await response.json();
  if (!response.ok || body?.success === false)
    throw new Error(body?.error?.message || `HTTP ${response.status}`);
  return (body?.data ?? body) as T;
}
export const jsonBody = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});
