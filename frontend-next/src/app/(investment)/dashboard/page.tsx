import { loadDashboard } from "../../../features/dashboard/server/queries";
import type { Metadata } from "next";
import DashboardView from "../../../features/dashboard/DashboardView";

export const metadata: Metadata = { title: "대시보드 | FINBRIEF" };

export const dynamic = "force-dynamic";

export default async function Page() {
  const initialData = await loadDashboard();
  return <DashboardView initialData={initialData} />;
}
