import type { Metadata } from "next";
import HistoryView from "../../../features/history/HistoryView";

export const metadata: Metadata = { title: "브리핑 이력 | FINBRIEF" };

export default function Page() {
  return <HistoryView />;
}
