"use client";
import { useRouter } from "next/navigation";
import type { Page } from "../../lib/pages";
import type { DashboardData } from "./types";

import { useAccountTypes } from "../../shared/account-context";
import { buildDashboardModel } from "./model";
export function useDashboard(initialData: DashboardData | null) {
  const router = useRouter();
  const go = (page: Page) => router.push(`/${page}`);
  const { accountLabel, dashboardLabel } = useAccountTypes();
  const dashboard = initialData,
    dashboardLoading = false,
    assetLoading = false,
    dashboardError = "",
    assetError = "";
  const {
    assetAccounts,
    total,
    totalCash,
    totalRate,
    latestAssetDate,
    cashRate,
    rateText,
  } = buildDashboardModel(dashboard);
  return {
    go,
    accountLabel,
    dashboardLabel,
    assetAccounts,
    assetLoading,
    assetError,
    dashboard,
    dashboardLoading,
    dashboardError,
    total,
    totalCash,
    totalRate,
    latestAssetDate,
    cashRate,
    rateText,
  };
}
