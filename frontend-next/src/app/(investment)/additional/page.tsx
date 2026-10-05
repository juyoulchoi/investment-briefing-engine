import { loadAdditional } from "../../../features/additional/server/queries";
import type { Metadata } from "next";
import AdditionalView from "../../../features/additional/AdditionalView";

export const metadata: Metadata = { title: "추가매수 | FINBRIEF" };

export const dynamic = "force-dynamic";

export default async function Page() {
  const initialData = await loadAdditional();
  return <AdditionalView initialData={initialData} />;
}
