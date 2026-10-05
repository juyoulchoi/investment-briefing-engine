"use client";
import type { Dispatch, SetStateAction } from "react";
import { useState } from "react";
import type { Account } from "../../shared/types";
import type { Holding } from "./types";

import { createHolding } from "./api";
import type { AdminAccount } from "./types";
export function useHoldingCreate({
  adminAccounts,
  selected,
  accountLabel,
  setHoldings,
}: {
  adminAccounts: AdminAccount[];
  selected: Account["accountType"];
  accountLabel: (type: Account["accountType"]) => string;
  setHoldings: Dispatch<SetStateAction<Holding[]>>;
}) {
  const [createOpen, setCreateOpen] = useState(false);
  const [createSaving, setCreateSaving] = useState(false);
  const [createError, setCreateError] = useState("");
  const [newHolding, setNewHolding] = useState<Record<string, any>>({
    holdingQuantity: 0,
    averagePrice: 0,
    exchangeRate: 1,
    targetWeight: null,
    holdingStatus: "ACTIVE",
    useYn: "Y",
    memo: "",
  });
  const openCreate = () => {
    const account = adminAccounts.find((a) => a.accountType === selected);
    setNewHolding({
      accountId: account?.accountId,
      stockId: "",
      holdingQuantity: 0,
      averagePrice: 0,
      wholeSharePurchaseAmount: selected === "DOMESTIC" ? 0 : null,
      fractionalSharePurchaseAmount: selected === "DOMESTIC" ? 0 : null,
      exchangeRate: 1,
      targetWeight: null,
      holdingStatus: "ACTIVE",
      useYn: "Y",
      memo: "",
    });
    setCreateError("");
    setCreateOpen(true);
  };
  const saveCreate = async () => {
    if (!newHolding.accountId || !newHolding.stockId) {
      setCreateError("계좌와 종목을 선택하세요.");
      return;
    }
    setCreateSaving(true);
    setCreateError("");
    try {
      const r = await createHolding(newHolding);
      setHoldings((current) => [
        ...current,
        {
          holdingId: r.holdingId,
          accountId: r.accountId,
          accountName: accountLabel(r.accountType as Account["accountType"]),
          accountType: r.accountType,
          stockId: r.stockId,
          stockCode: r.stockCode,
          stockName: r.stockName,
          holdingQuantity: Number(r.holdingQuantity || 0),
          averagePrice: Number(r.averagePrice || 0),
          wholeSharePurchaseAmount: r.wholeSharePurchaseAmount ?? null,
          fractionalSharePurchaseAmount:
            r.fractionalSharePurchaseAmount ?? null,
          currentPrice: Number(r.currentPrice || 0),
          evaluationAmount: Number(r.evaluationAmount || 0),
          profitLossRate: Number(r.profitLossRate || 0),
          targetWeight: r.targetWeight,
          currentWeight: r.currentWeight ?? null,
          investmentAssetWeight: r.investmentAssetWeight ?? null,
          weightStatus: r.weightStatus ?? null,
          weightStatusName: r.weightStatusName ?? null,
          holdingStatus: r.holdingStatus,
        },
      ]);
      setCreateOpen(false);
    } catch (e) {
      setCreateError(
        e instanceof Error ? e.message : "보유종목을 등록하지 못했습니다.",
      );
    } finally {
      setCreateSaving(false);
    }
  };
  return {
    createOpen,
    setCreateOpen,
    createSaving,
    createError,
    newHolding,
    setNewHolding,
    openCreate,
    saveCreate,
  };
}
