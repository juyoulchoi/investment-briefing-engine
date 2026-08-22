import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";
import ReferenceAdmin from "./ReferenceAdmin";
import OperationsAdmin from "./OperationsAdmin";
import MarketAnalysisAdmin from "./MarketAnalysisAdmin";
import BondYieldPage from "./BondYieldPage";
type Page =
  | "dashboard"
  | "briefing"
  | "holdings"
  | "additional"
  | "history"
  | "reference"
  | "operations"
  | "marketadmin"
  | "bondyields";
const nav: [Page, string, string][] = [
  ["dashboard", "대시보드", "⌂"],
  ["briefing", "투자 브리핑", "▤"],
  ["holdings", "보유종목", "◇"],
  ["additional", "추가매수", "+"],
  ["history", "브리핑 이력", "◷"],
  ["reference", "기준정보 관리", "⚙"],
  ["operations", "투자 설정 관리", "⌘"],
  ["marketadmin", "시장 분석 관리", "◉"],
  ["bondyields", "FRED 채권금리", "％"],
];
const pageStorageKey = "investment-briefing-page";
const savedPage = () => {
  const value = localStorage.getItem(pageStorageKey);
  return nav.some(([page]) => page === value) ? (value as Page) : "dashboard";
};
type Account = {
  accountId: number;
  accountType: "DOMESTIC" | "OVERSEAS" | "ISA" | "PENSION";
  cashAmount: number | null;
  reservedCashAmount: number | null;
};
type Holding = {
  holdingId: number;
  accountId: number;
  accountName: string;
  accountType: Account["accountType"];
  stockId: number;
  stockCode: string;
  stockName: string;
  holdingQuantity: number;
  averagePrice: number;
  wholeSharePurchaseAmount: number | null;
  fractionalSharePurchaseAmount: number | null;
  currentPrice: number;
  evaluationAmount: number;
  profitLossRate: number;
  targetWeight: number | null;
  currentWeight: number | null;
  weightStatus: string | null;
  weightStatusName: string | null;
  holdingStatus: string;
  buyStatus?: "ACTIVE" | "PAUSED" | "STOPPED" | null;
  userPauseYn?: "Y" | "N";
};
type DashboardData = {
  baseDate: string;
  briefingBaseDate: string | null;
  marketScore: number;
  marketRegime: string;
  sentimentScore: number;
  sentimentPhase: string;
  riskGrade: string;
  overallSignal: string;
  regularBuyTotal: number;
  additionalBuyTotal: number;
  title: string | null;
  summary: string | null;
  body: string | null;
  accountSummaries: {
    accountType: Account["accountType"];
    totalAsset: number;
    evaluationAmount: number;
    costAmount: number;
    cashAmount: number;
    holdingCount: number;
    priceBaseDate: string | null;
    currencyCode: string;
    displayTotalAsset: number;
    displayEvaluationAmount: number;
    displayCostAmount: number;
    displayCashAmount: number;
  }[];
  actionSignals: {
    accountType: Account["accountType"];
    stockCode: string;
    stockName: string;
    actionSignal: string;
    recommendedAmount: number | null;
    reason: string;
  }[];
  briefingArticles: {
    itemCode: string;
    summary: string;
    content: string;
    signalCode: string;
  }[];
};
type PageData<T> = { content: T[] };
type ApiResult<T> = { success: boolean; data: T; error?: { message?: string } };
type CommonCode = {
  code: string;
  name: string;
  description: string | null;
  displayOrder: number;
};
type AccountTypeContextValue = {
  accountTypes: Account["accountType"][];
  accountLabel: (type: Account["accountType"]) => string;
  dashboardLabel: (code: string) => string;
};
const accountTypeContext = createContext<AccountTypeContextValue>({
  accountTypes: [],
  accountLabel: (type) => type,
  dashboardLabel: (code) => code,
});
const isAccountType = (code: string): code is Account["accountType"] =>
  ["DOMESTIC", "OVERSEAS", "ISA", "PENSION"].includes(code);
const useAccountTypes = () => useContext(accountTypeContext);
const accountTabOrder: Account["accountType"][] = [
  "DOMESTIC",
  "OVERSEAS",
  "ISA",
  "PENSION",
];
const orderedAccountTypes = (types: Account["accountType"][]) =>
  accountTabOrder.filter((type) => types.includes(type));
const accountTabStorageKey = "investment-briefing-account-tab";
const savedAccountTab = () => {
  const value = localStorage.getItem(accountTabStorageKey);
  return value && isAccountType(value) ? value : "DOMESTIC";
};
function AccountTypeProvider({ children }: { children: React.ReactNode }) {
  const [accountCodes, setAccountCodes] = useState<CommonCode[]>([]);
  const [dashboardCodes, setDashboardCodes] = useState<CommonCode[]>([]);
  useEffect(() => {
    const loadCodes = async (group: string) => {
      const response = await fetch(`/api/v1/common-codes/${group}`);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return response.json() as Promise<CommonCode[]>;
    };
    loadCodes("ACCOUNT_TYPE")
      .then(setAccountCodes)
      .catch(() => setAccountCodes([]));
    loadCodes("DASHBOARD_LABEL")
      .then(setDashboardCodes)
      .catch(() => setDashboardCodes([]));
  }, []);
  const value = useMemo<AccountTypeContextValue>(() => {
    const activeCodes = accountCodes.filter((code) => isAccountType(code.code));
    const labels = Object.fromEntries(
      activeCodes.map((code) => [code.code, code.name]),
    ) as Partial<Record<Account["accountType"], string>>;
    const dashboardLabels = Object.fromEntries(
      dashboardCodes.map((code) => [code.code, code.name]),
    ) as Record<string, string>;
    return {
      accountTypes: activeCodes.map(
        (code) => code.code as Account["accountType"],
      ),
      accountLabel: (type) => labels[type] ?? type,
      dashboardLabel: (code) => dashboardLabels[code] ?? code,
    };
  }, [accountCodes, dashboardCodes]);
  return (
    <accountTypeContext.Provider value={value}>
      {children}
    </accountTypeContext.Provider>
  );
}
async function api<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const body = (await response.json()) as ApiResult<T>;
  if (!body.success)
    throw new Error(body.error?.message || "데이터 조회에 실패했습니다.");
  return body.data;
}
const won = (n: number) => `${n.toLocaleString("ko-KR")}원`;
const amount = (n: number, overseas = false) =>
  n.toLocaleString(overseas ? "en-US" : "ko-KR", {
    minimumFractionDigits: overseas ? 2 : 0,
    maximumFractionDigits: overseas ? 2 : 0,
  });
