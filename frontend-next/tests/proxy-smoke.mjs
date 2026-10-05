import assert from "node:assert/strict";
import { createServer } from "node:http";
import { spawn } from "node:child_process";
import { setTimeout } from "node:timers/promises";

// Exercise the built server against a temporary backend, never production data.
let dashboardVersion = 1;
const serverRequests = [];
const backend = createServer(async (req, res) => {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  if (req.url === "/api/empty") {
    res.writeHead(204).end();
    return;
  }
  res.writeHead(req.url === "/api/rejected" ? 422 : 200, {
    "Content-Type": "application/json",
  });
  if (req.url === "/api/v1/dashboard") {
    serverRequests.push({
      cookie: req.headers.cookie,
      authorization: req.headers.authorization,
    });
    res.end(
      JSON.stringify({
        success: true,
        data: {
          baseDate: "2026-10-05",
          briefingBaseDate: "2026-10-05",
          title: `서버 브리핑 ${dashboardVersion}`,
          summary: "서버 조회 확인",
          accountSummaries: [],
          actionSignals: [],
          marketScore: 55,
          sentimentScore: 50,
          regularBuyTotal: 0,
          additionalBuyTotal: 0,
          briefingArticles: [
            {
              itemCode: "TEST",
              summary: "검증 항목",
              content: "서버에서 조회한 브리핑 본문",
              signalCode: "HOLD",
            },
          ],
        },
      }),
    );
    return;
  }
  if (req.url === "/api/investment/buy-plans/additional/latest") {
    res.end(
      JSON.stringify({
        baseDate: "2026-10-05",
        reserveAmount: 0,
        recommendedTotal: 0,
        usageRate: 0,
        accounts: [],
        candidates: [],
      }),
    );
    return;
  }
  res.end(
    JSON.stringify({
      method: req.method,
      url: req.url,
      contentType: req.headers["content-type"],
      body: Buffer.concat(chunks).toString("base64"),
    }),
  );
});
await new Promise((resolve) => backend.listen(0, "127.0.0.1", resolve));
const reserve = createServer();
await new Promise((resolve) => reserve.listen(0, "127.0.0.1", resolve));
const port = reserve.address().port;
await new Promise((resolve) => reserve.close(resolve));
const child = spawn(process.execPath, [".next/standalone/server.js"], {
  env: {
    ...process.env,
    PORT: String(port),
    HOSTNAME: "127.0.0.1",
    BACKEND_URL: `http://127.0.0.1:${backend.address().port}`,
    NEXT_TELEMETRY_DISABLED: "1",
  },
  stdio: ["ignore", "pipe", "pipe"],
});
let logs = "";
child.stdout.on("data", (data) => {
  logs += data;
});
child.stderr.on("data", (data) => {
  logs += data;
});
const url = `http://127.0.0.1:${port}`;
try {
  let ready = false;
  for (let attempt = 0; attempt < 100; attempt++) {
    try {
      const r = await fetch(`${url}/api/ping`);
      if (r.ok) {
        ready = true;
        break;
      }
    } catch {}
    await setTimeout(100);
  }
  assert.ok(ready, logs);
  // Every menu must render its own route and initial HTML without a CSR bailout.
  const menus = {
    dashboard: "대시보드",
    briefing: "투자 브리핑",
    holdings: "보유종목",
    additional: "추가매수",
    history: "브리핑 이력",
    reference: "기준정보 관리",
    operations: "투자 설정 관리",
    marketadmin: "시장 분석 관리",
    bondyields: "FRED 채권금리",
    volumescreen: "거래량·횡보 검색",
    exchangerates: "환율 차트",
  };
  for (const [path, title] of Object.entries(menus)) {
    const response = await fetch(`${url}/${path}`);
    assert.equal(response.status, 200, path);
    const html = await response.text();
    assert.ok(
      html.includes(`<h1>${title}</h1>`),
      `${path}: initial page heading`,
    );
    assert.ok(
      html.includes(`<title>${title} | FINBRIEF</title>`),
      `${path}: metadata`,
    );
    assert.ok(html.includes(`href="/${path}"`), `${path}: menu link`);
    assert.ok(
      !html.includes("BAILOUT_TO_CLIENT_SIDE_RENDERING"),
      `${path}: server rendering`,
    );
    if (path === "dashboard" || path === "briefing") {
      assert.ok(
        html.includes(
          path === "dashboard"
            ? "서버 브리핑 1"
            : "서버에서 조회한 브리핑 본문",
        ),
        `${path}: server-fetched data`,
      );
    }
    assert.ok(
      !html.includes("NEXT_HTTP_ERROR_FALLBACK"),
      `${path}: no server error`,
    );
  }
  dashboardVersion = 2;
  const refreshed = await fetch(`${url}/dashboard`, {
    headers: { Cookie: "smoke=fixture", Authorization: "Bearer fixture" },
  });
  assert.ok(
    (await refreshed.text()).includes("서버 브리핑 2"),
    "server data is not stale-cached",
  );
  assert.deepEqual(serverRequests.at(-1), {
    cookie: "smoke=fixture",
    authorization: "Bearer fixture",
  });
  const home = await fetch(url, { redirect: "manual" });
  assert.equal(home.status, 307);
  assert.equal(home.headers.get("location"), "/dashboard");
  assert.equal((await fetch(`${url}/unknown-menu`)).status, 404);
  console.log(
    "PASS: 11 menu routes, initial HTML, metadata, root redirect, and 404",
  );
  const get = await fetch(`${url}/api/data?market=KOSPI&limit=3`);
  assert.equal(get.headers.get("cache-control"), "no-store");
  assert.equal((await get.json()).url, "/api/data?market=KOSPI&limit=3");
  const body = JSON.stringify({ label: "한국어", amount: 1234 });
  const post = await fetch(`${url}/api/data`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
  });
  const echoed = await post.json();
  assert.equal(echoed.method, "POST");
  assert.equal(Buffer.from(echoed.body, "base64").toString(), body);
  const form = new FormData();
  form.append("file", new Blob(["sample workbook bytes"]), "sample.xlsx");
  const upload = await fetch(`${url}/api/upload`, {
    method: "POST",
    body: form,
  });
  const uploaded = await upload.json();
  assert.match(uploaded.contentType, /^multipart\/form-data; boundary=/);
  assert.match(
    Buffer.from(uploaded.body, "base64").toString(),
    /sample workbook bytes/,
  );
  assert.equal((await fetch(`${url}/api/rejected`)).status, 422);
  assert.equal(
    (await fetch(`${url}/api/empty`, { method: "DELETE" })).status,
    204,
  );
  const head = await fetch(`${url}/api/data`, { method: "HEAD" });
  assert.equal(head.status, 200);
  assert.equal(await head.text(), "");
  await new Promise((resolve) => backend.close(resolve));
  assert.equal((await fetch(`${url}/api/data`)).status, 502);
  console.log(
    "PASS: query, JSON POST, multipart upload, upstream 422/204, HEAD, no-store, unavailable backend 502",
  );
} finally {
  child.kill();
  if (backend.listening) await new Promise((resolve) => backend.close(resolve));
}
