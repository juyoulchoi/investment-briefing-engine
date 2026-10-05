"use client";
import { config, labels } from "./model";
import type { Field } from "./types";

import { useOperations } from "./useOperations";
export default function OperationsView() {
  const {
    changeFormAccount,
    changeFormGrade,
    changeFormSelection,
    kind,
    accounts,
    investmentGrades,
    pauseReasons,
    loading,
    editing,
    setEditing,
    form,
    setForm,
    error,
    saving,
    query,
    setQuery,
    appliedCycleFilter,
    setAppliedCycleFilter,
    buyStatusFilter,
    setBuyStatusFilter,
    userPauseFilter,
    setUserPauseFilter,
    selectedAccount,
    setSelectedAccount,
    open,
    normalizedRegularBuyForm,
    save,
    shown,
    show,
    regularBuyDetails,
    regularBuyDetailRow,
    stockChoices,
    fixedBaseField,
    csvValues,
    toggleCsv,
  } = useOperations();
  const input = (f: Field) =>
    f.type === "account" ? (
      <select
        disabled={kind === "regular-buys" && Boolean(form.regularBuyKey)}
        value={form[f.key] ?? ""}
        onChange={(e) => changeFormAccount(f, e.target.value)}
      >
        <option value="">계좌 선택</option>
        {accounts.map((a) => (
          <option key={a.accountId} value={a.accountId}>
            {labels[a.accountType] || a.accountType}
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
            {s.stockCode} · {s.stockName}
          </option>
        ))}
      </select>
    ) : f.type === "investmentGrade" ? (
      <select
        value={form[f.key] ?? ""}
        onChange={(e) => changeFormGrade(f, e.target.value)}
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
            disabled={
              fixedBaseField(f.key) ||
              (kind === "regular-buys" &&
                (form.buyStatus === "STOPPED" || form.userPauseYn === "Y"))
            }
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
            disabled={
              kind === "regular-buys" &&
              (form.buyStatus === "STOPPED" || form.userPauseYn === "Y")
            }
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
          fixedBaseField(f.key) ||
          (kind === "regular-buys" &&
            f.key === "pauseReason" &&
            form.buyStatus !== "STOPPED" &&
            form.userPauseYn !== "Y")
        }
        value={form[f.key] ?? ""}
        onChange={(e) => changeFormSelection(f, e.target.value)}
      >
        {(kind === "regular-buys" && f.key === "pauseReason"
          ? [["", "선택 안 함"], ...pauseReasons.map((r) => [r.name, r.name])]
          : f.options
        )?.map((o) => (
          <option key={o[0]} value={o[0]}>
            {o[1]}
          </option>
        ))}
      </select>
    ) : (
      <input
        type={f.type || "text"}
        disabled={
          fixedBaseField(f.key) ||
          (kind === "holdings" &&
            f.key === "averagePrice" &&
            (form.accountType ??
              accounts.find(
                (a) => Number(a.accountId) === Number(form.accountId),
              )?.accountType) === "DOMESTIC")
        }
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
          <p>계좌별 정기매수 기준과 현재 적용 계획을 관리합니다.</p>
        </div>
        <button className="primary" onClick={() => open()}>
          + {config[kind].title} 등록
        </button>
      </section>
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
                  setAppliedCycleFilter("");
                  setBuyStatusFilter("");
                  setUserPauseFilter("");
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
                placeholder="종목 코드 또는 종목명 검색"
                aria-label="종목 검색"
              />
              <select
                className="status-filter"
                value={appliedCycleFilter}
                onChange={(e) => setAppliedCycleFilter(e.target.value)}
                aria-label="모으기 주기 검색"
              >
                <option value="">모으기 주기</option>
                <option value="DAILY">매일</option>
                <option value="WEEKLY">매주</option>
                <option value="MONTHLY">매월</option>
              </select>
              <select
                className="status-filter"
                value={buyStatusFilter}
                onChange={(e) => setBuyStatusFilter(e.target.value)}
                aria-label="매수 상태 검색"
              >
                <option value="">매수 상태</option>
                <option value="ACTIVE">활성</option>
                <option value="STOPPED">중지</option>
              </select>
              <select
                className="status-filter"
                value={userPauseFilter}
                onChange={(e) => setUserPauseFilter(e.target.value)}
                aria-label="사용자 일시정지 검색"
              >
                <option value="">사용자 일시정지</option>
                <option value="Y">예</option>
                <option value="N">아니오</option>
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
                  <th key={c[0]}>
                    {kind === "regular-buys" &&
                    c[0] === "appliedValue" &&
                    ["ISA", "PENSION"].includes(selectedAccount)
                      ? "현재 매수수량"
                      : c[1]}
                  </th>
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
                  <tr
                    key={r[config[kind].id]}
                    className={
                      kind === "regular-buys" && r.userPauseYn === "Y"
                        ? "user-paused-row"
                        : kind === "regular-buys" && r.buyStatus === "STOPPED"
                          ? "buy-stopped-row"
                          : kind === "regular-buys" && r.buyStatus === "PAUSED"
                            ? "buy-paused-row"
                            : undefined
                    }
                  >
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
                    (!f.domesticOnly ||
                      (form.accountType ??
                        accounts.find(
                          (a) => Number(a.accountId) === Number(form.accountId),
                        )?.accountType) === "DOMESTIC") &&
                    (kind !== "regular-buys" ||
                      ((f.key !== "buyDayCode" || form.buyCycle === "WEEKLY") &&
                        (f.key !== "buyDayNumbers" ||
                          form.buyCycle === "MONTHLY") &&
                        (f.key !== "appliedWeekDays" ||
                          form.appliedCycle === "WEEKLY") &&
                        (f.key !== "appliedMonthDays" ||
                          form.appliedCycle === "MONTHLY") &&
                        (!["minimumBuyAmount", "appliedAmount"].includes(
                          f.key,
                        ) ||
                          form.buyBasis === "AMOUNT") &&
                        (!["baseBuyQuantity", "buyQuantity"].includes(f.key) ||
                          form.buyBasis === "QUANTITY"))),
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
