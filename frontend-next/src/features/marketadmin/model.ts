import type { Field, Kind, Row } from "./types";
export const status: [string, string][] = [
  ["FRESH", "정상"],
  ["STALE", "지연"],
  ["MISSING", "누락"],
  ["PARTIAL", "일부"],
  ["ERROR", "오류"],
];
export const cfg: Record<
  Kind,
  {
    title: string;
    table: string;
    id: string;
    columns: [string, string][];
    fields: Field[];
    defaults: Row;
  }
> = {
  snapshots: {
    title: "일별 시장 스냅샷",
    table: "TB_MKT_SNAP",
    id: "marketSnapshotId",
    columns: [
      ["baseDate", "기준일"],
      ["marketSnapshotCode", "시장 코드"],
      ["marketName", "시장명"],
      ["mainIndexName", "대표지수"],
      ["mainIndexValue", "지수값"],
      ["mainIndexChangeRate", "등락률"],
      ["marketBreadthRate", "시장 확산도"],
      ["dataStatus", "상태"],
    ],
    defaults: {
      baseDate: new Date().toISOString().slice(0, 10),
      dataSourceCode: "INTERNAL",
      dataStatus: "FRESH",
      dataAgeMinutes: 0,
    },
    fields: [
      { key: "baseDate", label: "기준일", type: "date", required: true },
      { key: "marketSnapshotCode", label: "시장 스냅샷 코드", required: true },
      { key: "marketName", label: "시장명", required: true },
      { key: "mainIndexCode", label: "대표 기준지수", type: "index" },
      { key: "mainIndexValue", label: "대표지수 값", type: "number" },
      {
        key: "mainIndexChangeRate",
        label: "대표지수 등락률 (%)",
        type: "number",
      },
      { key: "foreignNetAmount", label: "외국인 순매수금액", type: "number" },
      { key: "exchangeRate", label: "대표 환율", type: "number" },
      { key: "advancingStockCount", label: "상승 종목 수", type: "number" },
      { key: "decliningStockCount", label: "하락 종목 수", type: "number" },
      { key: "marketBreadthRate", label: "시장 확산도", type: "number" },
      { key: "dataSourceCode", label: "산출 출처 코드", required: true },
      {
        key: "dataStatus",
        label: "데이터 상태",
        type: "select",
        required: true,
        options: status,
      },
      { key: "dataAgeMinutes", label: "데이터 경과시간 (분)", type: "number" },
    ],
  },
  sentiments: {
    title: "일별 시장심리 분석",
    table: "TB_MKT_SENT",
    id: "marketSentimentId",
    columns: [
      ["baseDate", "기준일"],
      ["marketSnapshotCode", "시장 코드"],
      ["sentimentScore", "심리 점수"],
      ["sentimentPhase", "심리 단계"],
      ["confidenceRate", "신뢰도"],
      ["structuralDamageYn", "구조적 훼손"],
      ["ruleVersionNumber", "규칙 버전"],
      ["dataStatus", "상태"],
    ],
    defaults: {
      baseDate: new Date().toISOString().slice(0, 10),
      sentimentScore: 50,
      sentimentPhase: "NEUTRAL",
      confidenceRate: 50,
      structuralDamageYn: "N",
      ruleVersionNumber: 1,
      dataStatus: "FRESH",
    },
    fields: [
      { key: "baseDate", label: "기준일", type: "date", required: true },
      { key: "marketSnapshotCode", label: "시장 스냅샷 코드", required: true },
      { key: "newsFearScore", label: "뉴스 공포 점수", type: "number" },
      { key: "aiFatigueScore", label: "AI 투자 피로 점수", type: "number" },
      {
        key: "earningsConfidenceScore",
        label: "기업실적 신뢰도 점수",
        type: "number",
      },
      {
        key: "sentimentScore",
        label: "종합 시장심리 점수",
        type: "number",
        required: true,
      },
      {
        key: "sentimentPhase",
        label: "시장심리 단계",
        type: "select",
        required: true,
        options: [
          ["GREED", "탐욕"],
          ["OPTIMISM", "낙관"],
          ["NEUTRAL", "중립"],
          ["FATIGUE", "피로"],
          ["FEAR", "공포"],
          ["PANIC", "패닉"],
        ],
      },
      {
        key: "confidenceRate",
        label: "판단 신뢰도 (%)",
        type: "number",
        required: true,
      },
      {
        key: "structuralDamageYn",
        label: "구조적 훼손",
        type: "select",
        options: [
          ["N", "아니오"],
          ["Y", "예"],
        ],
      },
      {
        key: "ruleVersionNumber",
        label: "계산규칙 버전",
        type: "number",
        required: true,
      },
      {
        key: "dataStatus",
        label: "입력 데이터 상태",
        type: "select",
        required: true,
        options: status,
      },
    ],
  },
};
export const label: Record<string, string> = {
  FRESH: "정상",
  STALE: "지연",
  MISSING: "누락",
  PARTIAL: "일부",
  ERROR: "오류",
  GREED: "탐욕",
  OPTIMISM: "낙관",
  NEUTRAL: "중립",
  FATIGUE: "피로",
  FEAR: "공포",
  PANIC: "패닉",
  Y: "예",
  N: "아니오",
};
