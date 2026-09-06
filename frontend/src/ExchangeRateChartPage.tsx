import React, { useEffect, useMemo, useRef, useState } from "react";
import "./exchange-rate-chart.css";

type ExchangeRate = {
  base_date: string;
  base_currency: string;
  quote_currency: string;
  exchange_rate: number;
  previous_exchange_rate: number | null;
  change_amount: number | null;
  change_rate: number | null;
  high_52week_rate: number | null;
  low_52week_rate: number | null;
  data_source_code: string;
  data_status: string;
  collected_at: string;
};

const WIDTH = 1200;
const HEIGHT = 480;
const MARGIN = { top: 28, right: 28, bottom: 50, left: 82 };
const plotWidth = WIDTH - MARGIN.left - MARGIN.right;
const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom;

const iso = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const fiveYearRange = () => {
  const to = new Date();
  const from = new Date(to);
  from.setFullYear(from.getFullYear() - 5);
  return { from: iso(from), to: iso(to) };
};

const wonRate = (value: number, digits = 2) =>
  `${Number(value).toLocaleString("ko-KR", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })}원`;

const dateLabel = (value: string) =>
  new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(`${value}T00:00:00`));

async function requestExchangeRates(from: string, to: string) {
  const response = await fetch(
    `/api/market-data/exchange-rates?baseCurrency=USD&quoteCurrency=KRW&from=${from}&to=${to}`,
  );
  const body = await response.json();
  if (!response.ok || body?.success === false)
    throw new Error(body?.error?.message || `HTTP ${response.status}`);
  return (body?.data ?? body) as ExchangeRate[];
}

