export const pageIds = [
  "dashboard", "briefing", "holdings", "additional", "history", "reference",
  "operations", "marketadmin", "bondyields", "volumescreen", "exchangerates",
] as const;
export type Page = (typeof pageIds)[number];
