import type { Holding } from "./types";

import type { Draft } from "./types";
export const isBuyLocked = (h: Holding) =>
  h.userPauseYn === "Y" ||
  h.buyStatus === "STOPPED" ||
  h.buyStatus === "PAUSED";
export const getChangedHoldings = (
  rows: Holding[],
  drafts: Record<number, Draft>,
  isDomestic: boolean,
) =>
  rows.filter(
    (h) =>
      !isBuyLocked(h) &&
      drafts[h.holdingId] &&
      (Number(drafts[h.holdingId].holdingQuantity) !==
        Number(h.holdingQuantity) ||
        (!isDomestic &&
          Number(drafts[h.holdingId].averagePrice) !==
            Number(h.averagePrice)) ||
        (isDomestic &&
          (Number(drafts[h.holdingId].wholeSharePurchaseAmount) !==
            Number(h.wholeSharePurchaseAmount) ||
            Number(drafts[h.holdingId].fractionalSharePurchaseAmount) !==
              Number(h.fractionalSharePurchaseAmount)))),
  );
export function validateHoldingDrafts(
  rows: Holding[],
  drafts: Record<number, Draft>,
  isDomestic: boolean,
): string | null {
  for (const h of rows) {
    const d = drafts[h.holdingId];
    const values = isDomestic
      ? [
          d.holdingQuantity,
          d.wholeSharePurchaseAmount,
          d.fractionalSharePurchaseAmount,
        ]
      : [d.holdingQuantity, d.averagePrice];
    if (values.some((v) => !Number.isFinite(Number(v)) || Number(v) < 0))
      return isDomestic
        ? "보유수량과 정수주·소수점주 매입금액은 0 이상의 숫자로 입력하세요."
        : "보유수량과 평단가는 0 이상의 숫자로 입력하세요.";
  }
  return null;
}
export const buildHoldingUpdates = (
  rows: Holding[],
  drafts: Record<number, Draft>,
  isDomestic: boolean,
) =>
  rows.map((h) => ({
    holdingId: h.holdingId,
    values: {
      holdingQuantity: Number(drafts[h.holdingId].holdingQuantity),
      averagePrice: Number(drafts[h.holdingId].averagePrice),
      wholeSharePurchaseAmount: isDomestic
        ? Number(drafts[h.holdingId].wholeSharePurchaseAmount)
        : null,
      fractionalSharePurchaseAmount: isDomestic
        ? Number(drafts[h.holdingId].fractionalSharePurchaseAmount)
        : null,
    },
  }));
