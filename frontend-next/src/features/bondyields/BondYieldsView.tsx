"use client";
import type { DateRange } from "../../shared/date";
import "./bond-yields.css";
import { useBondYields } from "./useBondYields";
export default function BondYieldsView({
  initialRange,
}: {
  initialRange: DateRange;
}) {
  const {
    from,
    setFrom,
    to,
    setTo,
    selected,
    setSelected,
    series,
    loading,
    collecting,
    error,
    load,
    collect,
    shown,
    latest,
    rate,
    bp,
  } = useBondYields(initialRange);
  return (
    <div className="page ref-page bond-page">
      <section className="ref-intro bond-intro">
        <div>
          <h2>미국 채권금리</h2>
          <p>
            FRED에서 수집한 미국 국채와 물가연동국채의 일별 금리 흐름을
            조회합니다.
          </p>
        </div>
        <button className="primary" disabled={collecting} onClick={collect}>
          {collecting ? "갱신 중..." : "↻ FRED 데이터 갱신"}
        </button>
      </section>
      <section className="bond-latest">
        {latest.map(({ item, row }) => (
          <article className="card" key={item.code}>
            <small>{item.name}</small>
            <strong>{rate(row?.yield_rate)}</strong>
            <span
              className={
                (row?.change_basis_points || 0) > 0
                  ? "neg"
                  : (row?.change_basis_points || 0) < 0
                    ? "pos"
                    : ""
              }
            >
              {bp(row?.change_basis_points ?? null)}
            </span>
            <em>{row?.base_date || "데이터 없음"}</em>
          </article>
        ))}
      </section>
      <section className="card bond-filter">
        <label>
          시작일
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
        </label>
        <label>
          종료일
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </label>
        <label>
          만기
          <select
            value={selected}
            onChange={(e) => setSelected(e.target.value)}
          >
            {series.map((item) => (
              <option key={item.code} value={item.code}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
        <button className="primary" onClick={() => load()}>
          조회
        </button>
      </section>
      <section className="card table ref-table">
        {error && <p className="form-error ref-error">{error}</p>}
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                <th>기준일</th>
                <th>채권명</th>
                <th>FRED 코드</th>
                <th>만기</th>
                <th>금리</th>
                <th>전일 금리</th>
                <th>변동</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="data-state">
                    불러오는 중입니다.
                  </td>
                </tr>
              ) : shown.length === 0 ? (
                <tr>
                  <td colSpan={8} className="data-state">
                    조회 기간에 수집된 채권금리가 없습니다.
                  </td>
                </tr>
              ) : (
                shown.map((row) => (
                  <tr key={`${row.base_date}-${row.bond_code}`}>
                    <td>{row.base_date}</td>
                    <td>
                      <b>{row.bond_name}</b>
                    </td>
                    <td>{row.bond_code}</td>
                    <td>
                      {row.maturity_months >= 12
                        ? `${row.maturity_months / 12}년`
                        : `${row.maturity_months}개월`}
                    </td>
                    <td>
                      <b>{rate(row.yield_rate)}</b>
                    </td>
                    <td>{rate(row.previous_yield_rate)}</td>
                    <td
                      className={
                        (row.change_basis_points || 0) > 0
                          ? "neg"
                          : (row.change_basis_points || 0) < 0
                            ? "pos"
                            : ""
                      }
                    >
                      {bp(row.change_basis_points)}
                    </td>
                    <td>
                      <span className="badge buy">
                        {row.data_status === "FRESH" ? "정상" : row.data_status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
