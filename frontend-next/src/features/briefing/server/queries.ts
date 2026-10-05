import "server-only";
import { requestBackend } from "../../../shared/server/backend";
import type { DashboardData } from "../../dashboard/types";

export const loadBriefing = () =>
  requestBackend<DashboardData | null>("/api/v1/dashboard", true);
