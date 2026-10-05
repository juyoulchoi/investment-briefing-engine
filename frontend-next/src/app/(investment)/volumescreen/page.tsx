import { todayInSeoul, daysBefore, yearsBefore } from "../../../shared/date";
import type { Metadata } from "next";
import VolumeScreenView from "../../../features/volumescreen/VolumeScreenView";

export const metadata: Metadata = { title: "거래량·횡보 검색 | FINBRIEF" };

export const dynamic = "force-dynamic";
export default function Page() {
  const to = todayInSeoul();
  return <VolumeScreenView initialDate={to} />;
}
