import type { Metadata } from "next";
import MarketAnalysisView from "../../../features/marketadmin/MarketAnalysisView";

export const metadata: Metadata = { title: "시장 분석 관리 | FINBRIEF" };

export default function Page() {
  return <MarketAnalysisView />;
}
