import assert from "node:assert/strict";
import { createServer } from "node:http";
import { spawn } from "node:child_process";
import { setTimeout } from "node:timers/promises";

// Exercise the built server against a temporary backend, never production data.
const backend = createServer(async (req, res) => {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  if (req.url === "/api/empty") { res.writeHead(204).end(); return; }
  res.writeHead(req.url === "/api/rejected" ? 422 : 200, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ method: req.method, url: req.url, contentType: req.headers["content-type"], body: Buffer.concat(chunks).toString("base64") }));
});
await new Promise((resolve) => backend.listen(0, "127.0.0.1", resolve));
const reserve = createServer();
await new Promise((resolve) => reserve.listen(0, "127.0.0.1", resolve));
const port = reserve.address().port;
await new Promise((resolve) => reserve.close(resolve));
const child = spawn(process.execPath, [".next/standalone/server.js"], {
  env: { ...process.env, PORT: String(port), HOSTNAME: "127.0.0.1", BACKEND_URL: `http://127.0.0.1:${backend.address().port}`, NEXT_TELEMETRY_DISABLED: "1" },
  stdio: ["ignore", "pipe", "pipe"],
});
let logs = "";
child.stdout.on("data", (data) => { logs += data; });
child.stderr.on("data", (data) => { logs += data; });
const url = `http://127.0.0.1:${port}`;
try {
  let ready = false;
  for (let attempt = 0; attempt < 100; attempt++) {
    try { const r = await fetch(`${url}/api/ping`); if (r.ok) { ready = true; break; } } catch {}
    await setTimeout(100);
  }
  assert.ok(ready, logs);
  const get = await fetch(`${url}/api/data?market=KOSPI&limit=3`);
  assert.equal(get.headers.get("cache-control"), "no-store");
  assert.equal((await get.json()).url, "/api/data?market=KOSPI&limit=3");
  const body = JSON.stringify({ label: "한국어", amount: 1234 });
  const post = await fetch(`${url}/api/data`, { method: "POST", headers: { "Content-Type": "application/json" }, body });
  const echoed = await post.json();
  assert.equal(echoed.method, "POST");
  assert.equal(Buffer.from(echoed.body, "base64").toString(), body);
  const form = new FormData();
  form.append("file", new Blob(["sample workbook bytes"]), "sample.xlsx");
  const upload = await fetch(`${url}/api/upload`, { method: "POST", body: form });
  const uploaded = await upload.json();
  assert.match(uploaded.contentType, /^multipart\/form-data; boundary=/);
  assert.match(Buffer.from(uploaded.body, "base64").toString(), /sample workbook bytes/);
  assert.equal((await fetch(`${url}/api/rejected`)).status, 422);
  assert.equal((await fetch(`${url}/api/empty`, { method: "DELETE" })).status, 204);
  const head = await fetch(`${url}/api/data`, { method: "HEAD" });
  assert.equal(head.status, 200);
  assert.equal(await head.text(), "");
  await new Promise((resolve) => backend.close(resolve));
  assert.equal((await fetch(`${url}/api/data`)).status, 502);
  console.log("PASS: query, JSON POST, multipart upload, upstream 422/204, HEAD, no-store, unavailable backend 502");
} finally {
  child.kill();
  if (backend.listening) await new Promise((resolve) => backend.close(resolve));
}
