export type Row = {
  briefingId: number;
  baseDate: string;
  briefingType: string;
  briefingTypeLabel: string;
  title: string;
  summary: string | null;
  status: string;
  statusLabel: string;
  publishedYn: string;
  confidenceRate: number;
  marketScore: number | null;
  marketRegime: string | null;
  marketRegimeLabel: string | null;
};
export type Detail = {
  briefingId: number;
  baseDate: string;
  briefingType: string;
  briefingTypeLabel: string;
  title: string;
  summary: string | null;
  body: string | null;
  status: string;
  statusLabel: string;
  publishedYn: string;
  confidenceRate: number;
  items: {
    itemCode: string;
    itemTitle: string;
    summary: string;
    content: string;
    signalCode: string | null;
    signalLabel: string | null;
  }[];
};
