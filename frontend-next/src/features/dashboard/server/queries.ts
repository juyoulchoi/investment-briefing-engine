import "server-only";
import { requestBackend } from "../../../shared/server/backend";
import type { DashboardData } from "../types";

export const loadDashboard = () =>
  requestBackend<DashboardData | null>("/api/v1/dashboard", true);
