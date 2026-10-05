"use client";
import { useEffect, useMemo, useState } from "react";
import type { DateRange } from "../../shared/date";
import { useNotify } from "../../shared/notifications";
import { getBondSeries, getBondYields, refreshBondYields } from "./api";
import type { BondYield, CommonCode } from "./types";
export function useBondYields(initialRange: DateRange) {
  const notify = useNotify();
  const [from, setFrom] = useState(initialRange.from),
    [to, setTo] = useState(initialRange.to),
    [selected, setSelected] = useState("ALL"),
    [series, setSeries] = useState<CommonCode[]>([]),
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
      setRows(await getBondYields(loadFrom, loadTo));
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "채권금리를 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    Promise.all([
      load(),
      getBondSeries().then((codes) => {
        setSeries(codes);
        if (!codes.some((code) => code.code === selected))
          setSelected(codes[0]?.code ?? "ALL");
      }),
    ]).catch((e) => {
      setError(
        e instanceof Error
          ? e.message
          : "채권금리 공통코드를 불러오지 못했습니다.",
      );
    });
  }, []);
  const collect = async () => {
    setCollecting(true);
    setError("");
    try {
      const result = await refreshBondYields();
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
      series
        .filter((item) => item.code !== "ALL")
        .map((item) => ({
          item,
          row: rows.find((row) => row.bond_code === item.code),
        })),
    [rows, series],
  );
  const rate = (value: number | null | undefined) =>
      value == null ? "-" : `${Number(value).toFixed(3)}%`,
    bp = (value: number | null) =>
      value == null
        ? "-"
        : `${value > 0 ? "+" : ""}${Number(value).toFixed(1)} bp`;
  return {
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
  };
}
