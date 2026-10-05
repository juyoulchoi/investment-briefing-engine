import { requestFlexible } from "../../shared/http";
import type { ExchangeRate } from "./types";
export const getExchangeRates = (from: string, to: string) =>
  requestFlexible<ExchangeRate[]>(
    `/api/market-data/exchange-rates?baseCurrency=USD&quoteCurrency=KRW&from=${from}&to=${to}`,
  );
