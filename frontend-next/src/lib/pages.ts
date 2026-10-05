export const pageIds = [
  "dashboard",
  "briefing",
  "holdings",
  "additional",
  "history",
  "reference",
  "operations",
  "marketadmin",
  "bondyields",
  "volumescreen",
  "exchangerates",
] as const;
export type Page = (typeof pageIds)[number];

export const nav: [Page, string, string][] = [
  ["dashboard", "대시보드", "⌂"],
  ["briefing", "투자 브리핑", "▤"],
  ["holdings", "보유종목", "◇"],
  ["additional", "추가매수", "+"],
  ["history", "브리핑 이력", "◷"],
  ["reference", "기준정보 관리", "⚙"],
  ["operations", "투자 설정 관리", "⌘"],
  ["marketadmin", "시장 분석 관리", "◉"],
  ["volumescreen", "거래량·횡보 검색", "⌕"],
  ["bondyields", "FRED 채권금리", "％"],
  ["exchangerates", "환율 차트", "↗"],
];
