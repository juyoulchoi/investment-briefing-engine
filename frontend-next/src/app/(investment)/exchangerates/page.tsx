import { todayInSeoul, daysBefore, yearsBefore } from "../../../shared/date";
import type { Metadata } from "next";
import ExchangeRatesView from "../../../features/exchangerates/ExchangeRatesView";

export const metadata: Metadata = { title: "환율 차트 | FINBRIEF" };

export const dynamic = "force-dynamic";
export default function Page() {
  const to = todayInSeoul();
  return <ExchangeRatesView initialRange={{ from: yearsBefore(to, 5), to }} />;
}
