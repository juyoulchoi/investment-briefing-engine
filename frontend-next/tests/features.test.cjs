const assert = require("node:assert/strict");
const { test } = require("node:test");
const { readFileSync, readdirSync } = require("node:fs");
const path = require("node:path");
const Module = require("node:module");
const ts = require("typescript");

// Compile pure TS modules in memory; no extra runtime dependency or generated files.
const cache = new Map();
function load(relative) {
  const file = path.resolve(__dirname, "..", relative);
  if (cache.has(file)) return cache.get(file).exports;
  const module = new Module(file, moduleParent);
  module.filename = file;
  module.paths = Module._nodeModulePaths(path.dirname(file));
  const originalRequire = module.require.bind(module);
  module.require = (name) =>
    name.startsWith(".")
      ? load(
          path.relative(
            path.resolve(__dirname, ".."),
            path.resolve(path.dirname(file), `${name}.ts`),
          ),
        )
      : originalRequire(name);
  cache.set(file, module);
  module._compile(
    ts.transpileModule(readFileSync(file, "utf8"), {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
      },
    }).outputText,
    file,
  );
  return module.exports;
}
const moduleParent = module;
const holdings = load("src/features/holdings/model.ts");
const operations = load("src/features/operations/model.ts");
const dashboard = load("src/features/dashboard/model.ts");
const additional = load("src/features/additional/model.ts");
const exchange = load("src/features/exchangerates/model.ts");
const row = {
  holdingId: 1,
  holdingQuantity: 2,
  averagePrice: 100,
  wholeSharePurchaseAmount: 180,
  fractionalSharePurchaseAmount: 20,
};
const draft = {
  holdingQuantity: "2",
  averagePrice: "100",
  wholeSharePurchaseAmount: "180",
  fractionalSharePurchaseAmount: "20",
};