export default function ExchangeRateChartPage() {
  const range = useMemo(fiveYearRange, []);
  const [rows, setRows] = useState<ExchangeRate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const chartRef = useRef<SVGSVGElement>(null);

  useEffect(() => {
    let alive = true;
    requestExchangeRates(range.from, range.to)
      .then((data) => {
        if (!alive) return;
        setRows(
          [...data].sort((a, b) => a.base_date.localeCompare(b.base_date)),
        );
      })
      .catch((reason) => {
        if (alive)
          setError(
            reason instanceof Error
              ? reason.message
              : "환율 데이터를 불러오지 못했습니다.",
          );
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [range.from, range.to]);

  const chart = useMemo(() => {
    if (!rows.length) return null;
    const values = rows.map((row) => Number(row.exchange_rate));
    const rawMin = Math.min(...values);
    const rawMax = Math.max(...values);
    const padding = Math.max((rawMax - rawMin) * 0.08, 10);
    const min = Math.floor((rawMin - padding) / 10) * 10;
    const max = Math.ceil((rawMax + padding) / 10) * 10;
    const x = (index: number) =>
      MARGIN.left + (index / Math.max(rows.length - 1, 1)) * plotWidth;
    const y = (value: number) =>
      MARGIN.top + ((max - value) / Math.max(max - min, 1)) * plotHeight;
    const points = rows
      .map((row, index) => `${x(index)},${y(Number(row.exchange_rate))}`)
      .join(" ");
    const yTicks = Array.from({ length: 5 }, (_, index) => {
      const value = min + ((max - min) * index) / 4;
      return { value, y: y(value) };
    }).reverse();
    const tickCount = Math.min(6, rows.length);
    const xTicks = Array.from({ length: tickCount }, (_, index) => {
      const rowIndex = Math.round(
        (index * Math.max(rows.length - 1, 0)) / Math.max(tickCount - 1, 1),
      );
      return { index: rowIndex, row: rows[rowIndex], x: x(rowIndex) };
    });
    return { rawMin, rawMax, x, y, points, yTicks, xTicks };
  }, [rows]);

  const latest = rows.at(-1);
  const first = rows[0];
  const periodChange =
    latest && first
      ? ((Number(latest.exchange_rate) - Number(first.exchange_rate)) /
          Number(first.exchange_rate)) *
        100
      : null;
  const hovered = hoverIndex == null ? null : rows[hoverIndex];

  const selectPoint = (clientX: number) => {
    if (!chartRef.current || !rows.length) return;
    const bounds = chartRef.current.getBoundingClientRect();
    const svgX = ((clientX - bounds.left) / bounds.width) * WIDTH;
    const ratio = Math.min(
      1,
      Math.max(0, (svgX - MARGIN.left) / plotWidth),
    );
    setHoverIndex(Math.round(ratio * (rows.length - 1)));
  };

  return (
    <div className="page exchange-page">
      <section className="exchange-heading">
        <div>
          <span className="exchange-eyebrow">USD / KRW</span>
          <h2>최근 5년 원·달러 환율</h2>
          <p>
            Yahoo Finance에서 수집한 일별 종가 기준입니다. 기준통화 1달러당
            원화 환율을 표시합니다.
          </p>
        </div>
        <div className="exchange-period">
          <small>조회 기간</small>
          <strong>
            {range.from} ~ {range.to}
          </strong>
        </div>
      </section>

      {error && <section className="card exchange-error">{error}</section>}
      {loading ? (
        <section className="card exchange-state">환율을 불러오는 중입니다.</section>
      ) : !chart || !latest ? (
        <section className="card exchange-state">
          최근 5년 동안 수집된 환율이 없습니다.
        </section>
      ) : (
        <>
          <section className="exchange-stats">
            <article className="card primary-stat">
              <small>최근 환율</small>
              <strong>{wonRate(Number(latest.exchange_rate))}</strong>
              <span>{dateLabel(latest.base_date)}</span>
            </article>
            <article className="card">
              <small>5년 최고</small>
              <strong>{wonRate(chart.rawMax)}</strong>
              <span>일별 종가 기준</span>
            </article>
            <article className="card">
              <small>5년 최저</small>
              <strong>{wonRate(chart.rawMin)}</strong>
              <span>일별 종가 기준</span>
            </article>
            <article className="card">
              <small>기간 변동률</small>
              <strong className={(periodChange ?? 0) >= 0 ? "up" : "down"}>
                {(periodChange ?? 0) > 0 ? "+" : ""}
                {periodChange?.toFixed(2)}%
              </strong>
              <span>{rows.length.toLocaleString("ko-KR")}개 관측치</span>
            </article>
          </section>

          <section className="card exchange-chart-card">
            <div className="chart-title-row">
              <div>
                <h3>USD/KRW 일별 환율 추이</h3>
                <p>차트 위를 움직이면 해당 날짜의 환율을 확인할 수 있습니다.</p>
              </div>
              <span className="chart-source">출처 · YAHOO</span>
            </div>
            <div className="exchange-chart-wrap">
              <svg
                ref={chartRef}
                className="exchange-chart"
                viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
                role="img"
                aria-label={`${range.from}부터 ${range.to}까지 USD/KRW 환율 꺾은선 차트`}
                onMouseMove={(event) => selectPoint(event.clientX)}
                onMouseLeave={() => setHoverIndex(null)}
                onTouchMove={(event) => selectPoint(event.touches[0].clientX)}
              >
                <defs>
                  <linearGradient id="exchangeArea" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#238765" stopOpacity="0.24" />
                    <stop offset="100%" stopColor="#238765" stopOpacity="0.01" />
                  </linearGradient>
                </defs>
                {chart.yTicks.map((tick) => (
                  <g key={tick.value}>
                    <line
                      className="chart-grid"
                      x1={MARGIN.left}
                      x2={WIDTH - MARGIN.right}
                      y1={tick.y}
                      y2={tick.y}
                    />
                    <text className="chart-axis-label" x={MARGIN.left - 14} y={tick.y + 5} textAnchor="end">
                      {Math.round(tick.value).toLocaleString("ko-KR")}
                    </text>
                  </g>
                ))}
                {chart.xTicks.map((tick) => (
                  <text
                    className="chart-axis-label"
                    key={`${tick.row.base_date}-${tick.index}`}
                    x={tick.x}
                    y={HEIGHT - 17}
                    textAnchor={
                      tick.index === 0
                        ? "start"
                        : tick.index === rows.length - 1
                          ? "end"
                          : "middle"
                    }
                  >
                    {tick.row.base_date.slice(0, 7)}
                  </text>
                ))}
                <polygon
                  className="exchange-area"
                  points={`${MARGIN.left},${MARGIN.top + plotHeight} ${chart.points} ${WIDTH - MARGIN.right},${MARGIN.top + plotHeight}`}
                />
                <polyline className="exchange-line" points={chart.points} />
                {hovered && hoverIndex != null && (
                  <g className="chart-focus">
                    <line
                      x1={chart.x(hoverIndex)}
                      x2={chart.x(hoverIndex)}
                      y1={MARGIN.top}
                      y2={MARGIN.top + plotHeight}
                    />
                    <circle
                      cx={chart.x(hoverIndex)}
                      cy={chart.y(Number(hovered.exchange_rate))}
                      r="6"
                    />
                  </g>
                )}
              </svg>
              {hovered && hoverIndex != null && (
                <div
                  className="exchange-tooltip"
                  style={{
                    left: `${(chart.x(hoverIndex) / WIDTH) * 100}%`,
                    top: `${(chart.y(Number(hovered.exchange_rate)) / HEIGHT) * 100}%`,
                  }}
                >
                  <small>{dateLabel(hovered.base_date)}</small>
                  <strong>{wonRate(Number(hovered.exchange_rate))}</strong>
                  <span className={(hovered.change_rate ?? 0) >= 0 ? "up" : "down"}>
                    전일 대비 {(hovered.change_rate ?? 0) > 0 ? "+" : ""}
                    {Number(hovered.change_rate ?? 0).toFixed(2)}%
                  </span>
                </div>
              )}
            </div>
            <footer className="exchange-chart-footer">
              <span>시작 {wonRate(Number(first.exchange_rate))}</span>
              <span>최근 수집 상태 · {latest.data_status === "FRESH" ? "정상" : latest.data_status}</span>
            </footer>
          </section>
        </>
      )}
    </div>
  );
}
