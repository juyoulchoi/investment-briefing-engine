import type { Metadata } from "next";
import OperationsView from "../../../features/operations/OperationsView";

export const metadata: Metadata = { title: "투자 설정 관리 | FINBRIEF" };

export default function Page() {
  return <OperationsView />;
}
