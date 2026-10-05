import type { Account } from "../../shared/types";
export type Draft = {
  holdingQuantity: string;
  averagePrice: string;
  wholeSharePurchaseAmount: string;
  fractionalSharePurchaseAmount: string;
};
export type AdminAccount = {
  accountId: number;
  accountType: Account["accountType"];
};
export type AdminStock = {
  stockId: number;
  stockCode: string;
  stockName: string;
  marketCode: string;
  useYn: string;
};

export type Holding = {
  holdingId: number;
  accountId: number;
  accountName: string;
  accountType: Account["accountType"];
  stockId: number;
  stockCode: string;
  stockName: string;
  holdingQuantity: number;
  averagePrice: number;
  wholeSharePurchaseAmount: number | null;
  fractionalSharePurchaseAmount: number | null;
  currentPrice: number;
  evaluationAmount: number;
  profitLossRate: number;
  targetWeight: number | null;
  currentWeight: number | null;
  investmentAssetWeight: number | null;
  weightStatus: string | null;
  weightStatusName: string | null;
  holdingStatus: string;
  buyStatus?: "ACTIVE" | "PAUSED" | "STOPPED" | null;
  userPauseYn?: "Y" | "N";
};
