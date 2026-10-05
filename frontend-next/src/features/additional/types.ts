import type { Account } from "../../shared/types";
export type Candidate = {
  additionalBuyId: number;
  accountType: Account["accountType"];
  stockCode: string;
  stockName: string;
  eligibleYn: string;
  priority: number | null;
  score: number;
  recommendedAmount: number;
  reason: string;
  executedYn: string;
};
export type Result = {
  baseDate: string | null;
  reserveAmount: number;
  recommendedTotal: number;
  usageRate: number;
  accounts: {
    accountId: number;
    accountType: Account["accountType"];
    reserveAmount: number;
    recommendedTotal: number;
    usageRate: number;
  }[];
  candidates: Candidate[];
};
