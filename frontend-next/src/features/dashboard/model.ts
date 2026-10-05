import type { DashboardData } from "./types";

import type { AssetAccount } from "./types";
export function buildDashboardModel(dashboard: DashboardData | null) {
  const assetAccounts: AssetAccount[] = (dashboard?.accountSummaries ?? []).map(
    (a) => ({
      type: a.accountType,
      value: Number(a.totalAsset || 0),
      evaluation: Number(a.evaluationAmount || 0),
      cost: Number(a.costAmount || 0),
      cash: Number(a.cashAmount || 0),
      displayValue: Number(a.displayTotalAsset || 0),
      displayEvaluation: Number(a.displayEvaluationAmount || 0),
      displayCost: Number(a.displayCostAmount || 0),
      displayCash: Number(a.displayCashAmount || 0),
      holdingCount: Number(a.holdingCount || 0),
      priceBaseDate: a.priceBaseDate,
    }),
  );
  const total = assetAccounts.reduce((sum, a) => sum + a.value, 0),
    totalEvaluation = assetAccounts.reduce((sum, a) => sum + a.evaluation, 0),
    totalCost = assetAccounts.reduce((sum, a) => sum + a.cost, 0),
    totalCash = assetAccounts.reduce((sum, a) => sum + a.cash, 0),
    totalRate =
      totalCost > 0 ? ((totalEvaluation - totalCost) / totalCost) * 100 : 0,
    latestAssetDate = assetAccounts
      .map((a) => a.priceBaseDate)
      .filter(Boolean)
      .sort()
      .at(-1),
    cashRate = total > 0 ? (totalCash / total) * 100 : 0;
  const rateText = (a: AssetAccount) => {
    const evaluation =
        a.type === "OVERSEAS" ? a.displayEvaluation : a.evaluation,
      cost = a.type === "OVERSEAS" ? a.displayCost : a.cost;
    return a.holdingCount === 0
      ? "대기"
      : `${evaluation - cost >= 0 ? "+" : ""}${(cost > 0 ? ((evaluation - cost) / cost) * 100 : 0).toFixed(1)}%`;
  };
  return {
    assetAccounts,
    total,
    totalCash,
    totalRate,
    latestAssetDate,
    cashRate,
    rateText,
  };
}
