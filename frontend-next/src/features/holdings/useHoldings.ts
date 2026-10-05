"use client";
import { useEffect, useState } from "react";
import type { Account } from "../../shared/types";
import type { AdminAccount, AdminStock, Holding } from "./types";

import { savedAccountTab, useAccountTypes } from "../../shared/account-context";
import { amount } from "../../shared/format";
import { getHoldingAccounts, getHoldingStocks, getHoldings } from "./api";
import { useHoldingCreate } from "./useHoldingCreate";
import { useHoldingEditor } from "./useHoldingEditor";
export function useHoldings() {
  const { accountTypes, accountLabel } = useAccountTypes();
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [selected, setSelected] = useState<Account["accountType"]>("DOMESTIC");
  const [sort, setSort] = useState("profitAsc");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [adminAccounts, setAdminAccounts] = useState<AdminAccount[]>([]);
  const [adminStocks, setAdminStocks] = useState<AdminStock[]>([]);
  useEffect(() => {
    setSelected(savedAccountTab());
  }, []);
  useEffect(() => {
    Promise.all([getHoldingAccounts(), getHoldingStocks()])
      .then(([a, s]) => {
        setAdminAccounts(a);
        setAdminStocks(s);
      })
      .catch(() => {});
  }, []);
  useEffect(() => {
    let alive = true;
    setLoading(true);
    getHoldings()
      .then((rows) => {
        if (alive)
          setHoldings(
            rows
              .filter((h) => h.useYn !== "N")
              .map((h) => ({
                ...h,
                accountName: accountLabel(h.accountType),
                currentWeight: h.currentWeight ?? null,
                weightStatus: h.weightStatus ?? null,
              })),
          );
      })
      .catch((e) => {
        if (alive)
          setError(
            e instanceof Error ? e.message : "보유종목을 불러오지 못했습니다.",
          );
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, []);
  const isOverseas = selected === "OVERSEAS";
  const isDomestic = selected === "DOMESTIC";
  const isWholeWonAccount = selected === "ISA" || selected === "PENSION";
  const holdingEvaluation = (h: Holding) =>
    isOverseas
      ? Number(h.currentPrice || 0) * Number(h.holdingQuantity || 0)
      : Number(h.evaluationAmount || 0);
  const formatMoney = (n: number) => amount(n, isOverseas);
  const formatHoldingQuantity = (n: number) =>
    n.toLocaleString(
      "ko-KR",
      isDomestic
        ? { minimumFractionDigits: 6, maximumFractionDigits: 6 }
        : undefined,
    );
  const rows = holdings
    .filter((h) => h.accountType === selected)
    .sort((a, b) =>
      sort === "profitAsc"
        ? Number(a.profitLossRate) - Number(b.profitLossRate)
        : sort === "profitDesc"
          ? Number(b.profitLossRate) - Number(a.profitLossRate)
          : holdingEvaluation(b) - holdingEvaluation(a),
    );
  const evaluation = rows.reduce((sum, h) => sum + holdingEvaluation(h), 0);
  const cost = rows.reduce(
    (sum, h) =>
      sum + Number(h.averagePrice || 0) * Number(h.holdingQuantity || 0),
    0,
  );
  const profit = evaluation - cost;
  const {
    editMode,
    drafts,
    saving,
    saveError,
    isBuyLocked,
    rowClass,
    beginEdit,
    cancelEdit,
    selectAccount,
    updateDraft,
    changed,
    saveAccount,
  } = useHoldingEditor({ rows, isDomestic, setHoldings, setSelected });
  const {
    createOpen,
    setCreateOpen,
    createSaving,
    createError,
    newHolding,
    setNewHolding,
    openCreate,
    saveCreate,
  } = useHoldingCreate({ adminAccounts, selected, accountLabel, setHoldings });
  return {
    accountTypes,
    accountLabel,
    selected,
    sort,
    setSort,
    loading,
    error,
    editMode,
    drafts,
    saving,
    saveError,
    adminAccounts,
    adminStocks,
    createOpen,
    setCreateOpen,
    createSaving,
    createError,
    newHolding,
    setNewHolding,
    isOverseas,
    isDomestic,
    isWholeWonAccount,
    holdingEvaluation,
    formatMoney,
    formatHoldingQuantity,
    rows,
    evaluation,
    profit,
    isBuyLocked,
    rowClass,
    beginEdit,
    cancelEdit,
    selectAccount,
    updateDraft,
    changed,
    openCreate,
    saveCreate,
    saveAccount,
  };
}