test("domestic edits track purchase amounts while overseas edits track average price", () => {
  const priceOnly = { 1: { ...draft, averagePrice: "999" } };
  assert.equal(holdings.getChangedHoldings([row], priceOnly, true).length, 0);
  assert.equal(holdings.getChangedHoldings([row], priceOnly, false).length, 1);
  const amountOnly = { 1: { ...draft, fractionalSharePurchaseAmount: "25" } };
  assert.equal(holdings.getChangedHoldings([row], amountOnly, true).length, 1);
  assert.equal(holdings.getChangedHoldings([row], amountOnly, false).length, 0);
});
test("stopped and paused holdings cannot become batch updates", () => {
  for (const locked of [
    { buyStatus: "STOPPED" },
    { buyStatus: "PAUSED" },
    { userPauseYn: "Y" },
  ]) {
    assert.equal(
      holdings.getChangedHoldings(
        [{ ...row, ...locked }],
        { 1: { ...draft, holdingQuantity: "5" } },
        true,
      ).length,
      0,
    );
  }
});
test("holding payloads retain domestic costs and clear them for non-domestic accounts", () => {
  assert.deepEqual(
    holdings.buildHoldingUpdates([row], { 1: draft }, true)[0].values,
    {
      holdingQuantity: 2,
      averagePrice: 100,
      wholeSharePurchaseAmount: 180,
      fractionalSharePurchaseAmount: 20,
    },
  );
  assert.equal(
    holdings.buildHoldingUpdates([row], { 1: draft }, false)[0].values
      .wholeSharePurchaseAmount,
    null,
  );
  assert.equal(
    holdings.buildHoldingUpdates([row], { 1: draft }, false)[0].values
      .fractionalSharePurchaseAmount,
    null,
  );
});
test("invalid edit values are rejected before requests", () => {
  for (const value of ["-1", "NaN", "Infinity"]) {
    assert.ok(
      holdings.validateHoldingDrafts(
        [row],
        { 1: { ...draft, holdingQuantity: value } },
        true,
      ),
    );
  }
  assert.equal(holdings.validateHoldingDrafts([row], { 1: draft }, true), null);
});
test("regular-buy cycle changes clear only the incompatible schedule", () => {
  const value = {
    appliedCycle: "WEEKLY",
    appliedWeekDays: "MON,WED",
    appliedMonthDays: "5,20",
  };
  assert.deepEqual(operations.normalizedRegularBuyForm("regular-buys", value), {
    ...value,
    appliedMonthDays: null,
  });
  assert.equal(
    operations.normalizedRegularBuyForm("regular-buys", {
      ...value,
      appliedCycle: "MONTHLY",
    }).appliedWeekDays,
    null,
  );
  assert.deepEqual(
    operations.normalizedRegularBuyForm("holdings", value),
    value,
  );
  for (const paused of [{ buyStatus: "STOPPED" }, { userPauseYn: "Y" }]) {
    const result = operations.normalizedRegularBuyForm("regular-buys", {
      ...value,
      ...paused,
    });
    assert.equal(result.appliedWeekDays, null);
    assert.equal(result.appliedMonthDays, null);
  }
  assert.equal(
    value.appliedMonthDays,
    "5,20",
    "original form remains unchanged",
  );
});
test("dashboard totals and overseas display returns keep separate currencies", () => {
  const result = dashboard.buildDashboardModel({
    accountSummaries: [
      {
        accountType: "OVERSEAS",
        totalAsset: 150000,
        evaluationAmount: 130000,
        costAmount: 100000,
        cashAmount: 20000,
        displayEvaluationAmount: 100,
        displayCostAmount: 80,
        holdingCount: 1,
        priceBaseDate: "2026-10-02",
      },
    ],
  });
  assert.equal(result.total, 150000);
  assert.equal(result.totalRate, 30);
  assert.equal(result.rateText(result.assetAccounts[0]), "+25.0%");
  assert.equal(dashboard.buildDashboardModel(null).totalRate, 0);
});
test("additional-buy account filtering preserves provider recommendations", () => {
  const data = {
    accounts: [{ accountType: "DOMESTIC", recommendedTotal: 777 }],
    candidates: [
      { accountType: "DOMESTIC", recommendedAmount: 777 },
      { accountType: "OVERSEAS", recommendedAmount: 999 },
    ],
  };
  const result = additional.selectAdditionalAccount(data, "DOMESTIC");
  assert.equal(result.rows.length, 1);
  assert.equal(result.accountSummary.recommendedTotal, 777);
  assert.equal(
    additional.selectAdditionalAccount(data, "ISA").accountSummary
      .recommendedTotal,
    0,
  );
});
test("exchange geometry handles empty and single-point series without invalid coordinates", () => {
  assert.equal(exchange.buildExchangeChart([]), null);
  const chart = exchange.buildExchangeChart([
    { base_date: "2026-10-05", exchange_rate: 1400 },
  ]);
  assert.ok(!/NaN|Infinity/.test(chart.points));
  assert.equal(chart.xTicks.length, 1);
});
test("page views cannot own HTTP requests or effect-driven data loading", () => {
  const root = path.resolve(__dirname, "../src/features");
  for (const feature of readdirSync(root)) {
    for (const file of readdirSync(path.join(root, feature)).filter((name) =>
      name.endsWith("View.tsx"),
    )) {
      const code = readFileSync(path.join(root, feature, file), "utf8");
      assert.doesNotMatch(
        code,
        /\bfetch\s*\(|\buseEffect\s*\(|["'`]\/api\//,
        `${feature}/${file}`,
      );
      assert.doesNotMatch(code, /from ["']\.\/api["']/, `${feature}/${file}`);
    }
  }
});
test("holdings transport preserves endpoints, verbs and batch payload", async () => {
  const api = load("src/features/holdings/api.ts");
  const previous = global.fetch;
  const calls = [];
  global.fetch = async (url, init) => {
    calls.push({ url, init });
    return new Response(JSON.stringify({ success: true, data: [] }), {
      status: 200,
    });
  };
  try {
    await api.getHoldings();
    await api.createHolding({ stockId: 11 });
    const updates = holdings.buildHoldingUpdates([row], { 1: draft }, true);
    await api.updateAccountHoldings(7, updates);
    assert.equal(calls[0].url, "/api/v1/admin/operations/holdings");
    assert.equal(calls[1].init.method, "POST");
    assert.equal(calls[2].url, "/api/v1/accounts/7/holdings");
    assert.equal(calls[2].init.method, "PATCH");
    assert.deepEqual(JSON.parse(calls[2].init.body), { updates });
  } finally {
    global.fetch = previous;
  }
});
