import { jsonBody, requestEnvelope, requestJson } from "../../shared/http";
import type { Kind, Row } from "./types";
export const getOperationRows = (kind: string) =>
  requestEnvelope<Row[]>(`/api/v1/admin/operations/${kind}`);
export const getReferenceRows = (kind: string) =>
  requestEnvelope<Row[]>(`/api/v1/admin/reference/${kind}`);
export const getPauseReasons = () =>
  requestJson<Row[]>("/api/v1/common-codes/REG_BUY_PAUSE_REASON");
export const saveOperation = (kind: Kind, id: unknown, form: Row) =>
  requestEnvelope<Row>(
    `/api/v1/admin/operations/${kind}${id ? `/${id}` : ""}`,
    jsonBody(id ? "PUT" : "POST", form),
  );
