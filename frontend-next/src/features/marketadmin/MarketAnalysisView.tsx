"use client";
import { cfg } from "./model";
import type { Field, Kind } from "./types";

import { useMarketAnalysis } from "./useMarketAnalysis";
export default function MarketAnalysisView() {
  const {
    kind,
    setKind,
    indices,
    loading,
    editing,
    setEditing,
    form,
    setForm,
    error,
    saving,
    baseDateQuery,
    setBaseDateQuery,
    marketQuery,
    setMarketQuery,
    marketOptions,
    save,
    shown,
    show,
  } = useMarketAnalysis();
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
