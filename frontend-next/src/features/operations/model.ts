import type { Field, Kind, Row } from "./types";
export const labels: Record<string, string> = {
  DOMESTIC: "국내",
  OVERSEAS: "해외",
  ISA: "ISA",
  PENSION: "연금",
  DAILY: "매일",
  WEEKLY: "매주",
  MONTHLY: "매월",
  CUSTOM: "사용자 지정",
  AMOUNT: "금액",
  QUANTITY: "수량",
  ACTIVE: "활성",
  PAUSED: "일시정지",
  STOPPED: "중지",
  TODAY: "매수",
  WAIT: "대기",
  CORE: "핵심",
  SATELLITE: "위성",
  THEME: "테마",
  CASH_LIKE: "현금성",
  CLOSED: "종료",
  TRANSFER_PENDING: "이관 대기",
  OVERWEIGHT: "비중초과",
  UNDERWEIGHT: "비중부족",
  NORMAL: "적정",
  Y: "예",
  N: "아니오",
};
export const config: Record<
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
  holdings: {
    title: "보유종목",
    table: "TB_HOLD",
    id: "holdingId",
    columns: [
      ["accountType", "계좌"],
      ["stockCode", "종목 코드"],
      ["stockName", "종목명"],
      ["holdingQuantity", "보유수량"],
      ["averagePrice", "평단가"],
      ["wholeSharePurchaseAmount", "정수주 매입금액"],
      ["fractionalSharePurchaseAmount", "소수점주 매입금액"],
      ["currentPrice", "현재가"],
      ["targetWeight", "목표비중"],
      ["currentWeight", "계좌 전체 비중"],
      ["weightStatusName", "비중상태"],
      ["holdingStatus", "상태"],
    ],
    defaults: {
      holdingQuantity: 0,
      averagePrice: 0,
      exchangeRate: 1,
      holdingStatus: "ACTIVE",
      useYn: "Y",
    },
    fields: [
      { key: "accountId", label: "계좌", type: "account", required: true },
      { key: "stockId", label: "종목", type: "stock", required: true },
      {
        key: "holdingQuantity",
        label: "보유수량",
        type: "number",
        required: true,
      },
      {
        key: "averagePrice",
        label: "평균 매입가",
        type: "number",
        required: true,
      },
      {
        key: "wholeSharePurchaseAmount",
        label: "정수주 매입금액",
        help: "국내주식 계좌 전용",
        type: "number",
        required: true,
        domesticOnly: true,
      },
      {
        key: "fractionalSharePurchaseAmount",
        label: "소수점주 매입금액",
        help: "국내주식 계좌 전용",
        type: "number",
        required: true,
        domesticOnly: true,
      },
      {
        key: "exchangeRate",
        label: "적용 환율",
        type: "number",
        required: true,
      },
      { key: "targetWeight", label: "목표 비중 (%)", type: "number" },
      {
        key: "holdingStatus",
        label: "보유 상태",
        type: "select",
        required: true,
        options: [
          ["ACTIVE", "활성"],
          ["CLOSED", "종료"],
          ["TRANSFER_PENDING", "이관 대기"],
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
      { key: "memo", label: "메모", wide: true },
    ],
  },
  "regular-buys": {
    title: "정기매수 설정",
    table: "TB_REG_BUY",
    id: "regularBuyKey",
    columns: [
      ["accountType", "계좌"],
      ["stockCode", "종목코드"],
      ["stockName", "종목명"],
      ["stockGrade", "종목등급"],
      ["targetWeight", "목표비중"],
      ["currentWeight", "계좌 전체 비중"],
      ["buyBasis", "매수단위"],
      ["appliedSchedule", "현재 적용주기"],
      ["appliedValue", "현재 적용금액"],
      ["buyStatus", "매수상태"],
      ["userPauseYn", "사용자 일시정지"],
      ["pauseReason", "정지 사유"],
    ],
    defaults: {
      buyCycle: "MONTHLY",
      appliedCycle: "MONTHLY",
      buyBasis: "AMOUNT",
      minimumBuyAmount: 0,
      buyStatus: "ACTIVE",
      userPauseYn: "N",
      autoCalculateYn: "Y",
    },
    fields: [
      { key: "accountId", label: "계좌", type: "account", required: true },
      { key: "stockId", label: "종목", type: "stock", required: true },
      {
        key: "investmentGrade",
        label: "투자등급",
        help: "공통코드 INVESTMENT_GRADE",
        type: "investmentGrade",
        required: true,
      },
      {
        key: "buyBasis",
        label: "매수 기준",
        type: "select",
        required: true,
        options: [
          ["AMOUNT", "금액 기준"],
          ["QUANTITY", "수량 기준"],
        ],
      },
      {
        key: "minimumBuyAmount",
        label: "기준금액",
        type: "number",
        required: true,
      },
      {
        key: "baseBuyQuantity",
        label: "기준 수량",
        type: "number",
      },
      {
        key: "buyCycle",
        label: "기준 매수주기",
        type: "select",
        required: true,
        options: [
          ["DAILY", "매일"],
          ["WEEKLY", "매주"],
          ["MONTHLY", "매월"],
        ],
      },
      {
        key: "buyDayCode",
        label: "기준 매수요일",
        type: "weekdays",
      },
      {
        key: "buyDayNumbers",
        label: "기준 매수일",
        help: "복수 선택 가능",
        type: "monthdays",
      },
      { key: "appliedAmount", label: "현재 적용금액", type: "number" },
      {
        key: "buyQuantity",
        label: "현재 매수 수량",
        type: "number",
      },
      {
        key: "appliedCycle",
        label: "현재 적용주기",
        type: "select",
        required: true,
        options: [
          ["DAILY", "매일"],
          ["WEEKLY", "매주"],
          ["MONTHLY", "매월"],
        ],
      },
      {
        key: "appliedWeekDays",
        label: "현재 매수요일",
        type: "weekdays",
      },
      {
        key: "appliedMonthDays",
        label: "현재 매수일",
        help: "복수 선택 가능",
        type: "monthdays",
      },
      {
        key: "buyStatus",
        label: "매수 상태",
        help: "정기매수의 현재 실행 상태",
        type: "select",
        required: true,
        options: [
          ["ACTIVE", "활성"],
          ["STOPPED", "중지"],
        ],
      },
      {
        key: "userPauseYn",
        label: "사용자 일시정지",
        help: "직접 잠시 멈출 때 예",
        type: "select",
        options: [
          ["N", "아니오"],
          ["Y", "예"],
        ],
      },
      {
        key: "pauseReason",
        label: "정지 사유",
        type: "select",
        options: [["", "선택 안 함"]],
      },
      { key: "memo", label: "비고", wide: true },
    ],
  },
  "cash-reserves": {
    title: "추가매수 대기현금",
    table: "TB_CASH_RSV",
    id: "cashReserveId",
    columns: [
      ["accountType", "계좌"],
      ["reserveAmount", "현재 대기금액"],
      ["accumulatedAmount", "누적 적립액"],
      ["usedAmount", "누적 사용액"],
      ["availableAmount", "가용 금액"],
    ],
    defaults: { reserveAmount: 0, accumulatedAmount: 0, usedAmount: 0 },
    fields: [
      { key: "accountId", label: "계좌", type: "account", required: true },
      {
        key: "reserveAmount",
        label: "현재 대기금액",
        type: "number",
        required: true,
      },
      {
        key: "accumulatedAmount",
        label: "누적 적립액",
        type: "number",
        required: true,
      },
      {
        key: "usedAmount",
        label: "누적 사용액",
        type: "number",
        required: true,
      },
      { key: "lastTransactionDate", label: "최종 거래일", type: "date" },
    ],
  },
};

export const fixedAmountRegularBuyBase = {
  buyBasis: "AMOUNT",
  minimumBuyAmount: 10000,
  buyCycle: "WEEKLY",
  buyDayCode: "TUE",
  buyDayNumber: null,
  buyDayNumbers: null,
  baseBuyQuantity: 1,
};

export const fixedQuantityRegularBuyBase = {
  buyBasis: "QUANTITY",
  minimumBuyAmount: 10000,
  buyCycle: "WEEKLY",
  buyDayCode: "TUE",
  buyDayNumber: null,
  buyDayNumbers: null,
  baseBuyQuantity: 1,
};

export const normalizedRegularBuyForm = (kind: Kind, value: Row) => {
  if (kind !== "regular-buys") return value;
  if (value.buyStatus === "STOPPED" || value.userPauseYn === "Y")
    return {
      ...value,
      appliedWeekDays: null,
      appliedMonthDays: null,
    };
  if (value.appliedCycle === "WEEKLY")
    return { ...value, appliedMonthDays: null };
  if (value.appliedCycle === "MONTHLY")
    return { ...value, appliedWeekDays: null };
  return {
    ...value,
    appliedWeekDays: null,
    appliedMonthDays: null,
  };
};

export const schedule = (cycle: any, weekDays: any, monthDays: any) => {
  if (cycle === "PAUSED" || cycle === "STOPPED") return "-";
  const name = labels[String(cycle)] || String(cycle || "-");
  if (cycle === "WEEKLY" && weekDays)
    return `${name} ${String(weekDays)
      .split(",")
      .map(
        (v) =>
          (({ MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금" }) as Row)[
            v
          ] || v,
      )
      .join("·")}`;
  if (cycle === "MONTHLY" && monthDays)
    return `${name} ${String(monthDays)
      .split(",")
      .map((v) => `${v}일`)
      .join("·")}`;
  return name;
};
