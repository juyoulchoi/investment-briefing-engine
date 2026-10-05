"use client";
import { useEffect, useMemo, useRef, useState } from "react";
import type { DateRange } from "../../shared/date";
import { getExchangeRates } from "./api";
import {
  buildExchangeChart,
  buildPeriodAverages,
  MARGIN,
  plotWidth,
  WIDTH,
} from "./model";
import type { ExchangeRate } from "./types";
export function useExchangeRates(initialRange: DateRange) {
  const range = initialRange;
  const [rows, setRows] = useState<ExchangeRate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const chartRef = useRef<SVGSVGElement>(null);
  useEffect(() => {
    let alive = true;
    getExchangeRates(range.from, range.to)
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
  const chart = useMemo(() => buildExchangeChart(rows), [rows]);
  const latest = rows.at(-1);
  const first = rows[0];
  const periodAverages = useMemo(
    () => buildPeriodAverages(rows, range.to),
    [range.to, rows],
  );
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
    const ratio = Math.min(1, Math.max(0, (svgX - MARGIN.left) / plotWidth));
    setHoverIndex(Math.round(ratio * (rows.length - 1)));
  };
  return {
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
  };
}
