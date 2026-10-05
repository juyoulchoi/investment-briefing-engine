import { jsonBody, requestEnvelope } from "../../shared/http";
import type { Kind, Row } from "./types";
export const getReferenceRows = (kind: Kind) =>
  requestEnvelope<Row[]>(`/api/v1/admin/reference/${kind}`);
export const saveReference = (kind: Kind, id: unknown, form: Row) =>
  requestEnvelope<Row>(
    `/api/v1/admin/reference/${kind}${id ? `/${id}` : ""}`,
    jsonBody(id ? "PUT" : "POST", form),
  );
