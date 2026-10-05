import { requestFlexible } from "../../shared/http";
import type { BondYield, CommonCode } from "./types";
export const getBondYields = (from: string, to: string) =>
  requestFlexible<BondYield[]>(`/api/market-data/bonds?from=${from}&to=${to}`);
export const getBondSeries = () =>
  requestFlexible<CommonCode[]>("/api/v1/common-codes/BOND_YIELD_SERIES");
export const refreshBondYields = () =>
  requestFlexible<{
    from: string;
    to: string;
    savedCount: number;
    latestObservationDate: string | null;
  }>("/api/market-data/bonds/refresh", { method: "POST" });
