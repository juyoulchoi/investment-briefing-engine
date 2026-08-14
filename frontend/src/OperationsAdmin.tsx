import React, { useEffect, useState } from "react";
type Kind = "holdings" | "regular-buys" | "cash-reserves";
type Row = Record<string, any>;
type ApiResult<T> = { success: boolean; data: T; error?: { message?: string } };
type Field = {
  key: string;
  label: string;
  help?: string;
  type?: string;
  required?: boolean;
  options?: [string, string][];
  wide?: boolean;
};
const labels: Record<string, string> = {
  DOMESTIC: "국내",
  OVERSEAS: "해외",
  ISA: "ISA",
  PENSION: "연금",
  DAILY: "매일",
  WEEKLY: "매주",
  MONTHLY: "매월",
  CUSTOM: "사용자 지정",
  AMOUNT: "금액",
  QUANTITY: "수량",
  ACTIVE: "활성",
  PAUSED: "일시정지",
  STOPPED: "중지",
  TODAY: "매수",
  WAIT: "대기",
  CORE: "핵심",
  SATELLITE: "위성",
  THEME: "테마",
  CASH_LIKE: "현금성",
  CLOSED: "종료",
  TRANSFER_PENDING: "이관 대기",
  Y: "예",
  N: "아니오",
};
const config: Record<
  Kind,
  {
    title: string;
    table: string;
    id: string;
    columns: [string, string][];
    fields: Field[];
    defaults: Row;
  }
