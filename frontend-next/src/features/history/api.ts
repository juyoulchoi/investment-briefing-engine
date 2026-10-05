import { requestEnvelope } from "../../shared/http";
import type { Detail, Row } from "./types";
export const getHistory = (type: string) =>
  requestEnvelope<Row[]>(
    `/api/v1/briefings/history?type=${encodeURIComponent(type)}`,
  );
export const getBriefingDetail = (id: number) =>
  requestEnvelope<Detail>(`/api/v1/briefings/${id}`);
