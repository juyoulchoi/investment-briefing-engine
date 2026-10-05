"use client";
import { formatAmountInput, meta } from "./model";
import type { Kind } from "./types";

import { useReference } from "./useReference";
export default function ReferenceView() {
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
    query,
    setQuery,
    open,
    save,
    shown,
    display,
  } = useReference();
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
                            (f.type === "number" || f.key.endsWith("Amount")) &&
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
