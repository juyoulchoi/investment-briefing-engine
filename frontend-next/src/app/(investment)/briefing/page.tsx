import { loadBriefing } from "../../../features/briefing/server/queries";
import type { Metadata } from "next";
import BriefingView from "../../../features/briefing/BriefingView";

export const metadata: Metadata = { title: "투자 브리핑 | FINBRIEF" };

export const dynamic = "force-dynamic";

export default async function Page() {
  const initialData = await loadBriefing();
  return <BriefingView initialData={initialData} />;
}
