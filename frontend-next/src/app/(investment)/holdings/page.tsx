import type { Metadata } from "next";
import HoldingsView from "../../../features/holdings/HoldingsView";

export const metadata: Metadata = { title: "보유종목 | FINBRIEF" };

export default function Page() {
  return <HoldingsView />;
}