const usd = (n: number) => `$${amount(n, true)}`;
const todayLabel = new Intl.DateTimeFormat("ko-KR", {
  dateStyle: "full",
}).format(new Date());
function App() {
  const [page, setPage] = useState<Page>(savedPage),
    [brief, setBrief] = useState("오늘"),
    [history, setHistory] = useState("일일"),
    [toast, setToast] = useState(""),
    [refreshing, setRefreshing] = useState(false),
    [contentVersion, setContentVersion] = useState(0);
  const move = (p: Page) => {
      localStorage.setItem(pageStorageKey, p);
      setPage(p);
      scrollTo({ top: 0, behavior: "smooth" });
    },
    notify = (m: string) => {
      setToast(m);
      setTimeout(() => setToast(""), 2200);
    },
    refreshMarketData = async () => {
      setRefreshing(true);
      try {
        const response = await fetch("/api/market-data/holdings/refresh", {
          method: "POST",
        });
        const result = (await response.json()) as {
          success: boolean;
          krxReceivedCounts: Record<string, number>;
          overseasRequestedCount: number;
          overseasSuccessCount: number;
          completedSteps: string[];
          failures: string[];
        };
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        setContentVersion((v) => v + 1);
        const domestic = Object.values(result.krxReceivedCounts || {}).reduce(
          (sum, count) => sum + count,
          0,
        );
        notify(
          result.success
            ? `1~10단계 ${result.completedSteps.length}개 완료 · 국내·ETF ${domestic.toLocaleString("ko-KR")}건 · 해외 ${result.overseasSuccessCount}/${result.overseasRequestedCount}개`
            : `일부 갱신 실패: ${result.failures.join(" · ")}`,
        );
      } catch (e) {
        notify(
          e instanceof Error ? e.message : "시장 데이터 갱신에 실패했습니다.",
        );
      } finally {
        setRefreshing(false);
      }
    };
  return (
    <div className="shell">
      <aside>
        <button className="brand" onClick={() => move("dashboard")}>
          <b>F</b>
          <span>
            <strong>FINBRIEF</strong>
            <small>Investment OS</small>
          </span>
        </button>
        <nav>
          <p>OVERVIEW</p>
          {nav.slice(0, 1).map((x) => (
            <Nav key={x[0]} x={x} active={page === x[0]} go={move} />
          ))}
          <p>INVEST</p>
          {nav.slice(1, 4).map((x) => (
            <Nav key={x[0]} x={x} active={page === x[0]} go={move} />
          ))}
          <p>RECORDS</p>
          {nav.slice(4).map((x) => (
            <Nav key={x[0]} x={x} active={page === x[0]} go={move} />
          ))}
        </nav>
        <div className="user">
          <b>민</b>
          <span>
            <strong>김투자</strong>
            <small>개인 포트폴리오</small>
          </span>
        </div>
      </aside>
      <main key={contentVersion}>
        <header className="top">
          <div>
            <h1>{nav.find((x) => x[0] === page)?.[1]}</h1>
          </div>
          <div>
            <span className="latest">● 데이터 최신</span>
            <button
              className="primary"
              disabled={refreshing}
              onClick={refreshMarketData}
            >
              {refreshing ? "갱신 중..." : "↻ 브리핑 갱신"}
            </button>
          </div>
        </header>
        {page === "dashboard" && <Dashboard go={move} />}{" "}
        {page === "briefing" && <Briefing />}{" "}
        {page === "holdings" && <Holdings />}{" "}
        {page === "additional" && <Additional />}{" "}
        {page === "history" && <History period={history} set={setHistory} />}{" "}
        {page === "reference" && <ReferenceAdmin notify={notify} />}{" "}
        {page === "operations" && <OperationsAdmin notify={notify} />}{" "}
        {page === "marketadmin" && <MarketAnalysisAdmin notify={notify} />}{" "}
        {page === "bondyields" && <BondYieldPage notify={notify} />}
      </main>
      <nav className="mobile" aria-label="모바일 전체 메뉴">
        {nav.map((x) => (
          <button
            key={x[0]}
            className={page === x[0] ? "active" : ""}
            onClick={() => move(x[0])}
          >
            <b>{x[2]}</b>
            {x[1]}
          </button>
        ))}
      </nav>
      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
function Nav({
  x,
  active,
  go,
}: {
  x: [Page, string, string];
  active: boolean;
  go: (p: Page) => void;
}) {
  return (
    <button
      className={`nav ${active ? "active" : ""}`}
      onClick={() => go(x[0])}
    >
      <b>{x[2]}</b>
      {x[1]}
    </button>
  );
}
function Dashboard({ go }: { go: (p: Page) => void }) {
  const { accountLabel, dashboardLabel } = useAccountTypes();
  type AssetAccount = {
    type: Account["accountType"];
    value: number;
    evaluation: number;
    cost: number;
    cash: number;
    displayValue: number;
    displayEvaluation: number;
    displayCost: number;
    displayCash: number;
    holdingCount: number;
    priceBaseDate: string | null;
  };
  const [assetAccounts, setAssetAccounts] = useState<AssetAccount[]>([]),
    [assetLoading, setAssetLoading] = useState(true),
    [assetError, setAssetError] = useState("");
  const [dashboard, setDashboard] = useState<DashboardData | null>(null),
    [dashboardLoading, setDashboardLoading] = useState(true),
    [dashboardError, setDashboardError] = useState("");
  useEffect(() => {
    let alive = true;
    api<DashboardData | null>("/api/v1/dashboard")
      .then((data) => {
        if (alive) {
          setDashboard(data);
          setAssetAccounts(
            data?.accountSummaries.map((a) => ({
              type: a.accountType,
              value: Number(a.totalAsset || 0),
              evaluation: Number(a.evaluationAmount || 0),
              cost: Number(a.costAmount || 0),
              cash: Number(a.cashAmount || 0),
              displayValue: Number(a.displayTotalAsset || 0),
              displayEvaluation: Number(a.displayEvaluationAmount || 0),
              displayCost: Number(a.displayCostAmount || 0),
              displayCash: Number(a.displayCashAmount || 0),
              holdingCount: Number(a.holdingCount || 0),
              priceBaseDate: a.priceBaseDate,
            })) || [],
          );
        }
      })
      .catch((error) => {
        if (alive) {
          const message =
            error instanceof Error
              ? error.message
              : "대시보드 최신 데이터를 불러오지 못했습니다.";
          setDashboardError(message);
          setAssetError(message);
        }
      })
      .finally(() => {
        if (alive) {
          setDashboardLoading(false);
          setAssetLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, []);
  const total = assetAccounts.reduce((sum, a) => sum + a.value, 0),
    totalEvaluation = assetAccounts.reduce((sum, a) => sum + a.evaluation, 0),
    totalCost = assetAccounts.reduce((sum, a) => sum + a.cost, 0),
    totalCash = assetAccounts.reduce((sum, a) => sum + a.cash, 0),
    totalRate =
      totalCost > 0 ? ((totalEvaluation - totalCost) / totalCost) * 100 : 0,
    latestAssetDate = assetAccounts
      .map((a) => a.priceBaseDate)
      .filter(Boolean)
      .sort()
      .at(-1),
    cashRate = total > 0 ? (totalCash / total) * 100 : 0;
  const rateText = (a: AssetAccount) => {
    const evaluation =
        a.type === "OVERSEAS" ? a.displayEvaluation : a.evaluation,
      cost = a.type === "OVERSEAS" ? a.displayCost : a.cost;
    return a.holdingCount === 0
      ? "대기"
      : `${evaluation - cost >= 0 ? "+" : ""}${(cost > 0 ? ((evaluation - cost) / cost) * 100 : 0).toFixed(1)}%`;
  };
  return (
    <div className="page">
      {dashboardError && <div className="batch-error">{dashboardError}</div>}
      <section className="hero">
        <div>
          <h2>
            {dashboardLoading
              ? "최신 투자 판단을 불러오는 중입니다."
              : dashboard?.title || "최신 투자 판단 데이터가 없습니다."}
          </h2>
          <p>
            {dashboard?.summary ||
              "브리핑 생성이 완료되면 최신 시장 판단이 표시됩니다."}
          </p>
        </div>
        <div className="ring">
          <strong>
            {dashboard ? Math.round(Number(dashboard.marketScore || 0)) : "-"}
          </strong>
          <small>시장점수</small>
        </div>
      </section>
      <section className="metrics">
        <Metric
          t="시장심리"
          v={dashboard ? dashboardLabel(dashboard.sentimentPhase) : "-"}
          d={
            dashboard
              ? `심리점수 ${Number(dashboard.sentimentScore || 0).toFixed(1)}`
              : "데이터 없음"
          }
          i="◒"
        />
        <Metric
          t="시장국면"
          v={dashboard ? dashboardLabel(dashboard.marketRegime) : "-"}
          d={
            dashboard
              ? `위험등급 ${dashboardLabel(dashboard.riskGrade)}`
              : "데이터 없음"
          }
          i="↗"
        />
        <Metric
          t="행동신호"
          v={dashboard ? dashboardLabel(dashboard.overallSignal) : "-"}
          d={dashboard ? `기준일 ${dashboard.baseDate}` : "데이터 없음"}
          i="⚑"
        />
        <Metric
          t="현금비중"
          v={assetLoading ? "-" : `${cashRate.toFixed(1)}%`}
          d={assetLoading ? "계산 중" : `현금·대기현금 ${won(totalCash)}`}
          i="₩"
        />
      </section>
      <div className="grid">
        <section className="card">
          <Head
            k="MY ASSETS"
            t={`계좌 현황${latestAssetDate ? ` · ${latestAssetDate} 기준` : ""}`}
            a="전체보기"
            click={() => go("holdings")}
          />
          {assetLoading ? (
            <div className="data-state">계좌 현황을 불러오는 중입니다.</div>
          ) : assetError ? (
            <div className="data-state error">{assetError}</div>
          ) : (
            <>
              <div className="total">
                <span>
                  총 자산<strong>₩ {total.toLocaleString("ko-KR")}</strong>
                </span>
                <b className={totalRate >= 0 ? "pos" : "neg"}>
                  {totalRate >= 0 ? "+" : ""}
                  {totalRate.toFixed(1)}%<small>총 수익률</small>
                </b>
              </div>
              <div className="alloc">
                {assetAccounts.map((a) => (
                  <i
                    key={a.type}
                    style={{
                      width: `${total > 0 ? (a.value / total) * 100 : 25}%`,
                    }}
                  />
                ))}
              </div>
              <div className="accounts">
                {assetAccounts.map((a) => (
                  <Account
                    key={a.type}
                    n={accountLabel(a.type)}
                    v={
                      a.type === "OVERSEAS" ? usd(a.displayValue) : won(a.value)
                    }
                    p={`${(total > 0 ? (a.value / total) * 100 : 0).toFixed(1)}%`}
                    g={rateText(a)}
                  />
                ))}
              </div>
            </>
          )}
        </section>
        <section className="card">
          <Head
            k="ACTION SIGNAL"
            t="오늘의 행동신호"
            a="상세보기"
            click={() => go("additional")}
          />
          {dashboardLoading ? (
            <div className="data-state">행동신호를 불러오는 중입니다.</div>
          ) : dashboard?.actionSignals?.length ? (
            dashboard.actionSignals.map((signal) => (
              <Signal
                key={`${signal.stockCode}-${signal.actionSignal}`}
                tag={signal.actionSignal === "INCREASE" ? "BUY" : "●"}
                n={`${signal.stockName} ${dashboardLabel(signal.actionSignal)}`}
                v={
                  signal.accountType === "OVERSEAS"
                    ? usd(Number(signal.recommendedAmount || 0))
                    : won(Number(signal.recommendedAmount || 0))
                }
              />
            ))
          ) : (
            <div className="data-state">최신 행동신호가 없습니다.</div>
          )}
          <button className="full" onClick={() => go("operations")}>
            오늘 추천매수 총{" "}
            {won(
              Number(dashboard?.regularBuyTotal || 0) +
                Number(dashboard?.additionalBuyTotal || 0),
            )}
            <span>계획 확인 →</span>
          </button>
        </section>
      </div>
      <section className="card briefing">
        <Head
          k="DAILY BRIEFING"
          t="오늘 브리핑"
          a="전체 브리핑 읽기"
          click={() => go("briefing")}
        />
        <div className="columns">
          {dashboardLoading ? (
            <div className="data-state">최신 브리핑을 불러오는 중입니다.</div>
          ) : dashboard?.briefingArticles?.length ? (
            dashboard.briefingArticles
              .slice(0, 3)
              .map((article, index) => (
                <Article
                  key={article.itemCode}
                  no={`0${index + 1}`}
                  t={article.summary}
                />
              ))
          ) : (
            <div className="data-state">발행된 최신 브리핑이 없습니다.</div>
          )}
        </div>
      </section>
    </div>
  );
}
const Head = ({
  k: _k,
  t,
  a,
  click,
}: {
  k: string;
  t: string;
  a: string;
  click?: () => void;
}) => (
  <header className="head">
    <div>
      <h3>{t}</h3>
    </div>
    <button type="button" onClick={click}>
      {a} →
    </button>
  </header>
);
function Metric({
  t,
  v,
  d,
  i,
}: {
  t: string;
  v: string;
  d: string;
  i: string;
}) {
  return (
    <article className="metric">
      <b>{i}</b>
      <span>
        <small>{t}</small>
        <strong>{v}</strong>
        <em>{d}</em>
      </span>
    </article>
  );
}
function Account({
  n,
  v,
  p,
  g,
}: {
  n: string;
  v: string;
  p: string;
  g: string;
}) {
  return (
    <div>
      <i />
      <b>{n}</b>
      <span>{v}</span>
      <small>{p}</small>
      <em className={g[0] === "+" ? "pos" : g[0] === "-" ? "neg" : ""}>{g}</em>
    </div>
  );
}
function Signal({ tag, n, v }: { tag: string; n: string; v: string }) {
  return (
    <div className="signal">
      <b>{tag}</b>
      <span>
        <strong>{n}</strong>
      </span>
      <em>{v}</em>
    </div>
  );
}
function Article({ no, t }: { no: string; t: string }) {
  return (
    <article>
      <b>{no}</b>
      <span>
        <strong>{t}</strong>
      </span>
    </article>
  );
}
function Tabs({
  vals,
  active,
  set,
}: {
  vals: string[];
  active: string;
  set: (s: string) => void;
}) {
  return (
    <div className="tabs">
      {vals.map((x) => (
        <button
          key={x}
          className={x === active ? "active" : ""}
          onClick={() => set(x)}
        >
          {x}
        </button>
      ))}
    </div>
  );
}
function Briefing() {
  const [data, setData] = useState<DashboardData | null>(null),
    [loading, setLoading] = useState(true),
    [error, setError] = useState("");
  useEffect(() => {
    let alive = true;
    api<DashboardData | null>("/api/v1/dashboard")
      .then((value) => {
        if (alive) setData(value);
      })
      .catch((e) => {
        if (alive)
          setError(
            e instanceof Error
              ? e.message
              : "최신 투자 브리핑을 불러오지 못했습니다.",
          );
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, []);
  if (loading)
    return (
      <div className="page">
        <section className="card data-state">
          최신 투자 브리핑을 불러오는 중입니다.
        </section>
      </div>
    );
  if (error)
    return (
      <div className="page">
        <section className="card data-state error">{error}</section>
      </div>
    );
  if (!data?.title || !data.briefingArticles?.length)
    return (
      <div className="page">
        <section className="card data-state">
          발행된 최신 투자 브리핑이 없습니다.
        </section>
      </div>
    );
  return (
    <div className="page">
      <section className="briefHero">
        <h2>{data.briefingBaseDate}</h2>
        <p>
          {data.summary || "최신 투자 판단을 바탕으로 생성된 브리핑입니다."}
        </p>
        <div>
          {data.briefingArticles.slice(0, 3).map((item, i) => (
            <article key={item.itemCode}>
              <b>{String(i + 1).padStart(2, "0")}</b>
              {item.summary}
            </article>
          ))}
        </div>
      </section>
      <section className="card briefing-sections">
        {data.briefingArticles.map((item, i) => (
          <article key={item.itemCode}>
            <b>{String(i + 1).padStart(2, "0")}</b>
            <div>
              <h3>{item.summary}</h3>
              <p>{item.content}</p>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
function Holdings() {
  const { accountTypes, accountLabel } = useAccountTypes();
  type Draft = {
    holdingQuantity: string;
    averagePrice: string;
    wholeSharePurchaseAmount: string;
    fractionalSharePurchaseAmount: string;
  };
  type AdminAccount = {
    accountId: number;
    accountType: Account["accountType"];
  };
  type AdminStock = {
    stockId: number;
    stockCode: string;
    stockName: string;
    marketCode: string;
    useYn: string;
  };
  const [holdings, setHoldings] = useState<Holding[]>([]),
    [selected, setSelected] = useState<Account["accountType"]>(savedAccountTab),
    [sort, setSort] = useState("profitAsc"),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [editMode, setEditMode] = useState(false),
    [drafts, setDrafts] = useState<Record<number, Draft>>({}),
    [saving, setSaving] = useState(false),
    [saveError, setSaveError] = useState(""),
    [adminAccounts, setAdminAccounts] = useState<AdminAccount[]>([]),
    [adminStocks, setAdminStocks] = useState<AdminStock[]>([]),
    [createOpen, setCreateOpen] = useState(false),
    [createSaving, setCreateSaving] = useState(false),
    [createError, setCreateError] = useState(""),
    [newHolding, setNewHolding] = useState<Record<string, any>>({
      holdingQuantity: 0,
      averagePrice: 0,
      exchangeRate: 1,
      targetWeight: null,
      holdingStatus: "ACTIVE",
      useYn: "Y",
      memo: "",
    });
  useEffect(() => {
    Promise.all([
      api<AdminAccount[]>("/api/v1/admin/reference/accounts"),
      api<AdminStock[]>("/api/v1/admin/reference/stocks"),
    ])
      .then(([a, s]) => {
        setAdminAccounts(a);
        setAdminStocks(s);
      })
      .catch(() => {});
  }, []);
  useEffect(() => {
    let alive = true;
    setLoading(true);
    api<(Holding & { useYn?: string })[]>("/api/v1/admin/operations/holdings")
      .then((rows) => {
        if (alive)
          setHoldings(
            rows
              .filter((h) => h.useYn !== "N")
              .map((h) => ({
                ...h,
                accountName: accountLabel(h.accountType),
                currentWeight: h.currentWeight ?? null,
                weightStatus: h.weightStatus ?? null,
              })),
          );
      })
      .catch((e) => {
        if (alive)
          setError(
            e instanceof Error ? e.message : "보유종목을 불러오지 못했습니다.",
          );
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, []);
  const isOverseas = selected === "OVERSEAS",
    isDomestic = selected === "DOMESTIC",
    isWholeWonAccount = selected === "ISA" || selected === "PENSION",
    holdingEvaluation = (h: Holding) =>
      isOverseas
        ? Number(h.currentPrice || 0) * Number(h.holdingQuantity || 0)
        : Number(h.evaluationAmount || 0),
    formatMoney = (n: number) => amount(n, isOverseas),
    formatHoldingQuantity = (n: number) =>
      n.toLocaleString(
        "ko-KR",
        isDomestic
          ? { minimumFractionDigits: 6, maximumFractionDigits: 6 }
          : undefined,
      );
  const rows = holdings
      .filter((h) => h.accountType === selected)
      .sort((a, b) =>
        sort === "profitAsc"
          ? Number(a.profitLossRate) - Number(b.profitLossRate)
          : sort === "profitDesc"
            ? Number(b.profitLossRate) - Number(a.profitLossRate)
            : holdingEvaluation(b) - holdingEvaluation(a),
      ),
    evaluation = rows.reduce((sum, h) => sum + holdingEvaluation(h), 0),
    cost = rows.reduce(
      (sum, h) =>
        sum + Number(h.averagePrice || 0) * Number(h.holdingQuantity || 0),
      0,
    ),
    profit = evaluation - cost;
  const isBuyLocked = (h: Holding) =>
    h.userPauseYn === "Y" ||
    h.buyStatus === "STOPPED" ||
    h.buyStatus === "PAUSED";
  const rowClass = (h: Holding) =>
    [
      h.userPauseYn === "Y"
        ? "holding-user-paused-row"
        : h.buyStatus === "STOPPED"
          ? "holding-buy-stopped-row"
          : h.buyStatus === "PAUSED"
            ? "holding-buy-paused-row"
            : "",
      editMode && changed.some((x) => x.holdingId === h.holdingId)
        ? "changed-row"
        : "",
    ]
      .filter(Boolean)
      .join(" ");
  const beginEdit = () => {
    setDrafts(
      Object.fromEntries(
        rows.map((h) => [
          h.holdingId,
          {
            holdingQuantity: String(h.holdingQuantity),
            averagePrice: String(h.averagePrice),
            wholeSharePurchaseAmount: String(h.wholeSharePurchaseAmount ?? 0),
            fractionalSharePurchaseAmount: String(
              h.fractionalSharePurchaseAmount ?? 0,
            ),
          },
        ]),
      ),
    );
    setSaveError("");
    setEditMode(true);
  };
  const cancelEdit = () => {
    if (!saving) {
      setEditMode(false);
      setDrafts({});
      setSaveError("");
    }
  };
  const selectAccount = (type: Account["accountType"]) => {
    cancelEdit();
    localStorage.setItem(accountTabStorageKey, type);
    setSelected(type);
  };
  const updateDraft = (id: number, field: keyof Draft, value: string) =>
    setDrafts((current) => ({
      ...current,
      [id]: { ...current[id], [field]: value },
    }));
  const changed = rows.filter(
    (h) =>
      !isBuyLocked(h) &&
      drafts[h.holdingId] &&
      (Number(drafts[h.holdingId].holdingQuantity) !==
        Number(h.holdingQuantity) ||
        (!isDomestic &&
          Number(drafts[h.holdingId].averagePrice) !==
            Number(h.averagePrice)) ||
        (isDomestic &&
          (Number(drafts[h.holdingId].wholeSharePurchaseAmount) !==
            Number(h.wholeSharePurchaseAmount) ||
            Number(drafts[h.holdingId].fractionalSharePurchaseAmount) !==
              Number(h.fractionalSharePurchaseAmount)))),
  );
  const openCreate = () => {
    const account = adminAccounts.find((a) => a.accountType === selected);
    setNewHolding({
      accountId: account?.accountId,
      stockId: "",
      holdingQuantity: 0,
      averagePrice: 0,
      wholeSharePurchaseAmount: selected === "DOMESTIC" ? 0 : null,
      fractionalSharePurchaseAmount: selected === "DOMESTIC" ? 0 : null,
      exchangeRate: 1,
      targetWeight: null,
      holdingStatus: "ACTIVE",
      useYn: "Y",
      memo: "",
    });
    setCreateError("");
    setCreateOpen(true);
  };
  const saveCreate = async () => {
    if (!newHolding.accountId || !newHolding.stockId) {
      setCreateError("계좌와 종목을 선택하세요.");
      return;
    }
    setCreateSaving(true);
    setCreateError("");
    try {
      const response = await fetch("/api/v1/admin/operations/holdings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newHolding),
      });
      const body = (await response.json()) as ApiResult<any>;
      if (!response.ok || !body.success)
        throw new Error(body.error?.message || `HTTP ${response.status}`);
      const r = body.data;
      setHoldings((current) => [
        ...current,
        {
          holdingId: r.holdingId,
          accountId: r.accountId,
          accountName: accountLabel(r.accountType as Account["accountType"]),
          accountType: r.accountType,
          stockId: r.stockId,
          stockCode: r.stockCode,
          stockName: r.stockName,
          holdingQuantity: Number(r.holdingQuantity || 0),
          averagePrice: Number(r.averagePrice || 0),
          wholeSharePurchaseAmount: r.wholeSharePurchaseAmount ?? null,
          fractionalSharePurchaseAmount:
            r.fractionalSharePurchaseAmount ?? null,
          currentPrice: Number(r.currentPrice || 0),
          evaluationAmount: Number(r.evaluationAmount || 0),
          profitLossRate: Number(r.profitLossRate || 0),
          targetWeight: r.targetWeight,
          currentWeight: r.currentWeight ?? null,
          weightStatus: r.weightStatus ?? null,
          weightStatusName: r.weightStatusName ?? null,
          holdingStatus: r.holdingStatus,
        },
      ]);
      setCreateOpen(false);
    } catch (e) {
      setCreateError(
        e instanceof Error ? e.message : "보유종목을 등록하지 못했습니다.",
      );
    } finally {
      setCreateSaving(false);
    }
  };
  const saveAccount = async () => {
    if (!changed.length) {
      cancelEdit();
      return;
    }
    for (const h of changed) {
      const d = drafts[h.holdingId],
        values = isDomestic
          ? [
              d.holdingQuantity,
              d.wholeSharePurchaseAmount,
              d.fractionalSharePurchaseAmount,
            ]
          : [d.holdingQuantity, d.averagePrice];
      if (values.some((v) => !Number.isFinite(Number(v)) || Number(v) < 0)) {
        setSaveError(
          isDomestic
            ? "보유수량과 정수주·소수점주 매입금액은 0 이상의 숫자로 입력하세요."
            : "보유수량과 평단가는 0 이상의 숫자로 입력하세요.",
        );
        return;
      }
    }
    setSaving(true);
    setSaveError("");
    try {
      const accountId = rows[0]?.accountId;
      if (!accountId) throw new Error("계좌를 찾을 수 없습니다.");
      const response = await fetch(`/api/v1/accounts/${accountId}/holdings`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          updates: changed.map((h) => ({
            holdingId: h.holdingId,
            values: {
              holdingQuantity: Number(drafts[h.holdingId].holdingQuantity),
              averagePrice: Number(drafts[h.holdingId].averagePrice),
              wholeSharePurchaseAmount: isDomestic
                ? Number(drafts[h.holdingId].wholeSharePurchaseAmount)
                : null,
              fractionalSharePurchaseAmount: isDomestic
                ? Number(drafts[h.holdingId].fractionalSharePurchaseAmount)
                : null,
            },
          })),
        }),
      });
      const body = (await response.json()) as ApiResult<Holding[]>;
      if (!response.ok || !body.success)
        throw new Error(body.error?.message || `HTTP ${response.status}`);
      const updated = new Map(body.data.map((h) => [h.holdingId, h]));
      setHoldings((current) =>
        current.map((h) =>
          updated.has(h.holdingId)
            ? {
                ...h,
                ...updated.get(h.holdingId),
                accountType: h.accountType,
                accountName: h.accountName,
              }
            : h,
        ),
      );
      setEditMode(false);
      setDrafts({});
    } catch (err) {
      setSaveError(
        err instanceof Error ? err.message : "일괄 저장하지 못했습니다.",
      );
    } finally {
      setSaving(false);
    }
  };
  return (
    <div className="page">
      <div className="holding-toolbar">
        <div className="tabs account-tabs">
          {orderedAccountTypes(accountTypes).map((type) => (
            <button
              key={type}
              className={selected === type ? "active" : ""}
              onClick={() => selectAccount(type)}
              disabled={saving}
            >
              {accountLabel(type)}
            </button>
          ))}
        </div>
        <div className="holding-actions">
          <span className="latest">단위: {isOverseas ? "USD" : "원"}</span>
          <label className="sort-select">
            <span>정렬</span>
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value)}
              disabled={editMode}
            >
              <option value="profitAsc">수익률 낮은순</option>
              <option value="profitDesc">수익률 높은순</option>
              <option value="evaluationDesc">평가금액 많은순</option>
            </select>
          </label>
          {editMode ? (
            <>
              <button
                className="cancel-button"
                onClick={cancelEdit}
                disabled={saving}
              >
                취소
              </button>
              <button
                className="primary"
                onClick={saveAccount}
                disabled={saving}
              >
                {saving ? "저장 중..." : `변경 ${changed.length}건 저장`}
              </button>
            </>
          ) : (
            <>
              <button
                className="primary"
                onClick={openCreate}
                disabled={loading || adminAccounts.length === 0}
              >
                + 종목 등록
              </button>
              <button
                className="primary"
                onClick={beginEdit}
                disabled={loading || rows.length === 0}
              >
                수정
              </button>
            </>
          )}
        </div>
      </div>
      {saveError && <div className="batch-error">{saveError}</div>}
      <Summary evaluation={evaluation} profit={profit} format={formatMoney} />
      <section className="card table holdings-table">
        {error && <div className="data-state error">{error}</div>}
        <Table
          heads={[
            "종목 코드",
            "종목명",
            "보유수량",
            ...(isDomestic ? ["정수주 매입금액", "소수점주 매입금액"] : []),
            "평균단가",
            "현재가",
            "평가금액",
            "손익률",
            "목표비중",
            "현재비중",
            "비중상태",
          ]}
        >
          {loading ? (
            <tr>
              <td colSpan={isDomestic ? 12 : 10} className="data-state">
                보유종목을 불러오는 중입니다.
              </td>
            </tr>
          ) : rows.length === 0 && !error ? (
            <tr>
              <td colSpan={isDomestic ? 12 : 10} className="data-state">
                등록된 보유종목이 없습니다.
              </td>
            </tr>
          ) : (
            rows.map((h) => (
              <tr key={h.holdingId} className={rowClass(h)}>
                <td>{h.stockCode}</td>
                <td>
                  <strong>{h.stockName}</strong>
                </td>
                <td>
                  {editMode ? (
                    <input
                      className="inline-edit"
                      type="number"
                      min="0"
                      step="0.00000001"
                      value={drafts[h.holdingId]?.holdingQuantity ?? ""}
                      disabled={isBuyLocked(h)}
                      title={
                        isBuyLocked(h)
                          ? "투자 설정에서 매수가 정지되어 수정할 수 없습니다."
                          : undefined
                      }
                      onChange={(e) =>
                        updateDraft(
                          h.holdingId,
                          "holdingQuantity",
                          e.target.value,
                        )
                      }
                    />
                  ) : (
                    formatHoldingQuantity(Number(h.holdingQuantity))
                  )}
                </td>
                {isDomestic && (
                  <td>
                    {editMode ? (
                      <input
                        className="inline-edit price"
                        type="number"
                        min="0"
                        step="0.0001"
                        value={
                          drafts[h.holdingId]?.wholeSharePurchaseAmount ?? ""
                        }
                        disabled={isBuyLocked(h)}
                        onChange={(e) =>
                          updateDraft(
                            h.holdingId,
                            "wholeSharePurchaseAmount",
                            e.target.value,
                          )
                        }
                      />
                    ) : (
                      formatMoney(Number(h.wholeSharePurchaseAmount ?? 0))
                    )}
                  </td>
                )}
                {isDomestic && (
                  <td>
                    {editMode ? (
                      <input
                        className="inline-edit price"
                        type="number"
                        min="0"
                        step="0.0001"
                        value={
                          drafts[h.holdingId]?.fractionalSharePurchaseAmount ??
                          ""
                        }
                        disabled={isBuyLocked(h)}
                        onChange={(e) =>
                          updateDraft(
                            h.holdingId,
                            "fractionalSharePurchaseAmount",
                            e.target.value,
                          )
                        }
                      />
                    ) : (
                      formatMoney(Number(h.fractionalSharePurchaseAmount ?? 0))
                    )}
                  </td>
                )}
                <td>
                  {editMode && !isDomestic ? (
                    <input
                      className="inline-edit price"
                      type="number"
                      min="0"
                      step={isWholeWonAccount ? "1" : "0.000001"}
                      value={drafts[h.holdingId]?.averagePrice ?? ""}
                      disabled={isBuyLocked(h)}
                      title={
                        isBuyLocked(h)
                          ? "투자 설정에서 매수가 정지되어 수정할 수 없습니다."
                          : undefined
                      }
                      onChange={(e) =>
                        updateDraft(h.holdingId, "averagePrice", e.target.value)
                      }
                    />
                  ) : editMode && isDomestic ? (
                    formatMoney(
                      Number(drafts[h.holdingId]?.holdingQuantity) > 0
                        ? (Number(
                            drafts[h.holdingId]?.wholeSharePurchaseAmount,
                          ) +
                            Number(
                              drafts[h.holdingId]
                                ?.fractionalSharePurchaseAmount,
                            )) /
                            Number(drafts[h.holdingId]?.holdingQuantity)
                        : 0,
                    )
                  ) : (
                    formatMoney(Number(h.averagePrice))
                  )}
                </td>
                <td>{formatMoney(Number(h.currentPrice))}</td>
                <td>
                  <strong>{formatMoney(holdingEvaluation(h))}</strong>
                </td>
                <td className={Number(h.profitLossRate) >= 0 ? "pos" : "neg"}>
                  {Number(h.profitLossRate) >= 0 ? "+" : ""}
                  {Number(h.profitLossRate).toFixed(2)}%
                </td>
                <td>
                  {h.targetWeight == null
                    ? "-"
                    : `${Number(h.targetWeight).toFixed(2)}%`}
                </td>
                <td>
                  {h.currentWeight == null
                    ? "-"
                    : `${Number(h.currentWeight).toFixed(2)}%`}
                </td>
                <td title={h.weightStatus ?? undefined}>
                  {h.weightStatusName ?? "-"}
                </td>
              </tr>
            ))
          )}
        </Table>
      </section>
      {createOpen && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) =>
            e.target === e.currentTarget && setCreateOpen(false)
          }
        >
          <section className="edit-modal ref-modal">
            <header>
              <div>
                <h3>보유종목 등록</h3>
                <p>{accountLabel(selected)} 계좌에 새 종목을 등록합니다.</p>
              </div>
              <button
                className="modal-close"
                onClick={() => setCreateOpen(false)}
              >
                ×
              </button>
            </header>
            <div className="ref-form">
              <label>
                <span>계좌 *</span>
                <select
                  value={newHolding.accountId ?? ""}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      accountId: Number(e.target.value),
                    })
                  }
                >
                  {adminAccounts
                    .filter((a) => a.accountType === selected)
                    .map((a) => (
                      <option key={a.accountId} value={a.accountId}>
                        {accountLabel(a.accountType)}
                      </option>
                    ))}
                </select>
              </label>
              <label>
                <span>종목 *</span>
                <select
                  value={newHolding.stockId ?? ""}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      stockId: Number(e.target.value),
                    })
                  }
                >
                  <option value="">종목 선택</option>
                  {adminStocks
                    .filter((st) => st.useYn === "Y")
                    .map((st) => (
                      <option key={st.stockId} value={st.stockId}>
                        {st.stockName} ({st.stockCode})
                      </option>
                    ))}
                </select>
              </label>
              <label>
                <span>보유수량 *</span>
                <input
                  type="number"
                  min="0"
                  step="0.00000001"
                  value={newHolding.holdingQuantity}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      holdingQuantity: Number(e.target.value),
                    })
                  }
                />
              </label>
              <label>
                <span>평균 매입가 *</span>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={
                    isDomestic
                      ? Number(newHolding.holdingQuantity) > 0
                        ? (Number(newHolding.wholeSharePurchaseAmount) +
                            Number(newHolding.fractionalSharePurchaseAmount)) /
                          Number(newHolding.holdingQuantity)
                        : 0
                      : newHolding.averagePrice
                  }
                  disabled={isDomestic}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      averagePrice: Number(e.target.value),
                    })
                  }
                />
              </label>
              {isDomestic && (
                <label>
                  <span>정수주 매입금액 *</span>
                  <input
                    type="number"
                    min="0"
                    step="0.0001"
                    value={newHolding.wholeSharePurchaseAmount}
                    onChange={(e) =>
                      setNewHolding({
                        ...newHolding,
                        wholeSharePurchaseAmount: Number(e.target.value),
                      })
                    }
                  />
                </label>
              )}
              {isDomestic && (
                <label>
                  <span>소수점주 매입금액 *</span>
                  <input
                    type="number"
                    min="0"
                    step="0.0001"
                    value={newHolding.fractionalSharePurchaseAmount}
                    onChange={(e) =>
                      setNewHolding({
                        ...newHolding,
                        fractionalSharePurchaseAmount: Number(e.target.value),
                      })
                    }
                  />
                </label>
              )}
              <label>
                <span>적용 환율 *</span>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={newHolding.exchangeRate}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      exchangeRate: Number(e.target.value),
                    })
                  }
                />
              </label>
              <label>
                <span>목표 비중 (%)</span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  step="any"
                  value={newHolding.targetWeight ?? ""}
                  onChange={(e) =>
                    setNewHolding({
                      ...newHolding,
                      targetWeight:
                        e.target.value === "" ? null : Number(e.target.value),
                    })
                  }
                />
              </label>
              <label className="wide">
                <span>메모</span>
                <input
                  value={newHolding.memo}
                  onChange={(e) =>
                    setNewHolding({ ...newHolding, memo: e.target.value })
                  }
                />
              </label>
            </div>
            {createError && <p className="form-error">{createError}</p>}
            <footer>
              <button
                className="cancel-button"
                onClick={() => setCreateOpen(false)}
              >
                취소
              </button>
              <button
                className="primary"
                disabled={createSaving}
                onClick={saveCreate}
              >
                {createSaving ? "저장 중..." : "등록"}
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}
const Summary = ({
  evaluation,
  profit,
  format = won,
}: {
  evaluation: number;
  profit: number;
  format?: (n: number) => string;
}) => (
  <section className="summary" style={{ gridTemplateColumns: "repeat(2,1fr)" }}>
    <div>
      <small>평가금액</small>
      <strong>{format(evaluation)}</strong>
    </div>
    <div>
      <small>평가손익</small>
      <strong className={profit >= 0 ? "pos" : "neg"}>
        {profit >= 0 ? "+" : ""}
        {format(profit)}
      </strong>
    </div>
  </section>
);
const Badge = ({
  children,
  buy,
}: {
  children: React.ReactNode;
  buy?: boolean;
}) => <span className={`badge ${buy ? "buy" : ""}`}>{children}</span>;
const Table = ({
  heads,
  children,
}: {
  heads: string[];
  children: React.ReactNode;
}) => (
  <div className="tablewrap">
    <table>
      <thead>
        <tr>
          {heads.map((x) => (
            <th key={x}>{x}</th>
          ))}
        </tr>
      </thead>
      <tbody>{children}</tbody>
    </table>
  </div>
);
function Additional() {
  const { accountTypes, accountLabel } = useAccountTypes();
  type Candidate = {
    additionalBuyId: number;
    accountType: Account["accountType"];
    stockCode: string;
    stockName: string;
    eligibleYn: string;
    priority: number | null;
    score: number;
    recommendedAmount: number;
    reason: string;
    executedYn: string;
  };
  type Result = {
    baseDate: string | null;
    reserveAmount: number;
    recommendedTotal: number;
    usageRate: number;
    accounts: {
      accountId: number;
      accountType: Account["accountType"];
      reserveAmount: number;
      recommendedTotal: number;
      usageRate: number;
    }[];
    candidates: Candidate[];
  };
  const [data, setData] = useState<Result | null>(null),
    [error, setError] = useState(""),
    [accountType, setAccountType] =
      useState<Account["accountType"]>("DOMESTIC");
  useEffect(() => {
    fetch("/api/investment/buy-plans/additional/latest", { cache: "no-store" })
      .then(async (r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json() as Promise<Result>;
      })
      .then(setData)
      .catch((e) =>
        setError(
          e instanceof Error
            ? e.message
            : "추가매수 추천을 불러오지 못했습니다.",
        ),
      );
  }, []);
  if (error)
    return (
      <div className="page">
        <section className="card empty">
          <p>{error}</p>
        </section>
      </div>
    );
  if (!data)
    return (
      <div className="page">
        <section className="card empty">
          <p>추가매수 추천을 불러오는 중입니다.</p>
        </section>
      </div>
    );
  const rows = data.candidates.filter((r) => r.accountType === accountType),
    accountSummary = data.accounts.find(
      (account) => account.accountType === accountType,
    ) || {
      accountId: 0,
      accountType,
      reserveAmount: 0,
      recommendedTotal: 0,
      usageRate: 0,
    };
  return (
    <div className="page">
      <div className="tabs account-tabs">
        {orderedAccountTypes(accountTypes).map((type) => (
          <button
            key={type}
            className={accountType === type ? "active" : ""}
            onClick={() => setAccountType(type)}
          >
            {accountLabel(type)}
          </button>
        ))}
      </div>
      <section className="cash">
        <div>
          <small>{accountLabel(accountType)} 추가매수 확보현금</small>
          <strong>{won(accountSummary.reserveAmount)}</strong>
          <p>
            {data.baseDate
              ? `${data.baseDate} 계산 기준`
              : "계산된 추가매수 계획 없음"}{" "}
            · 계좌 최대 사용 권장액 {won(accountSummary.recommendedTotal)}
          </p>
        </div>
        <div>
          <i>
            <b
              style={{ width: `${Math.min(100, accountSummary.usageRate)}%` }}
            />
          </i>
          <span>
            계좌 확보현금 사용 권장률{" "}
            <strong>{accountSummary.usageRate.toFixed(1)}%</strong>
          </span>
        </div>
      </section>
      <section className="card recommends">
        {rows.length === 0 ? (
          <p className="data-state">
            선택한 계좌의 추가매수 평가 결과가 없습니다.
          </p>
        ) : (
          rows.map((r) => (
            <article
              key={r.additionalBuyId}
              className={r.eligibleYn === "Y" ? "" : "muted-row"}
            >
              <span>
                <strong>{r.stockCode}</strong>
                <small>{r.stockName || r.stockCode}</small>
              </span>
              <em>
                {r.eligibleYn === "Y"
                  ? "추천점수 " + Number(r.score).toFixed(0)
                  : "제외"}
              </em>
              <strong>
                {r.accountType === "OVERSEAS"
                  ? usd(Number(r.recommendedAmount))
                  : won(Number(r.recommendedAmount))}
              </strong>
            </article>
          ))
        )}
      </section>
    </div>
  );
}
function History({
  period,
  set,
}: {
  period: string;
  set: (s: string) => void;
}) {
  type Row = {
    briefingId: number;
    baseDate: string;
    briefingType: string;
    title: string;
    summary: string | null;
    status: string;
    publishedYn: string;
    confidenceRate: number;
    marketScore: number | null;
    marketRegime: string | null;
  };
  type Detail = {
    briefingId: number;
    baseDate: string;
    briefingType: string;
    title: string;
    summary: string | null;
    body: string | null;
    status: string;
    publishedYn: string;
    confidenceRate: number;
    items: {
      itemCode: string;
      summary: string;
      content: string;
      signalCode: string | null;
    }[];
  };
  const [rows, setRows] = useState<Row[]>([]),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [detail, setDetail] = useState<Detail | null>(null),
    [detailLoading, setDetailLoading] = useState(false);
  const type =
    period === "일일" ? "DAILY" : period === "주간" ? "WEEKLY" : "MONTHLY";
  useEffect(() => {
    let alive = true;
    setDetail(null);
    setLoading(true);
    setError("");
    api<Row[]>(`/api/v1/briefings/history?type=${type}`)
      .then((data) => {
        if (alive) setRows(data);
      })
      .catch((e) => {
        if (alive)
          setError(
            e instanceof Error
              ? e.message
              : "브리핑 이력을 불러오지 못했습니다.",
          );
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [type]);
  const openDetail = (id: number) => {
    setDetailLoading(true);
    setError("");
    api<Detail>(`/api/v1/briefings/${id}`)
      .then(setDetail)
      .catch((e) =>
        setError(
          e instanceof Error ? e.message : "브리핑 상세를 불러오지 못했습니다.",
        ),
      )
      .finally(() => setDetailLoading(false));
  };
  const statusLabel: Record<string, string> = {
    PUBLISHED: "발행",
    REVIEWED: "검토",
    GENERATED: "생성",
    GENERATING: "생성 중",
    READY: "준비",
    FAILED: "실패",
    CANCELLED: "취소",
  };
  if (detail)
    return (
      <div className="page">
        <button className="history-back" onClick={() => setDetail(null)}>
          ← 브리핑 이력
        </button>
        <section className="briefHero">
          <h2>{detail.baseDate}</h2>
          <p>
            {detail.summary || detail.body || "저장된 브리핑 요약이 없습니다."}
          </p>
        </section>
        <section className="card briefing-sections">
          {detail.items.length ? (
            detail.items.map((item, i) => (
              <article key={item.itemCode}>
                <b>{String(i + 1).padStart(2, "0")}</b>
                <div>
                  <h3>{item.summary}</h3>
                  <p>{item.content}</p>
                </div>
              </article>
            ))
          ) : (
            <p className="data-state">등록된 브리핑 세부 항목이 없습니다.</p>
          )}
        </section>
      </div>
    );
  return (
    <div className="page">
      <Tabs vals={["일일", "주간", "월간"]} active={period} set={set} />
      <section className="card history">
        {error ? (
          <p className="data-state error">{error}</p>
        ) : loading ? (
          <p className="data-state">브리핑 이력을 불러오는 중입니다.</p>
        ) : rows.length === 0 ? (
          <p className="data-state">등록된 {period} 브리핑 이력이 없습니다.</p>
        ) : (
          rows.map((r) => (
            <article key={r.briefingId}>
              <span>
                <button
                  className="history-title"
                  onClick={() => openDetail(r.briefingId)}
                  disabled={detailLoading}
                >
                  {r.baseDate}
                </button>
              </span>
              <em>
                {r.marketScore == null ? (
                  "시장점수 -"
                ) : (
                  <>
                    시장점수 <b>{Number(r.marketScore).toFixed(0)}</b>
                  </>
                )}
              </em>
              <Badge buy={r.publishedYn === "Y"}>
                {statusLabel[r.status] || r.status}
              </Badge>
              <button
                className="history-arrow"
                onClick={() => openDetail(r.briefingId)}
                disabled={detailLoading}
              >
                →
              </button>
            </article>
          ))
        )}
      </section>
    </div>
  );
}
createRoot(document.getElementById("root")!).render(
  <AccountTypeProvider>
    <App />
  </AccountTypeProvider>,
);
