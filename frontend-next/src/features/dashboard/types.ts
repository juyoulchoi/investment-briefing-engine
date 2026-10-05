import type { Account } from "../../shared/types";
export type AssetAccount = {
  type: Account["accountType"];
  value: number;
  evaluation: number;
  cost: number;
  cash: number;
  displayValue: number;
  displayEvaluation: number;
  displayCost: number;
  displayCash: number;
  holdingCount: number;
  priceBaseDate: string | null;
};

export type DashboardData = {
  baseDate: string;
  briefingBaseDate: string | null;
  marketScore: number;
  marketRegime: string;
  sentimentScore: number;
  sentimentPhase: string;
  riskGrade: string;
  overallSignal: string;
  regularBuyTotal: number;
  additionalBuyTotal: number;
  title: string | null;
  summary: string | null;
  body: string | null;
  accountSummaries: {
    accountType: Account["accountType"];
    totalAsset: number;
    evaluationAmount: number;
    etfEvaluationAmount: number;
    etfAssetAmount: number;
    etfEvaluationRatio: number;
    costAmount: number;
    cashAmount: number;
    holdingCount: number;
    priceBaseDate: string | null;
    currencyCode: string;
    displayTotalAsset: number;
    displayEvaluationAmount: number;
    displayCostAmount: number;
    displayCashAmount: number;
  }[];
  actionSignals: {
    accountType: Account["accountType"];
    stockCode: string;
    stockName: string;
    actionSignal: string;
    recommendedAmount: number | null;
    reason: string;
  }[];
  briefingArticles: {
    itemCode: string;
    summary: string;
    content: string;
    signalCode: string;
  }[];
};
