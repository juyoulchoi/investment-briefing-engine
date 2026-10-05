"use client";
import type { Dispatch, SetStateAction } from "react";
import { useState } from "react";
import type { Account } from "../../shared/types";
import type { Holding } from "./types";

import { accountTabStorageKey } from "../../shared/account-context";
import { updateAccountHoldings } from "./api";
import {
  buildHoldingUpdates,
  getChangedHoldings,
  isBuyLocked,
  validateHoldingDrafts,
} from "./model";
import type { Draft } from "./types";
export function useHoldingEditor({
  rows,
  isDomestic,
  setHoldings,
  setSelected,
}: {
  rows: Holding[];
  isDomestic: boolean;
  setHoldings: Dispatch<SetStateAction<Holding[]>>;
  setSelected: Dispatch<SetStateAction<Account["accountType"]>>;
}) {
  const [editMode, setEditMode] = useState(false);
  const [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  const rowClass = (h: Holding) =>
    [
      h.userPauseYn === "Y"
        ? "holding-user-paused-row"
        : h.buyStatus === "STOPPED"
          ? "holding-buy-stopped-row"
          : h.buyStatus === "PAUSED"
            ? "holding-buy-paused-row"
            : "",
      editMode && changed.some((x) => x.holdingId === h.holdingId)
        ? "changed-row"
        : "",
    ]
      .filter(Boolean)
      .join(" ");
  const beginEdit = () => {
    setDrafts(
      Object.fromEntries(
        rows.map((h) => [
          h.holdingId,
          {
            holdingQuantity: String(h.holdingQuantity),
            averagePrice: String(h.averagePrice),
            wholeSharePurchaseAmount: String(h.wholeSharePurchaseAmount ?? 0),
            fractionalSharePurchaseAmount: String(
              h.fractionalSharePurchaseAmount ?? 0,
            ),
          },
        ]),
      ),
    );
    setSaveError("");
    setEditMode(true);
  };
  const cancelEdit = () => {
    if (!saving) {
      setEditMode(false);
      setDrafts({});
      setSaveError("");
    }
  };
  const selectAccount = (type: Account["accountType"]) => {
    cancelEdit();
    localStorage.setItem(accountTabStorageKey, type);
    setSelected(type);
  };
  const updateDraft = (id: number, field: keyof Draft, value: string) =>
    setDrafts((current) => ({
      ...current,
      [id]: { ...current[id], [field]: value },
    }));
  const changed = getChangedHoldings(rows, drafts, isDomestic);
  const saveAccount = async () => {
    if (!changed.length) {
      cancelEdit();
      return;
    }
    const validation = validateHoldingDrafts(changed, drafts, isDomestic);
    if (validation) {
      setSaveError(validation);
      return;
    }
    setSaving(true);
    setSaveError("");
    try {
      const accountId = rows[0]?.accountId;
      if (!accountId) throw new Error("계좌를 찾을 수 없습니다.");
      const saved = await updateAccountHoldings(
        accountId,
        buildHoldingUpdates(changed, drafts, isDomestic),
      );
      const updated = new Map(saved.map((h) => [h.holdingId, h]));
      setHoldings((current) =>
        current.map((h) =>
          updated.has(h.holdingId)
            ? {
                ...h,
                ...updated.get(h.holdingId),
                accountType: h.accountType,
                accountName: h.accountName,
              }
            : h,
        ),
      );
      setEditMode(false);
      setDrafts({});
    } catch (err) {
      setSaveError(
        err instanceof Error ? err.message : "일괄 저장하지 못했습니다.",
      );
    } finally {
      setSaving(false);
    }
  };
  return {
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
  };
}
