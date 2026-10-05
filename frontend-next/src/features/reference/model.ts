import type { Kind, Row } from "./types";
export const meta: Record<
  Kind,
  {
    title: string;
    id: string;
    columns: [string, string][];
    fields: {
      key: string;
      label: string;
      help?: string;
      required?: boolean;
      type?: string;
      options?: [string, string][];
      wide?: boolean;
    }[];
  }
> = {
  indices: {
    title: "기준지수",
    id: "indexCode",
    columns: [
      ["indexCode", "지수 코드"],
      ["indexName", "지수명"],
      ["indexType", "유형"],
      ["marketCode", "시장"],
      ["dataSourceCode", "데이터 출처"],
      ["defaultYn", "기본"],
      ["useYn", "사용"],
    ],
    fields: [
      { key: "indexCode", label: "지수 코드", required: true },
      { key: "indexName", label: "지수명", required: true },
      { key: "indexEnglishName", label: "영문 지수명", wide: true },
      {
        key: "indexType",
        label: "지수 유형",
        required: true,
        type: "select",
        options: [
          ["MARKET", "시장"],
          ["SECTOR", "섹터"],
          ["ETF_PROXY", "ETF 대용"],
          ["BOND", "채권"],
          ["COMMODITY", "원자재"],
        ],
      },
      { key: "marketCode", label: "시장 코드" },
      { key: "countryCode", label: "국가 코드", required: true },
      { key: "currencyCode", label: "통화 코드", required: true },
      {
        key: "dataSourceCode",
        label: "데이터 출처",
        required: true,
        type: "select",
        options: [
          ["KRX", "KRX"],
          ["YAHOO", "Yahoo"],
          ["FRED", "FRED"],
          ["MANUAL", "수동"],
        ],
      },
      { key: "sourceSymbol", label: "수집 심볼" },
      {
        key: "defaultYn",
        label: "기본 지수",
        type: "select",
        options: [
          ["N", "아니오"],
          ["Y", "예"],
        ],
      },
      {
        key: "useYn",
        label: "사용 여부",
        type: "select",
        options: [
          ["Y", "사용"],
          ["N", "미사용"],
        ],
      },
    ],
  },
  accounts: {
    title: "계좌",
    id: "accountId",
    columns: [
      ["accountType", "계좌 유형"],
      ["cashAmount", "예수금"],
      ["reservedCashAmount", "대기 현금"],
      ["targetCashWeight", "목표 현금 비중"],
    ],
    fields: [
      {
        key: "accountType",
        label: "계좌 유형",
        required: true,
        type: "select",
        options: [
          ["DOMESTIC", "국내"],
          ["OVERSEAS", "해외"],
          ["ISA", "ISA"],
          ["PENSION", "연금"],
        ],
      },
      {
        key: "cashAmount",
        label: "예수금",
        help: "현재 계좌에서 사용할 수 있는 증권 계좌의 예수금입니다.",
        required: true,
        type: "number",
      },
      {
        key: "reservedCashAmount",
        label: "대기 현금",
        help: "예수금에서 추가 매수에 사용할 현금이며, 적립과 사용 내역에 따라 자동 관리됩니다.",
        type: "readonly",
      },
      {
        key: "targetCashWeight",
        label: "목표 현금 비중 (%)",
        help: "전체 평가 금액에서 현금으로 유지할 목표 비중입니다.",
        type: "number",
      },
      {
        key: "displaySequence",
        label: "표시 순번",
        required: true,
        type: "number",
      },
    ],
  },
  stocks: {
    title: "종목",
    id: "stockId",
    columns: [
      ["stockCode", "종목 코드"],
      ["stockName", "종목명"],
      ["marketCode", "시장"],
      ["assetType", "자산 유형"],
      ["stockGrade", "등급"],
      ["baseIndexCode", "기준지수"],
      ["useYn", "사용"],
    ],
    fields: [
      { key: "stockCode", label: "종목 코드", required: true },
      { key: "stockName", label: "종목명", required: true },
      { key: "stockEnglishName", label: "영문 종목명", wide: true },
      { key: "marketCode", label: "시장 코드", required: true },
      { key: "countryCode", label: "국가 코드", required: true },
      { key: "currencyCode", label: "통화 코드", required: true },
      {
        key: "assetType",
        label: "자산 유형",
        required: true,
        type: "select",
        options: [
          ["STOCK", "주식"],
          ["ETF", "ETF"],
          ["BOND_ETF", "채권 ETF"],
          ["COMMODITY_ETF", "원자재 ETF"],
          ["CASH_EQUIVALENT", "현금성"],
        ],
      },
      {
        key: "stockGrade",
        label: "종목 등급",
        required: true,
        type: "select",
        options: [
          ["CORE", "핵심"],
          ["SATELLITE", "위성"],
          ["THEME", "테마"],
        ],
      },
      { key: "baseIndexCode", label: "기준지수", type: "index" },
      { key: "sectorCode", label: "섹터 코드" },
      { key: "sectorName", label: "섹터명" },
      { key: "industryCode", label: "산업 코드" },
      { key: "industryName", label: "산업명" },
      {
        key: "regularBuyYn",
        label: "정기매수",
        type: "select",
        options: [
          ["Y", "허용"],
          ["N", "미허용"],
        ],
      },
      {
        key: "additionalBuyYn",
        label: "추가매수",
        type: "select",
        options: [
          ["Y", "허용"],
          ["N", "미허용"],
        ],
      },
      {
        key: "rebuyYn",
        label: "재매수",
        type: "select",
        options: [
          ["Y", "허용"],
          ["N", "미허용"],
        ],
      },
      {
        key: "useYn",
        label: "사용 여부",
        type: "select",
        options: [
          ["Y", "사용"],
          ["N", "미사용"],
        ],
      },
    ],
  },
};
export const defaults: Record<Kind, Row> = {
  indices: {
    indexType: "MARKET",
    countryCode: "KR",
    currencyCode: "KRW",
    dataSourceCode: "KRX",
    defaultYn: "N",
  },
  accounts: {
    accountType: "DOMESTIC",
    cashAmount: 0,
    reservedCashAmount: 0,
    targetCashWeight: 20,
    displaySequence: 0,
    useYn: "Y",
  },
  stocks: {
    countryCode: "KR",
    currencyCode: "KRW",
    assetType: "STOCK",
    stockGrade: "CORE",
    regularBuyYn: "Y",
    additionalBuyYn: "Y",
    rebuyYn: "Y",
    useYn: "Y",
  },
};
export const formatAmountInput = (value: unknown) => {
  if (value === null || value === undefined || value === "") return "";
  const [integer, decimal] = String(value).replaceAll(",", "").split(".");
  const formatted = Number(integer || 0).toLocaleString("ko-KR");
  return decimal === undefined ? formatted : `${formatted}.${decimal}`;
};