> = {
  holdings: {
    title: "보유종목",
    table: "TB_HOLD",
    id: "holdingId",
    columns: [
      ["accountType", "계좌"],
      ["stockName", "종목명"],
      ["stockCode", "종목 코드"],
      ["holdingQuantity", "보유수량"],
      ["averagePrice", "평단가"],
      ["currentPrice", "현재가"],
      ["targetWeight", "목표비중"],
      ["holdingStatus", "상태"],
    ],
    defaults: {
      holdingQuantity: 0,
      averagePrice: 0,
      exchangeRate: 1,
      holdingStatus: "ACTIVE",
      useYn: "Y",
    },
    fields: [
      { key: "accountId", label: "계좌", type: "account", required: true },
      { key: "stockId", label: "종목", type: "stock", required: true },
      {
        key: "holdingQuantity",
        label: "보유수량",
        type: "number",
        required: true,
      },
      {
        key: "averagePrice",
        label: "평균 매입가",
        type: "number",
        required: true,
      },
      {
        key: "exchangeRate",
        label: "적용 환율",
        type: "number",
        required: true,
      },
      { key: "targetWeight", label: "목표 비중 (%)", type: "number" },
      {
        key: "holdingStatus",
        label: "보유 상태",
        type: "select",
        required: true,
        options: [
          ["ACTIVE", "활성"],
          ["CLOSED", "종료"],
          ["TRANSFER_PENDING", "이관 대기"],
        ],
      },
      {
        key: "useYn",
        label: "사용 여부",
        type: "select",
        options: [
          ["Y", "사용"],
          ["N", "미사용"],
        ],
      },
      { key: "memo", label: "메모", wide: true },
    ],
  },
  "regular-buys": {
    title: "정기매수 설정",
    table: "TB_REG_BUY",
    id: "regularBuyKey",
    columns: [
      ["accountType", "계좌"],
      ["priority", "우선순위"],
      ["stockCode", "종목코드"],
      ["stockName", "종목명"],
      ["stockGrade", "종목등급"],
      ["targetWeight", "목표비중"],
      ["buyBasis", "매수단위"],
      ["appliedSchedule", "현재 적용주기"],
      ["appliedValue", "현재 적용금액"],
      ["pauseReason", "중지사유"],
    ],
    defaults: {
      buyCycle: "MONTHLY",
      appliedCycle: "MONTHLY",
      buyBasis: "AMOUNT",
      minimumBuyAmount: 0,
      maximumMultiplier: 3,
      buyStatus: "ACTIVE",
      userPauseYn: "N",
      autoCalculateYn: "Y",
    },
    fields: [
      { key: "accountId", label: "계좌", type: "account", required: true },
      { key: "stockId", label: "종목", type: "stock", required: true },
      {
        key: "priority",
        label: "우선순위",
        help: "큰 숫자가 높은 우선순위",
        type: "number",
        required: true,
      },
      {
        key: "investmentGrade",
        label: "투자등급",
        help: "공통코드 INVESTMENT_GRADE",
        type: "investmentGrade",
        required: true,
      },
      {
        key: "buyBasis",
        label: "매수 기준",
        type: "select",
        required: true,
        options: [
          ["AMOUNT", "금액 기준"],
          ["QUANTITY", "수량 기준"],
        ],
      },
      {
        key: "minimumBuyAmount",
        label: "기준금액",
        type: "number",
        required: true,
      },
      {
        key: "buyCycle",
        label: "기준 매수주기",
        type: "select",
        required: true,
        options: [
          ["DAILY", "매일"],
          ["WEEKLY", "매주"],
          ["MONTHLY", "매월"],
        ],
      },
      {
        key: "buyDayCode",
        label: "기준 매수요일",
        type: "weekdays",
        wide: true,
      },
      {
        key: "buyDayNumbers",
        label: "기준 매수일",
        help: "복수 선택 가능",
        type: "monthdays",
        wide: true,
      },
      { key: "appliedAmount", label: "현재 적용금액", type: "number" },
      {
        key: "appliedCycle",
        label: "현재 적용주기",
        type: "select",
        required: true,
        options: [
          ["DAILY", "매일"],
          ["WEEKLY", "매주"],
          ["MONTHLY", "매월"],
          ["PAUSED", "적용 중지"],
          ["MANUAL", "사용자 지정"],
        ],
      },
      {
        key: "appliedWeekDays",
        label: "적용 매수요일",
        type: "weekdays",
        wide: true,
      },
      {
        key: "appliedMonthDay",
        label: "적용 매수일",
        type: "number",
        wide: true,
      },
      {
        key: "baseBuyQuantity",
        label: "기본 매수 수량",
        help: "정기매수 계산의 기준 수량",
        type: "number",
      },
      {
        key: "buyQuantity",
        label: "매수 수량",
        help: "주기마다 실제로 매수할 수량",
        type: "number",
      },
      {
        key: "buyStatus",
        label: "매수 상태",
        help: "정기매수의 현재 실행 상태",
        type: "select",
        required: true,
        options: [
          ["ACTIVE", "활성"],
          ["PAUSED", "일시정지"],
          ["STOPPED", "중지"],
        ],
      },
      {
        key: "userPauseYn",
        label: "사용자 일시정지",
        help: "직접 잠시 멈출 때 예",
        type: "select",
        options: [
          ["N", "아니오"],
          ["Y", "예"],
        ],
      },
      {
        key: "pauseReason",
        label: "일시정지 사유",
        type: "select",
        options: [
          ["", "선택 안 함"],
          ["기본 설정", "기본 설정"],
          ["비중 초과", "비중 초과"],
          ["사용자 일시정지", "사용자 일시정지"],
          ["매수 조건 미충족", "매수 조건 미충족"],
          ["투자 전략 변경", "투자 전략 변경"],
          ["기타", "기타"],
        ],
      },
      { key: "memo", label: "비고", wide: true },
    ],
  },
  "cash-reserves": {
    title: "추가매수 대기현금",
    table: "TB_CASH_RSV",
    id: "cashReserveId",
    columns: [
      ["accountType", "계좌"],
      ["reserveAmount", "현재 대기금액"],
      ["accumulatedAmount", "누적 적립액"],
      ["usedAmount", "누적 사용액"],
      ["availableAmount", "가용 금액"],
    ],
    defaults: { reserveAmount: 0, accumulatedAmount: 0, usedAmount: 0 },
    fields: [
      { key: "accountId", label: "계좌", type: "account", required: true },
      {
        key: "reserveAmount",
        label: "현재 대기금액",
        type: "number",
        required: true,
      },
      {
        key: "accumulatedAmount",
        label: "누적 적립액",
        type: "number",
        required: true,
      },
      {
        key: "usedAmount",
        label: "누적 사용액",
        type: "number",
        required: true,
      },
      { key: "lastTransactionDate", label: "최종 거래일", type: "date" },
    ],
  },
};
async function call<T>(url: string, init?: RequestInit) {
  const r = await fetch(url, init),
    b = (await r.json()) as ApiResult<T>;
  if (!r.ok || !b.success)
    throw new Error(b.error?.message || `HTTP ${r.status}`);
  return b.data;
}
export default function OperationsAdmin({
  notify,
  initialKind,
}: {
  notify: (s: string) => void;
  initialKind?: Kind;
}) {
  const [kind, setKind] = useState<Kind>(initialKind || "regular-buys"),
    [rows, setRows] = useState<Row[]>([]),
    [accounts, setAccounts] = useState<Row[]>([]),
    [stocks, setStocks] = useState<Row[]>([]),
    [accountHoldings, setAccountHoldings] = useState<Row[]>([]),
    [investmentGrades, setInvestmentGrades] = useState<Row[]>([]),
    [loading, setLoading] = useState(true),
    [editing, setEditing] = useState<Row | null>(null),
    [form, setForm] = useState<Row>({}),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false),
    [query, setQuery] = useState(""),
    [buyStatusFilter, setBuyStatusFilter] = useState(""),
    [selectedAccount, setSelectedAccount] = useState<string>("DOMESTIC");
  const load = async (k = kind) => {
    setLoading(true);
    setError("");
    try {
      const [data, a, s, h, grades] = await Promise.all([
        call<Row[]>(`/api/v1/admin/operations/${k}`),
        accounts.length
          ? Promise.resolve(accounts)
          : call<Row[]>("/api/v1/admin/reference/accounts"),
        stocks.length
          ? Promise.resolve(stocks)
          : call<Row[]>("/api/v1/admin/reference/stocks"),
        accountHoldings.length
          ? Promise.resolve(accountHoldings)
          : call<Row[]>("/api/v1/admin/operations/holdings"),
        investmentGrades.length
          ? Promise.resolve(investmentGrades)
          : call<Row[]>("/api/v1/admin/operations/investment-grades"),
      ]);
      setRows(data);
      setAccounts(a);
      setStocks(s);
      setAccountHoldings(h);
      setInvestmentGrades(grades);
    } catch (e) {
      setError(e instanceof Error ? e.message : "데이터 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load(kind);
  }, [kind]);
  const open = (r?: Row) => {
    setEditing(r || {});
    setForm(
      r
        ? { ...r }
        : {
            ...config[kind].defaults,
            ...(kind === "regular-buys"
              ? {
                  accountId: accounts.find(
                    (a) => a.accountType === selectedAccount,
                  )?.accountId,
                }
              : {}),
          },
    );
    setError("");
  };
  const save = async () => {
    for (const f of config[kind].fields)
      if (
        f.required &&
        (form[f.key] === undefined ||
          form[f.key] === null ||
          String(form[f.key]).trim() === "")
      ) {
        setError(`${f.label}을(를) 입력하세요.`);
        return;
      }
    setSaving(true);
    setError("");
    try {
      const id = form[config[kind].id];
      await call(`/api/v1/admin/operations/${kind}${id ? `/${id}` : ""}`, {
        method: id ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      setEditing(null);
      await load();
      notify(`${config[kind].title} 정보가 저장되었습니다.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };
  const schedule = (cycle: any, weekDays: any, monthDays: any) => {
    if (cycle === "PAUSED" || cycle === "STOPPED") return "-";
    const name = labels[String(cycle)] || String(cycle || "-");
    if (cycle === "WEEKLY" && weekDays)
      return `${name} ${String(weekDays)
        .split(",")
        .map(
          (v) =>
            (
              ({ MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금" }) as Row
            )[v] || v,
        )
        .join("·")}`;
    if (cycle === "MONTHLY" && monthDays)
      return `${name} ${String(monthDays)
        .split(",")
        .map((v) => `${v}일`)
        .join("·")}`;
    return name;
  };
  const viewRows: Row[] = rows.map(
      (r) =>
        ({
          ...r,
          baseSchedule: schedule(r.baseCycle, r.baseWeekDays, r.baseMonthDays),
          appliedSchedule: schedule(
            r.appliedCycle,
            r.appliedWeekDays,
            r.appliedMonthDay,
          ),
          baseValue: r.baseAmount,
          recommendedValue: r.recommendedAmount,
          appliedValue: r.appliedAmount,
        }) as Row,
    ),
    normalizedQuery = query.trim().toLowerCase(),
    shown = viewRows
      .filter(
        (r) =>
          (kind !== "regular-buys" || r.accountType === selectedAccount) &&
          (kind !== "regular-buys" ||
            !buyStatusFilter ||
            r.buyStatus === buyStatusFilter) &&
          (kind !== "regular-buys" ||
            !normalizedQuery ||
            String(r.stockName ?? "")
              .toLowerCase()
              .includes(normalizedQuery) ||
            String(r.stockCode ?? "")
              .toLowerCase()
              .includes(normalizedQuery)),
      )
      .sort((a, b) => {
        const accountOrder = ["DOMESTIC", "OVERSEAS", "ISA", "PENSION"],
          accountDiff =
            accountOrder.indexOf(String(a.accountType)) -
            accountOrder.indexOf(String(b.accountType)),
          aPriority =
            a.priority == null ? Number.NEGATIVE_INFINITY : Number(a.priority),
          bPriority =
            b.priority == null ? Number.NEGATIVE_INFINITY : Number(b.priority);
        return (
          accountDiff ||
          bPriority - aPriority ||
          String(a.stockName ?? "").localeCompare(
            String(b.stockName ?? ""),
            "ko",
          )
        );
      }),
    moneyKeys = new Set([
      "averagePrice",
      "currentPrice",
      "minimumBuyAmount",
      "maximumBuyAmount",
      "appliedAmount",
      "reserveAmount",
      "accumulatedAmount",
      "usedAmount",
      "availableAmount",
      "baseValue",
      "recommendedValue",
      "appliedValue",
    ]),
    show = (k: string, v: any, r: Row) =>
      k === "investmentGrade" && !v
        ? "미설정"
        : k === "targetWeight"
          ? v == null
            ? "-"
            : `${Number(v).toFixed(2)}%`
          : k === "buyBasis"
            ? v === "QUANTITY"
              ? "수량"
              : "금액"
            : k === "activeYn"
              ? v === "Y"
                ? "적용"
                : "미적용"
              : k === "marketCode"
                ? v === "KO"
                  ? "국내"
                  : v === "US"
                    ? "미국"
                    : String(v ?? "-")
                : moneyKeys.has(k)
                  ? v == null
                    ? "-"
                    : r.accountType === "OVERSEAS"
                      ? `USD ${Number(v).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                      : Number(v).toLocaleString("ko-KR")
                  : labels[String(v)] || String(v ?? "-");
  const regularBuyDetails: [string, string][] = [
    ["stockGrade", "종목등급"],
    ["benchmarkName", "기준지수"],
    ["targetWeight", "목표비중"],
    ["recommendedValue", "추천금액"],
  ];
  const regularBuyDetailRow: Row = {
    ...form,
    baseSchedule: schedule(
      form.buyCycle,
      form.buyDayCode,
      form.buyDayNumbers || form.buyDayNumber,
    ),
    baseValue: form.minimumBuyAmount,
    recommendedValue: form.recommendedAmount ?? form.recommendedBuyAmount,
    activeYn: form.activeYn ?? (form.buyStatus === "ACTIVE" ? "Y" : "N"),
    appliedSchedule: schedule(
      form.appliedCycle,
      form.appliedWeekDays,
      form.appliedMonthDay,
    ),
    appliedValue: form.appliedAmount,
  };
  const selectedAccountType = accounts.find(
      (a) => a.accountId === Number(form.accountId),
    )?.accountType,
    stockMarket =
      selectedAccountType === "OVERSEAS"
        ? "US"
        : selectedAccountType
          ? "KO"
          : null,
    stockChoices = stocks
      .filter(
        (s) =>
          s.useYn === "Y" &&
          Boolean(selectedAccountType) &&
          s.marketCode === stockMarket &&
          (Boolean(form.regularBuyKey) ||
            accountHoldings.some(
              (h) =>
                Number(h.accountId) === Number(form.accountId) &&
                Number(h.stockId) === Number(s.stockId),
            )) &&
          (Boolean(form.regularBuyKey) ||
            !rows.some(
              (r) =>
                Number(r.accountId) === Number(form.accountId) &&
                Number(r.stockId) === Number(s.stockId),
            )),
      )
      .sort((a, b) =>
        String(a.stockName).localeCompare(String(b.stockName), "ko"),
      );
  const csvValues = (key: string) =>
      String(form[key] || "")
        .split(",")
        .filter(Boolean),
    toggleCsv = (key: string, value: string, order: string[]) => {
      const values = csvValues(key),
        next = values.includes(value)
          ? values.filter((v) => v !== value)
          : [...values, value];
      setForm({
        ...form,
        [key]: order.filter((v) => next.includes(v)).join(",") || null,
      });
    };
  const input = (f: Field) =>
    f.type === "account" ? (
      <select
        disabled={kind === "regular-buys" && Boolean(form.regularBuyKey)}
        value={form[f.key] ?? ""}
        onChange={(e) =>
          setForm({ ...form, [f.key]: Number(e.target.value), stockId: null })
        }
      >
        <option value="">계좌 선택</option>
        {accounts
          .filter((a) => a.useYn === "Y")
          .map((a) => (
            <option key={a.accountId} value={a.accountId}>
              {labels[a.accountType] || a.accountType}
              {a.brokerName ? ` · ${a.brokerName}` : ""}
            </option>
          ))}
      </select>
    ) : f.type === "stock" ? (
      <select
        disabled={kind === "regular-buys" && Boolean(form.regularBuyKey)}
        value={form[f.key] ?? ""}
        onChange={(e) => setForm({ ...form, [f.key]: Number(e.target.value) })}
      >
        <option value="">종목 선택 ({stockChoices.length}개)</option>
        {stockChoices.map((s) => (
          <option key={s.stockId} value={s.stockId}>
            {s.stockName} ({s.stockCode})
          </option>
        ))}
      </select>
    ) : f.type === "investmentGrade" ? (
      <select
        value={form[f.key] ?? ""}
        onChange={(e) => {
          const grade = investmentGrades.find(
            (item) => item.investmentGrade === e.target.value,
          );
          setForm({
            ...form,
            [f.key]: grade?.investmentGrade ?? null,
            weightScore: grade?.weightScore ?? null,
          });
        }}
      >
        <option value="">선택 안 함</option>
        {investmentGrades.map((grade) => (
          <option key={grade.investmentGrade} value={grade.investmentGrade}>
            {grade.investmentGrade} · {grade.weightScore}점 ·{" "}
            {grade.description}
          </option>
        ))}
      </select>
    ) : f.type === "weekdays" ? (
      <div className="multi-options">
        {[
          ["MON", "월"],
          ["TUE", "화"],
          ["WED", "수"],
          ["THU", "목"],
          ["FRI", "금"],
        ].map(([v, n]) => (
          <button
            type="button"
            key={v}
            className={csvValues(f.key).includes(v) ? "selected" : ""}
            onClick={() =>
              toggleCsv(f.key, v, ["MON", "TUE", "WED", "THU", "FRI"])
            }
          >
            {n}
          </button>
        ))}
      </div>
    ) : f.type === "monthdays" ? (
      <div className="multi-options month-days">
        {Array.from({ length: 31 }, (_, i) => String(i + 1)).map((v) => (
          <button
            type="button"
            key={v}
            className={csvValues(f.key).includes(v) ? "selected" : ""}
            onClick={() =>
              toggleCsv(
                f.key,
                v,
                Array.from({ length: 31 }, (_, i) => String(i + 1)),
              )
            }
          >
            {v}
          </button>
        ))}
      </div>
    ) : f.type === "select" ? (
      <select
        disabled={
          kind === "regular-buys" &&
          f.key === "userPauseYn" &&
          form.buyStatus !== "ACTIVE"
        }
        value={form[f.key] ?? ""}
        onChange={(e) => {
          const value = e.target.value || null;
          if (kind === "regular-buys" && f.key === "buyStatus")
            setForm({
              ...form,
              buyStatus: value,
              userPauseYn: value === "ACTIVE" ? form.userPauseYn : "N",
            });
          else if (kind === "regular-buys" && f.key === "userPauseYn")
            setForm({ ...form, userPauseYn: value });
          else setForm({ ...form, [f.key]: value });
        }}
      >
        {f.options?.map((o) => (
          <option key={o[0]} value={o[0]}>
            {o[1]}
          </option>
        ))}
      </select>
    ) : (
      <input
        type={f.type || "text"}
        value={form[f.key] ?? ""}
        onChange={(e) =>
          setForm({
            ...form,
            [f.key]:
              f.type === "number" && e.target.value !== ""
                ? Number(e.target.value)
                : e.target.value || null,
          })
        }
      />
    );
  return (
    <div className="page ref-page">
      <section className="ref-intro operations-intro">
        <div>
          <h2>투자 설정 관리</h2>
          <p>
            계좌별 보유종목, 정기매수 계획과 추가매수 대기현금을 관리합니다.
          </p>
        </div>
        <button className="primary" onClick={() => open()}>
          + {config[kind].title} 등록
        </button>
      </section>
      <div className="tabs ref-tabs">
        {(["regular-buys", "cash-reserves"] as Kind[]).map((k) => (
          <button
            key={k}
            className={kind === k ? "active" : ""}
            onClick={() => {
              setKind(k);
              setQuery("");
              setBuyStatusFilter("");
            }}
          >
            {config[k].title}
          </button>
        ))}
      </div>
      <section className="card table ref-table">
        {kind === "regular-buys" && (
          <header className="head list-tools">
            <div className="list-filters">
              <select
                className="status-filter"
                value={selectedAccount}
                onChange={(e) => {
                  setSelectedAccount(e.target.value);
                  setQuery("");
                }}
                aria-label="계좌 검색"
              >
                <option value="DOMESTIC">국내주식</option>
                <option value="OVERSEAS">해외주식</option>
                <option value="ISA">ISA</option>
                <option value="PENSION">연금</option>
              </select>
              <input
                className="ref-search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="종목명 또는 종목 코드 검색"
                aria-label="종목 검색"
              />
              <select
                className="status-filter"
                value={buyStatusFilter}
                onChange={(e) => setBuyStatusFilter(e.target.value)}
                aria-label="매수 상태 검색"
              >
                <option value="">전체 매수 상태</option>
                <option value="ACTIVE">활성</option>
                <option value="PAUSED">일시정지</option>
                <option value="STOPPED">중지</option>
              </select>
            </div>
          </header>
        )}
        {error && !editing && <p className="form-error ref-error">{error}</p>}
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                {config[kind].columns.map((c) => (
                  <th key={c[0]}>{c[1]}</th>
                ))}
                {kind !== "regular-buys" && <th>관리</th>}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td
                    colSpan={
                      config[kind].columns.length +
                      (kind === "regular-buys" ? 0 : 1)
                    }
                    className="data-state"
                  >
                    불러오는 중입니다.
                  </td>
                </tr>
              ) : shown.length === 0 ? (
                <tr>
                  <td
                    colSpan={
                      config[kind].columns.length +
                      (kind === "regular-buys" ? 0 : 1)
                    }
                    className="data-state empty-state"
                  >
                    등록된 정보가 없습니다.
                    <button className="edit-button" onClick={() => open()}>
                      첫 설정 등록하기
                    </button>
                  </td>
                </tr>
              ) : (
                shown.map((r) => (
                  <tr key={r[config[kind].id]}>
                    {config[kind].columns.map((c) => (
                      <td key={c[0]}>
                        {kind === "regular-buys" && c[0] === "stockName" ? (
                          <button
                            className="row-edit-link"
                            onClick={() => open(r)}
                            title="정기매수 설정 수정"
                          >
                            {show(c[0], r[c[0]], r)}
                          </button>
                        ) : (
                          show(c[0], r[c[0]], r)
                        )}
                      </td>
                    ))}
                    {kind !== "regular-buys" && (
                      <td>
                        <button className="edit-button" onClick={() => open(r)}>
                          수정
                        </button>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
      {editing && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) => e.target === e.currentTarget && setEditing(null)}
        >
          <section className="edit-modal ref-modal">
            <header>
              <div>
                <h3>
                  {config[kind].title} {form[config[kind].id] ? "수정" : "등록"}
                </h3>
                <p>* 표시는 필수 입력 항목입니다.</p>
              </div>
              <button className="modal-close" onClick={() => setEditing(null)}>
                ×
              </button>
            </header>
            <div
              className={`ref-form ${kind === "regular-buys" ? "regular-buy-form" : ""}`}
            >
              {config[kind].fields
                .filter(
                  (f) =>
                    kind !== "regular-buys" ||
                    ((f.key !== "buyDayCode" || form.buyCycle === "WEEKLY") &&
                      (f.key !== "buyDayNumbers" ||
                        form.buyCycle === "MONTHLY") &&
                      (f.key !== "appliedWeekDays" ||
                        form.appliedCycle === "WEEKLY") &&
                      (f.key !== "appliedMonthDay" ||
                        form.appliedCycle === "MONTHLY") &&
                      (![
                        "minimumBuyAmount",
                        "maximumBuyAmount",
                        "appliedAmount",
                      ].includes(f.key) ||
                        form.buyBasis === "AMOUNT") &&
                      (!["baseBuyQuantity", "buyQuantity"].includes(f.key) ||
                        form.buyBasis === "QUANTITY")),
                )
                .map((f) => (
                  <label
                    key={f.key}
                    className={[
                      f.wide ? "wide" : "",
                      kind === "regular-buys"
                        ? `regular-buy-field regular-buy-field-${f.key}`
                        : "",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    <span>
                      {f.label}
                      {f.required && <b> *</b>}
                      {f.help && <small>{f.help}</small>}
                    </span>
                    {input(f)}
                  </label>
                ))}
            </div>
            {kind === "regular-buys" && (
              <section className="regular-buy-detail">
                <h4>
                  운영·전략 상세{" "}
                  <small>
                    {form.regularBuyKey
                      ? "저장 데이터"
                      : "등록 후 자동 연결되는 값은 미설정으로 표시됩니다."}
                  </small>
                </h4>
                <div>
                  {regularBuyDetails.map(([key, label]) => (
                    <dl key={key}>
                      <dt>{label}</dt>
                      <dd>
                        {show(
                          key,
                          regularBuyDetailRow[key],
                          regularBuyDetailRow,
                        )}
                      </dd>
                    </dl>
                  ))}
                </div>
              </section>
            )}
            {error && <p className="form-error">{error}</p>}
            <footer>
              <button
                className="cancel-button"
                onClick={() => setEditing(null)}
              >
                취소
              </button>
              <button className="primary" disabled={saving} onClick={save}>
                {saving ? "저장 중..." : "저장"}
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}
