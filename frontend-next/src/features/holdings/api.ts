import { jsonBody, requestEnvelope } from "../../shared/http";
import type { AdminAccount, AdminStock, Holding } from "./types";

export const getHoldingAccounts = () =>
  requestEnvelope<AdminAccount[]>("/api/v1/admin/reference/accounts");
export const getHoldingStocks = () =>
  requestEnvelope<AdminStock[]>("/api/v1/admin/reference/stocks");
export const getHoldings = () =>
  requestEnvelope<(Holding & { useYn?: string })[]>(
    "/api/v1/admin/operations/holdings",
  );
export const createHolding = (values: Record<string, unknown>) =>
  requestEnvelope<Holding>(
    "/api/v1/admin/operations/holdings",
    jsonBody("POST", values),
  );
export const updateAccountHoldings = (accountId: number, updates: unknown[]) =>
  requestEnvelope<Holding[]>(
    `/api/v1/accounts/${accountId}/holdings`,
    jsonBody("PATCH", { updates }),
  );
