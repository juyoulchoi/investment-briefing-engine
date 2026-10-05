export const runStatus: Record<string, string> = {
  RUNNING: "검색 중",
  CALCULATED: "수치 계산 완료 · 검증 필요",
  PARTIAL: "잠정 계산 · 데이터 보완 필요",
  FAILED: "검색 실패",
  SKIPPED: "검색 생략",
};
export const todayInSeoul = () =>
  new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Seoul" }).format(
    new Date(),
  );
export const num = (n: number | null | undefined, digits = 2) =>
  n == null
    ? "확인 불가"
    : n.toLocaleString("ko-KR", { maximumFractionDigits: digits });
export const stateName: Record<string, string> = {
  WATCHING: "돌파 대기",
  BREAKOUT: "돌파 관측",
  RETEST_HELD: "재지지 관측",
  FAILED: "돌파 실패",
  EXPIRED: "10거래일 관찰 만료",
  DATA_UNAVAILABLE: "데이터 확인 필요",
};
