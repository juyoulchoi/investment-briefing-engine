"use client";
import { useEffect, useState } from "react";
import { getBriefingDetail, getHistory } from "./api";
import { statusLabel } from "./model";
import type { Detail, Row } from "./types";
export function useHistory() {
  const [period, set] = useState("일일");
  const [rows, setRows] = useState<Row[]>([]),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [detail, setDetail] = useState<Detail | null>(null),
    [detailLoading, setDetailLoading] = useState(false);
  const type =
    period === "일일" ? "DAILY" : period === "주간" ? "WEEKLY" : "MONTHLY";
  useEffect(() => {
    let alive = true;
    setDetail(null);
    setLoading(true);
    setError("");
    getHistory(type)
      .then((data) => {
        if (alive) setRows(data);
      })
      .catch((e) => {
        if (alive)
          setError(
            e instanceof Error
              ? e.message
              : "브리핑 이력을 불러오지 못했습니다.",
          );
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [type]);
  const openDetail = (id: number) => {
    setDetailLoading(true);
    setError("");
    getBriefingDetail(id)
      .then(setDetail)
      .catch((e) =>
        setError(
          e instanceof Error ? e.message : "브리핑 상세를 불러오지 못했습니다.",
        ),
      )
      .finally(() => setDetailLoading(false));
  };

  return {
    period,
    set,
    rows,
    loading,
    error,
    detail,
    setDetail,
    detailLoading,
    openDetail,
    statusLabel,
  };
}
