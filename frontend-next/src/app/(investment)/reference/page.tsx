import type { Metadata } from "next";
import ReferenceView from "../../../features/reference/ReferenceView";

export const metadata: Metadata = { title: "기준정보 관리 | FINBRIEF" };

export default function Page() {
  return <ReferenceView />;
}
