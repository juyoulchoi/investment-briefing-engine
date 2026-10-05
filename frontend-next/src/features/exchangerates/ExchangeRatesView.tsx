"use client";
import type { DateRange } from "../../shared/date";
import "./exchange-rate-chart.css";
import { dateLabel, HEIGHT, MARGIN, plotHeight, WIDTH, wonRate } from "./model";
import { useExchangeRates } from "./useExchangeRates";
export default function ExchangeRatesView({
  initialRange,
}: {
  initialRange: DateRange;
}) {
  const {
    range,
    rows,
    loading,
    error,
    hoverIndex,
    setHoverIndex,
    chartRef,
    chart,
    latest,
    first,
    periodAverages,
    periodChange,
    hovered,
    selectPoint,
  } = useExchangeRates(initialRange);
  return (
    <div className="page exchange-page">
      <section className="exchange-heading">
        <div>
          <span className="exchange-eyebrow">USD / KRW</span>
          <h2>최근 5년 원·달러 환율</h2>
          <p>
            Yahoo Finance에서 수집한 일별 종가 기준입니다. 기준통화 1달러당 원화
            환율을 표시합니다.
          </p>
        </div>
        <div className="exchange-period">
          <small>조회일</small>
          <strong>{range.to}</strong>
        </div>
      </section>

      {error && <section className="card exchange-error">{error}</section>}
      {loading ? (
        <section className="card exchange-state">
          환율을 불러오는 중입니다.
        </section>
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
            {periodAverages.map((item) => (
              <article className="card exchange-average-card" key={item.years}>
                <small>최근 {item.years}년 평균</small>
                <strong>{wonRate(item.average)}</strong>
                <span>{item.count.toLocaleString("ko-KR")}개 관측치</span>
              </article>
            ))}
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
                    <stop
                      offset="100%"
                      stopColor="#238765"
                      stopOpacity="0.01"
                    />
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
                    <text
                      className="chart-axis-label"
                      x={MARGIN.left - 14}
                      y={tick.y + 5}
                      textAnchor="end"
                    >
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
                  <span
                    className={(hovered.change_rate ?? 0) >= 0 ? "up" : "down"}
                  >
                    전일 대비 {(hovered.change_rate ?? 0) > 0 ? "+" : ""}
                    {Number(hovered.change_rate ?? 0).toFixed(2)}%
                  </span>
                </div>
              )}
            </div>
            <footer className="exchange-chart-footer">
              <span>시작 {wonRate(Number(first.exchange_rate))}</span>
              <span>
                최근 수집 상태 ·{" "}
                {latest.data_status === "FRESH" ? "정상" : latest.data_status}
              </span>
            </footer>
          </section>
        </>
      )}
    </div>
  );
}
