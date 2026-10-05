import React, { useEffect, useState } from "react";
type Kind = "snapshots" | "sentiments";
type Row = Record<string, any>;
type ApiResult<T> = { success: boolean; data: T; error?: { message?: string } };
type Field = {
  key: string;
  label: string;
  type?: string;
  required?: boolean;
  options?: [string, string][];
};
const status: [string, string][] = [
  ["FRESH", "정상"],
  ["STALE", "지연"],
  ["MISSING", "누락"],
  ["PARTIAL", "일부"],
  ["ERROR", "오류"],
];
const cfg: Record<
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
  snapshots: {
    title: "일별 시장 스냅샷",
    table: "TB_MKT_SNAP",
    id: "marketSnapshotId",
    columns: [
      ["baseDate", "기준일"],
      ["marketSnapshotCode", "시장 코드"],
      ["marketName", "시장명"],
      ["mainIndexName", "대표지수"],
      ["mainIndexValue", "지수값"],
      ["mainIndexChangeRate", "등락률"],
      ["marketBreadthRate", "시장 확산도"],
      ["dataStatus", "상태"],
    ],
    defaults: {
      baseDate: new Date().toISOString().slice(0, 10),
      dataSourceCode: "INTERNAL",
      dataStatus: "FRESH",
      dataAgeMinutes: 0,
    },
    fields: [
      { key: "baseDate", label: "기준일", type: "date", required: true },
      { key: "marketSnapshotCode", label: "시장 스냅샷 코드", required: true },
      { key: "marketName", label: "시장명", required: true },
      { key: "mainIndexCode", label: "대표 기준지수", type: "index" },
      { key: "mainIndexValue", label: "대표지수 값", type: "number" },
      {
        key: "mainIndexChangeRate",
        label: "대표지수 등락률 (%)",
        type: "number",
      },
      { key: "foreignNetAmount", label: "외국인 순매수금액", type: "number" },
      { key: "exchangeRate", label: "대표 환율", type: "number" },
      { key: "advancingStockCount", label: "상승 종목 수", type: "number" },
      { key: "decliningStockCount", label: "하락 종목 수", type: "number" },
      { key: "marketBreadthRate", label: "시장 확산도", type: "number" },
      { key: "dataSourceCode", label: "산출 출처 코드", required: true },
      {
        key: "dataStatus",
        label: "데이터 상태",
        type: "select",
        required: true,
        options: status,
      },
      { key: "dataAgeMinutes", label: "데이터 경과시간 (분)", type: "number" },
    ],
  },
  sentiments: {
    title: "일별 시장심리 분석",
    table: "TB_MKT_SENT",
    id: "marketSentimentId",
    columns: [
      ["baseDate", "기준일"],
      ["marketSnapshotCode", "시장 코드"],
      ["sentimentScore", "심리 점수"],
      ["sentimentPhase", "심리 단계"],
      ["confidenceRate", "신뢰도"],
      ["structuralDamageYn", "구조적 훼손"],
      ["ruleVersionNumber", "규칙 버전"],
      ["dataStatus", "상태"],
    ],
    defaults: {
      baseDate: new Date().toISOString().slice(0, 10),
      sentimentScore: 50,
      sentimentPhase: "NEUTRAL",
      confidenceRate: 50,
      structuralDamageYn: "N",
      ruleVersionNumber: 1,
      dataStatus: "FRESH",
    },
    fields: [
      { key: "baseDate", label: "기준일", type: "date", required: true },
      { key: "marketSnapshotCode", label: "시장 스냅샷 코드", required: true },
      { key: "newsFearScore", label: "뉴스 공포 점수", type: "number" },
      { key: "aiFatigueScore", label: "AI 투자 피로 점수", type: "number" },
      {
        key: "earningsConfidenceScore",
        label: "기업실적 신뢰도 점수",
        type: "number",
      },
      {
        key: "sentimentScore",
        label: "종합 시장심리 점수",
        type: "number",
        required: true,
      },
      {
        key: "sentimentPhase",
        label: "시장심리 단계",
        type: "select",
        required: true,
        options: [
          ["GREED", "탐욕"],
          ["OPTIMISM", "낙관"],
          ["NEUTRAL", "중립"],
          ["FATIGUE", "피로"],
          ["FEAR", "공포"],
          ["PANIC", "패닉"],
        ],
      },
      {
        key: "confidenceRate",
        label: "판단 신뢰도 (%)",
        type: "number",
        required: true,
      },
      {
        key: "structuralDamageYn",
        label: "구조적 훼손",
        type: "select",
        options: [
          ["N", "아니오"],
          ["Y", "예"],
        ],
      },
      {
        key: "ruleVersionNumber",
        label: "계산규칙 버전",
        type: "number",
        required: true,
      },
      {
        key: "dataStatus",
        label: "입력 데이터 상태",
        type: "select",
        required: true,
        options: status,
      },
    ],
  },
};
const label: Record<string, string> = {
  FRESH: "정상",
  STALE: "지연",
  MISSING: "누락",
  PARTIAL: "일부",
  ERROR: "오류",
  GREED: "탐욕",
  OPTIMISM: "낙관",
  NEUTRAL: "중립",
  FATIGUE: "피로",
  FEAR: "공포",
  PANIC: "패닉",
  Y: "예",
  N: "아니오",
};
async function call<T>(url: string, init?: RequestInit) {
  const r = await fetch(url, init),
    b = (await r.json()) as ApiResult<T>;
  if (!r.ok || !b.success)
    throw new Error(b.error?.message || `HTTP ${r.status}`);
  return b.data;
}
export default function MarketAnalysisAdmin({
  notify,
}: {
  notify: (s: string) => void;
}) {
  const [kind, setKind] = useState<Kind>("snapshots"),
    [rows, setRows] = useState<Row[]>([]),
    [indices, setIndices] = useState<Row[]>([]),
    [loading, setLoading] = useState(true),
    [editing, setEditing] = useState<Row | null>(null),
    [form, setForm] = useState<Row>({}),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false),
    [baseDateQuery, setBaseDateQuery] = useState(""),
    [marketQuery, setMarketQuery] = useState(""),
    [marketOptions, setMarketOptions] = useState<
      { code: string; name: string }[]
    >([]);
  const load = async (k = kind) => {
    setLoading(true);
    setError("");
    try {
      const [d, i, m] = await Promise.all([
        call<Row[]>(`/api/v1/admin/market-analysis/${k}`),
        indices.length
          ? Promise.resolve(indices)
          : call<Row[]>("/api/v1/admin/reference/indices"),
        call<Row[]>("/api/v1/admin/market-analysis/snapshots"),
      ]);
      setRows(
        [...d].sort(
          (a, b) =>
            String(b.baseDate ?? "").localeCompare(String(a.baseDate ?? "")) ||
            String(a.marketSnapshotCode ?? "").localeCompare(
              String(b.marketSnapshotCode ?? ""),
            ),
        ),
      );
      setIndices(i);
      setMarketOptions(
        m
          .filter(
            (row, index, all) =>
              all.findIndex(
                (candidate) =>
                  candidate.marketSnapshotCode === row.marketSnapshotCode,
              ) === index,
          )
          .map((row) => ({
            code: String(row.marketSnapshotCode),
            name: String(row.marketName),
          })),
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : "조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load(kind);
  }, [kind]);
  const open = (r?: Row) => {
    setEditing(r || {});
    setForm(r ? { ...r } : { ...cfg[kind].defaults });
    setError("");
  };
  const save = async () => {
    for (const f of cfg[kind].fields)
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
      const id = form[cfg[kind].id];
      await call(`/api/v1/admin/market-analysis/${kind}${id ? `/${id}` : ""}`, {
        method: id ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      setEditing(null);
      await load();
      notify(`${cfg[kind].title} 정보가 저장되었습니다.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };
  const shown = rows.filter(
      (r) =>
        (!baseDateQuery || String(r.baseDate ?? "") === baseDateQuery) &&
        (!marketQuery || String(r.marketSnapshotCode ?? "") === marketQuery),
    ),
    show = (k: string, v: any) => label[String(v)] || String(v ?? "-");
  const input = (f: Field) =>
    f.type === "index" ? (
      <select
        value={form[f.key] ?? ""}
        onChange={(e) =>
          setForm({
            ...form,
            [f.key]: e.target.value || null,
          })
        }
      >
        <option value="">선택 안 함</option>
        {indices
          .filter((i) => i.useYn === "Y")
          .map((i) => (
            <option key={i.indexCode} value={i.indexCode}>
              {i.indexName} ({i.indexCode})
            </option>
          ))}
      </select>
    ) : f.type === "select" ? (
      <select
        value={form[f.key] ?? ""}
        onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
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
        step={f.type === "number" ? "any" : undefined}
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
      <section className="ref-intro market-admin-intro">
        <div>
          <h2>시장 분석 관리</h2>
          <p>
            자동 생성된 시장 스냅샷과 시장심리 분석 결과를 조회하고 관리합니다.
          </p>
        </div>
      </section>
      <div className="tabs ref-tabs">
        {(["snapshots", "sentiments"] as Kind[]).map((k) => (
          <button
            key={k}
            className={kind === k ? "active" : ""}
            onClick={() => {
              setKind(k);
              setBaseDateQuery("");
              setMarketQuery("");
            }}
          >
            {cfg[k].title}
          </button>
        ))}
      </div>
      <section className="card table ref-table">
        <header className="head list-tools">
          <div className="list-actions market-search-fields">
            <input
              className="ref-search"
              type="date"
              aria-label="기준일 검색"
              value={baseDateQuery}
              onChange={(e) => setBaseDateQuery(e.target.value)}
            />
            <select
              className="ref-search"
              aria-label="시장 선택"
              value={marketQuery}
              onChange={(e) => setMarketQuery(e.target.value)}
            >
              <option value="">전체 시장</option>
              {marketOptions.map((market) => (
                <option key={market.code} value={market.code}>
                  {market.code} · {market.name}
                </option>
              ))}
            </select>
          </div>
        </header>
        {error && !editing && <p className="form-error ref-error">{error}</p>}
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                {cfg[kind].columns.map((c) => (
                  <th key={c[0]}>{c[1]}</th>
                ))}
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
                  <tr key={r[cfg[kind].id]}>
                    {cfg[kind].columns.map((c) => (
                      <td key={c[0]}>{show(c[0], r[c[0]])}</td>
                    ))}
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
                  {cfg[kind].title} {form[cfg[kind].id] ? "수정" : "등록"}
                </h3>
                <p>* 표시는 필수 입력 항목입니다.</p>
              </div>
              <button className="modal-close" onClick={() => setEditing(null)}>
                ×
              </button>
            </header>
            <div className="ref-form">
              {cfg[kind].fields.map((f) => (
                <label key={f.key}>
                  <span>
                    {f.label}
                    {f.required && <b> *</b>}
                  </span>
                  {input(f)}
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
