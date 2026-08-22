import React, { useEffect, useState } from "react";

type Kind = "indices" | "accounts" | "stocks";
type Row = Record<string, any>;
type ApiResult<T> = { success: boolean; data: T; error?: { message?: string } };
const meta: Record<
  Kind,
  {
    title: string;
    id: string;
    columns: [string, string][];
    fields: {
      key: string;
      label: string;
      help?: string;
      required?: boolean;
      type?: string;
      options?: [string, string][];
      wide?: boolean;
    }[];
  }
> = {
  indices: {
    title: "기준지수",
    id: "indexId",
    columns: [
      ["indexCode", "지수 코드"],
      ["indexName", "지수명"],
      ["indexType", "유형"],
      ["marketCode", "시장"],
      ["dataSourceCode", "데이터 출처"],
      ["defaultYn", "기본"],
      ["useYn", "사용"],
    ],
    fields: [
      { key: "indexCode", label: "지수 코드", required: true },
      { key: "indexName", label: "지수명", required: true },
      { key: "indexEnglishName", label: "영문 지수명", wide: true },
      {
        key: "indexType",
        label: "지수 유형",
        required: true,
        type: "select",
        options: [
          ["MARKET", "시장"],
          ["SECTOR", "섹터"],
          ["ETF_PROXY", "ETF 대용"],
          ["BOND", "채권"],
          ["COMMODITY", "원자재"],
        ],
      },
      { key: "marketCode", label: "시장 코드" },
      { key: "countryCode", label: "국가 코드", required: true },
      { key: "currencyCode", label: "통화 코드", required: true },
      {
        key: "dataSourceCode",
        label: "데이터 출처",
        required: true,
        type: "select",
        options: [
          ["KRX", "KRX"],
          ["YAHOO", "Yahoo"],
          ["FRED", "FRED"],
          ["MANUAL", "수동"],
        ],
      },
      { key: "sourceSymbol", label: "수집 심볼" },
      {
        key: "defaultYn",
        label: "기본 지수",
        type: "select",
        options: [
          ["N", "아니오"],
          ["Y", "예"],
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
    ],
  },
  accounts: {
    title: "계좌",
    id: "accountId",
    columns: [
      ["accountType", "계좌 유형"],
      ["baseCurrencyCode", "통화"],
      ["cashAmount", "예수금"],
      ["reservedCashAmount", "대기 현금"],
      ["targetCashWeight", "목표 현금 비중"],
    ],
    fields: [
      {
        key: "accountType",
        label: "계좌 유형",
        required: true,
        type: "select",
        options: [
          ["DOMESTIC", "국내"],
          ["OVERSEAS", "해외"],
          ["ISA", "ISA"],
          ["PENSION", "연금"],
        ],
      },
      { key: "baseCurrencyCode", label: "기준 통화", required: true },
      {
        key: "cashAmount",
        label: "예수금",
        help: "현재 계좌에서 사용할 수 있는 증권 계좌의 예수금입니다.",
        required: true,
        type: "number",
      },
      {
        key: "reservedCashAmount",
        label: "대기 현금",
        help: "예수금에서 추가 매수에 사용할 현금이며, 적립과 사용 내역에 따라 자동 관리됩니다.",
        type: "readonly",
      },
      {
        key: "targetCashWeight",
        label: "목표 현금 비중 (%)",
        help: "전체 평가 금액에서 현금으로 유지할 목표 비중입니다.",
        type: "number",
      },
      {
        key: "displaySequence",
        label: "표시 순번",
        required: true,
        type: "number",
      },
    ],
  },
  stocks: {
    title: "종목",
    id: "stockId",
    columns: [
      ["stockCode", "종목 코드"],
      ["stockName", "종목명"],
      ["marketCode", "시장"],
      ["assetType", "자산 유형"],
      ["stockGrade", "등급"],
      ["baseIndexId", "기준지수"],
      ["useYn", "사용"],
    ],
    fields: [
      { key: "stockCode", label: "종목 코드", required: true },
      { key: "stockName", label: "종목명", required: true },
      { key: "stockEnglishName", label: "영문 종목명", wide: true },
      { key: "marketCode", label: "시장 코드", required: true },
      { key: "countryCode", label: "국가 코드", required: true },
      { key: "currencyCode", label: "통화 코드", required: true },
      {
        key: "assetType",
        label: "자산 유형",
        required: true,
        type: "select",
        options: [
          ["STOCK", "주식"],
          ["ETF", "ETF"],
          ["BOND_ETF", "채권 ETF"],
          ["COMMODITY_ETF", "원자재 ETF"],
          ["CASH_EQUIVALENT", "현금성"],
        ],
      },
      {
        key: "stockGrade",
        label: "종목 등급",
        required: true,
        type: "select",
        options: [
          ["CORE", "핵심"],
          ["SATELLITE", "위성"],
          ["THEME", "테마"],
        ],
      },
      { key: "baseIndexId", label: "기준지수", type: "index" },
      { key: "sectorCode", label: "섹터 코드" },
      { key: "sectorName", label: "섹터명" },
      { key: "industryCode", label: "산업 코드" },
      { key: "industryName", label: "산업명" },
      {
        key: "regularBuyYn",
        label: "정기매수",
        type: "select",
        options: [
          ["Y", "허용"],
          ["N", "미허용"],
        ],
      },
      {
        key: "additionalBuyYn",
        label: "추가매수",
        type: "select",
        options: [
          ["Y", "허용"],
          ["N", "미허용"],
        ],
      },
      {
        key: "rebuyYn",
        label: "재매수",
        type: "select",
        options: [
          ["Y", "허용"],
          ["N", "미허용"],
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
    ],
  },
};
const defaults: Record<Kind, Row> = {
  indices: {
    indexType: "MARKET",
    countryCode: "KR",
    currencyCode: "KRW",
    dataSourceCode: "KRX",
    defaultYn: "N",
  },
  accounts: {
    accountType: "DOMESTIC",
    baseCurrencyCode: "KRW",
    cashAmount: 0,
    reservedCashAmount: 0,
    targetCashWeight: 20,
    displaySequence: 0,
    useYn: "Y",
  },
  stocks: {
    countryCode: "KR",
    currencyCode: "KRW",
    assetType: "STOCK",
    stockGrade: "CORE",
    regularBuyYn: "Y",
    additionalBuyYn: "Y",
    rebuyYn: "Y",
    useYn: "Y",
  },
};
async function call<T>(url: string, init?: RequestInit) {
  const res = await fetch(url, init),
    body = (await res.json()) as ApiResult<T>;
  if (!res.ok || !body.success)
    throw new Error(body.error?.message || `HTTP ${res.status}`);
  return body.data;
}
const formatAmountInput = (value: unknown) => {
  if (value === null || value === undefined || value === "") return "";
  const [integer, decimal] = String(value).replaceAll(",", "").split(".");
  const formatted = Number(integer || 0).toLocaleString("ko-KR");
  return decimal === undefined ? formatted : `${formatted}.${decimal}`;
};
export default function ReferenceAdmin({
  notify,
}: {
  notify: (s: string) => void;
}) {
  const [kind, setKind] = useState<Kind>("indices"),
    [rows, setRows] = useState<Row[]>([]),
    [indices, setIndices] = useState<Row[]>([]),
    [loading, setLoading] = useState(true),
    [editing, setEditing] = useState<Row | null>(null),
    [form, setForm] = useState<Row>({}),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false),
    [query, setQuery] = useState("");
  const load = async (k = kind) => {
    setLoading(true);
    setError("");
    try {
      const data = await call<Row[]>(`/api/v1/admin/reference/${k}`);
      setRows(data);
      if (k === "indices") setIndices(data);
      else if (!indices.length)
        setIndices(await call<Row[]>("/api/v1/admin/reference/indices"));
    } catch (e) {
      setError(e instanceof Error ? e.message : "조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load(kind);
  }, [kind]);
  const open = (row?: Row) => {
    setEditing(row || {});
    setForm(row ? { ...row } : { ...defaults[kind] });
    setError("");
  };
  const save = async () => {
    for (const f of meta[kind].fields)
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
      const id = form[meta[kind].id],
        url = `/api/v1/admin/reference/${kind}${id ? `/${id}` : ""}`;
      await call(url, {
        method: id ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      setEditing(null);
      await load();
      notify(`${meta[kind].title} 정보가 저장되었습니다.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };
  const shown = rows.filter((r) =>
      Object.values(r).some((v) =>
        String(v ?? "")
          .toLowerCase()
          .includes(query.toLowerCase()),
      ),
    ),
    display = (key: string, v: any, r: Row) =>
      key === "baseIndexId"
        ? indices.find((i) => i.indexId === v)?.indexName || "-"
        : key === "baseCurrencyCode" && r.accountType === "OVERSEAS"
          ? "달러"
          : key.endsWith("Amount")
            ? Number(v || 0).toLocaleString("ko-KR")
            : v === "Y"
              ? "예"
              : v === "N"
                ? "아니오"
                : String(v ?? "-");
  return (
    <div className="page ref-page">
      <section className="ref-intro">
        <div>
          <h2>기준정보 관리</h2>
          <p>외부 API로 수집된 기준지수를 조회하고 계좌 정보를 관리합니다.</p>
        </div>
        {kind === "accounts" && (
          <button className="primary" onClick={() => open()}>
            + 계좌 등록
          </button>
        )}
      </section>
      <div className="tabs ref-tabs">
        {(["indices", "accounts"] as Kind[]).map((k) => (
          <button
            key={k}
            className={kind === k ? "active" : ""}
            onClick={() => {
              setKind(k);
              setQuery("");
            }}
          >
            {meta[k].title} 정보
          </button>
        ))}
      </div>
      <section className="card table ref-table">
        <header className="head list-tools">
          {kind !== "accounts" && (
            <input
              className="ref-search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="코드 또는 이름 검색"
            />
          )}
        </header>
        {error && !editing && <p className="form-error ref-error">{error}</p>}
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                {meta[kind].columns.map((c) => (
                  <th key={c[0]}>{c[1]}</th>
                ))}
                {kind === "accounts" && <th></th>}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={20} className="data-state">
                    불러오는 중입니다.
                  </td>
                </tr>
              ) : shown.length === 0 ? (
                <tr>
                  <td colSpan={20} className="data-state">
                    등록된 정보가 없습니다.
                  </td>
                </tr>
              ) : (
                shown.map((r) => (
                  <tr key={r[meta[kind].id]}>
                    {meta[kind].columns.map((c) => (
                      <td key={c[0]}>{display(c[0], r[c[0]], r)}</td>
                    ))}
                    {kind === "accounts" && (
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
                  {meta[kind].title} {form[meta[kind].id] ? "수정" : "등록"}
                </h3>
                <p>* 표시는 필수 입력 항목입니다.</p>
              </div>
              <button className="modal-close" onClick={() => setEditing(null)}>
                ×
              </button>
            </header>
            <div className="ref-form">
              {meta[kind].fields.map((f) => (
                <label key={f.key} className={f.wide ? "wide" : ""}>
                  <span>
                    {f.label}
                    {f.required && <b> *</b>}
                  </span>
                  {f.type === "select" ? (
                    <select
                      value={form[f.key] ?? ""}
                      onChange={(e) =>
                        setForm({ ...form, [f.key]: e.target.value })
                      }
                    >
                      {f.options?.map((o) => (
                        <option key={o[0]} value={o[0]}>
                          {o[1]}
                        </option>
                      ))}
                    </select>
                  ) : f.type === "index" ? (
                    <select
                      value={form[f.key] ?? ""}
                      onChange={(e) =>
                        setForm({
                          ...form,
                          [f.key]: e.target.value
                            ? Number(e.target.value)
                            : null,
                        })
                      }
                    >
                      <option value="">선택 안 함</option>
                      {indices
                        .filter((i) => i.useYn === "Y")
                        .map((i) => (
                          <option key={i.indexId} value={i.indexId}>
                            {i.indexName} ({i.indexCode})
                          </option>
                        ))}
                    </select>
                  ) : (
                    <input
                      type={
                        f.key.endsWith("Amount") || f.type === "readonly"
                          ? "text"
                          : f.type || "text"
                      }
                      inputMode={
                        f.key.endsWith("Amount") ? "decimal" : undefined
                      }
                      disabled={f.type === "readonly"}
                      value={
                        f.key.endsWith("Amount")
                          ? formatAmountInput(form[f.key])
                          : (form[f.key] ?? "")
                      }
                      onChange={(e) => {
                        const raw = e.target.value.replaceAll(",", "");
                        if (
                          f.key.endsWith("Amount") &&
                          raw !== "" &&
                          !/^\d*(\.\d*)?$/.test(raw)
                        )
                          return;
                        setForm({
                          ...form,
                          [f.key]:
                            (f.type === "number" ||
                              f.key.endsWith("Amount")) &&
                            raw !== ""
                              ? Number(raw)
                              : raw,
                        });
                      }}
                    />
                  )}
                  {f.help && <small>{f.help}</small>}
                </label>
              ))}
            </div>
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
