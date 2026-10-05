import { requestFlexible } from "../../shared/http";
import type { Candidate, Screen, SearchRun, Tracking } from "./types";
const endpoint = "/api/v1/krx/screens/volume-consolidation";
export const getScreen = (
  date: string,
  saved: SearchRun | undefined,
  signal: AbortSignal,
) =>
  requestFlexible<Screen>(
    saved
      ? `${endpoint}/runs/${saved.runId}/result`
      : `${endpoint}${date ? `?baseDate=${date}` : ""}`,
    { signal },
  );
export const getSearchRuns = (signal: AbortSignal) =>
  requestFlexible<SearchRun[]>(`${endpoint}/runs`, { signal });
export const getTracking = (
  selected: Candidate,
  asOf: string,
  signal: AbortSignal,
) => {
  const params = new URLSearchParams({
    market: selected.market,
    stockCode: selected.stockCode,
    selectionDate: selected.baseDate,
  });
  if (asOf) params.set("asOf", asOf);
  return requestFlexible<Tracking>(`${endpoint}/tracking?${params}`, {
    signal,
  });
};
