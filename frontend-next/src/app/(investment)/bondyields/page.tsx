import { todayInSeoul, daysBefore, yearsBefore } from "../../../shared/date";
import type { Metadata } from "next";
import BondYieldsView from "../../../features/bondyields/BondYieldsView";

export const metadata: Metadata = { title: "FRED 채권금리 | FINBRIEF" };

export const dynamic = "force-dynamic";
export default function Page() {
  const to = todayInSeoul();
  return <BondYieldsView initialRange={{ from: daysBefore(to, 90), to }} />;
}
