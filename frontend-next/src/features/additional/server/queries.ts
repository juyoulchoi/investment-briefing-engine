import "server-only";
import { requestBackend } from "../../../shared/server/backend";
import type { Result } from "../types";
export const loadAdditional = () =>
  requestBackend<Result>("/api/investment/buy-plans/additional/latest", false);
