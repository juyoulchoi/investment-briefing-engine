import React, { useEffect, useMemo, useState } from "react";
import "./bond-yields.css";

type BondYield = {
  base_date: string;
  bond_code: string;
  bond_name: string;
  country_code: string;
  maturity_months: number;
  yield_rate: number;
  previous_yield_rate: number | null;
  change_basis_points: number | null;
  data_source_code: string;
  data_status: string;
};
const series = [
  { code: "ALL", name: "전체 만기" },
  { code: "DGS2", name: "미국 국채 2년" },
  { code: "DGS10", name: "미국 국채 10년" },
  { code: "DGS30", name: "미국 국채 30년" },
  { code: "DFII10", name: "미국 물가연동국채 10년" },
];
const iso = (date: Date) => {
  const year = date.getFullYear(),
    month = String(date.getMonth() + 1).padStart(2, "0"),
    day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};
const initialFrom = () => {
  const date = new Date();
  date.setDate(date.getDate() - 90);
  return iso(date);
};
async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init),
    body = await response.json();
  if (!response.ok || body?.success === false)
    throw new Error(body?.error?.message || `HTTP ${response.status}`);
  return (body?.data ?? body) as T;
}

export default function BondYieldPage({
  notify,
}: {
  notify: (message: string) => void;
}) {
  const [from, setFrom] = useState(initialFrom),
    [to, setTo] = useState(() => iso(new Date())),
    [selected, setSelected] = useState("ALL"),
    [rows, setRows] = useState<BondYield[]>([]),
    [loading, setLoading] = useState(true),
    [collecting, setCollecting] = useState(false),
    [error, setError] = useState("");
  const load = async (loadFrom = from, loadTo = to) => {
    if (loadFrom > loadTo) {
      setError("시작일은 종료일보다 늦을 수 없습니다.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      setRows(
        await request<BondYield[]>(
          `/api/market-data/bonds?from=${loadFrom}&to=${loadTo}`,
        ),
      );
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "채권금리를 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load();
  }, []);
  const collect = async () => {
    setCollecting(true);
    setError("");
    try {
      const result = await request<{
        from: string;
        to: string;
        savedCount: number;
        latestObservationDate: string | null;
      }>("/api/market-data/bonds/refresh", { method: "POST" });
      const nextFrom = from > result.to ? result.from : from;
      setFrom(nextFrom);
      setTo(result.to);
      await load(nextFrom, result.to);
      notify(
        result.latestObservationDate
          ? `FRED 채권금리 ${result.savedCount.toLocaleString("ko-KR")}건을 갱신했습니다. 최신 관측일: ${result.latestObservationDate}`
          : "FRED에서 수집 가능한 최신 관측치가 없습니다.",
      );
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "FRED 데이터 갱신에 실패했습니다.",
      );
    } finally {
      setCollecting(false);
    }
  };
  const shown = useMemo(
    () =>
      selected === "ALL"
        ? rows
        : rows.filter((row) => row.bond_code === selected),
    [rows, selected],
  );
  const latest = useMemo(
    () =>
      series.slice(1).map((item) => ({
        item,
        row: rows.find((row) => row.bond_code === item.code),
      })),
    [rows],
  );
  const rate = (value: number | null | undefined) =>
      value == null ? "-" : `${Number(value).toFixed(3)}%`,
    bp = (value: number | null) =>
      value == null
        ? "-"
        : `${value > 0 ? "+" : ""}${Number(value).toFixed(1)} bp`;
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
