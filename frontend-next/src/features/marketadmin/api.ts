import { jsonBody, requestEnvelope } from "../../shared/http";
import type { Kind, Row } from "./types";
export const getMarketRows = (kind: Kind) =>
  requestEnvelope<Row[]>(`/api/v1/admin/market-analysis/${kind}`);
export const getMarketIndices = () =>
  requestEnvelope<Row[]>("/api/v1/admin/reference/indices");
export const saveMarketRow = (kind: Kind, id: unknown, form: Row) =>
  requestEnvelope<Row>(
    `/api/v1/admin/market-analysis/${kind}${id ? `/${id}` : ""}`,
    jsonBody(id ? "PUT" : "POST", form),
  );
