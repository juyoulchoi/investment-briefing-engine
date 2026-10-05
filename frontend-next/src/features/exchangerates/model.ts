import type { ExchangeRate } from "./types";
export const WIDTH = 1200;
export const HEIGHT = 480;
export const MARGIN = { top: 28, right: 28, bottom: 50, left: 82 };
export const plotWidth = WIDTH - MARGIN.left - MARGIN.right;
export const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom;
export const iso = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};
export const fiveYearRange = () => {
  const to = new Date();
  const from = new Date(to);
  from.setFullYear(from.getFullYear() - 5);
  return { from: iso(from), to: iso(to) };
};
export const subtractYears = (dateValue: string, years: number) => {
  const date = new Date(`${dateValue}T00:00:00`);
  date.setFullYear(date.getFullYear() - years);
  return iso(date);
};
export const wonRate = (value: number, digits = 2) =>
  `${Number(value).toLocaleString("ko-KR", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })}원`;
export const dateLabel = (value: string) =>
  new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(`${value}T00:00:00`));

export function buildExchangeChart(rows: ExchangeRate[]) {
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
}

export function buildPeriodAverages(rows: ExchangeRate[], to: string) {
  if (!rows.length) return [];
  return [3, 4, 5].map((years) => {
    const from = subtractYears(to, years);
    const periodRows = rows.filter((row) => row.base_date >= from);
    const average =
      periodRows.reduce((sum, row) => sum + Number(row.exchange_rate), 0) /
      periodRows.length;
    return { years, from, average, count: periodRows.length };
  });
}
