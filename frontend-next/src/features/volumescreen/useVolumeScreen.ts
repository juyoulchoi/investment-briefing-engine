"use client";
import { useEffect, useRef, useState } from "react";
import { getScreen, getSearchRuns, getTracking } from "./api";
import { todayInSeoul } from "./model";
import type { Candidate, Screen, SearchRun, Tracking } from "./types";
export function useVolumeScreen(initialDate: string) {
  const [baseDate, setBaseDate] = useState(initialDate);
  const [screen, setScreen] = useState<Screen | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("matches");
  const [market, setMarket] = useState("");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<Candidate | null>(null);
  const [tracking, setTracking] = useState<Tracking | null>(null);
  const [trackingDate, setTrackingDate] = useState("");
  const [trackingError, setTrackingError] = useState("");
  const [trackingBusy, setTrackingBusy] = useState(false);
  const [page, setPage] = useState(1);
  const [runs, setRuns] = useState<SearchRun[]>([]);
  const [runsError, setRunsError] = useState("");
  const [savedLabel, setSavedLabel] = useState("");
  const active = useRef<AbortController | null>(null);
  const activeTracking = useRef<AbortController | null>(null);
  const load = async (date: string, saved?: SearchRun) => {
    const queryDate = date === todayInSeoul() ? "" : date;
    active.current?.abort();
    activeTracking.current?.abort();
    const controller = new AbortController();
    active.current = controller;
    setLoading(true);
    setError("");
    setScreen(null);
    setSelected(null);
    setTracking(null);
    setTrackingBusy(false);
    setPage(1);
    setSavedLabel(
      saved
        ? `${new Date(saved.startedAt).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" })} 실행 당시 저장 결과`
        : "현재 저장된 시세로 조회한 결과",
    );
    try {
      setScreen(await getScreen(queryDate, saved, controller.signal));
    } catch (e) {
      if (!controller.signal.aborted)
        setError(e instanceof Error ? e.message : "조회 실패");
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  };
  useEffect(() => {
    void load(initialDate);
    const historyController = new AbortController();
    getSearchRuns(historyController.signal)
      .then(setRuns)
      .catch((e) => {
        if (!historyController.signal.aborted)
          setRunsError(
            e instanceof Error ? e.message : "자동 검색 이력 조회 실패",
          );
      });
    return () => {
      historyController.abort();
      active.current?.abort();
      activeTracking.current?.abort();
    };
  }, []);
  useEffect(() => {
    setPage(1);
  }, [query, market, filter]);
  const track = async () => {
    if (!selected) return;
    activeTracking.current?.abort();
    const controller = new AbortController();
    activeTracking.current = controller;
    setTrackingBusy(true);
    setTrackingError("");
    setTracking(null);

    try {
      setTracking(await getTracking(selected, trackingDate, controller.signal));
    } catch (e) {
      if (!controller.signal.aborted)
        setTrackingError(e instanceof Error ? e.message : "추적 실패");
    } finally {
      if (!controller.signal.aborted) setTrackingBusy(false);
    }
  };
  const choose = (row: Candidate) => {
    activeTracking.current?.abort();
    setSelected(row);
    setTracking(null);
    setTrackingError("");
    setTrackingBusy(false);
  };
  const shown = (screen?.rows ?? []).filter(
    (r) =>
      (filter === "all" ||
        (filter === "matches" ? r.quantitativeMatch : !r.quantitativeMatch)) &&
      (!market || r.market === market) &&
      `${r.stockName} ${r.stockCode}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const pages = Math.max(1, Math.ceil(shown.length / 50));
  const pageRows = shown.slice((page - 1) * 50, page * 50);
  return {
    baseDate,
    setBaseDate,
    screen,
    loading,
    error,
    filter,
    setFilter,
    market,
    setMarket,
    query,
    setQuery,
    selected,
    tracking,
    trackingDate,
    setTrackingDate,
    trackingError,
    trackingBusy,
    page,
    setPage,
    runs,
    runsError,
    savedLabel,
    load,
    track,
    choose,
    shown,
    pages,
    pageRows,
  };
}
