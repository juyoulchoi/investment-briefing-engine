export type DateRange = { from: string; to: string };
export const todayInSeoul = (now = new Date()) =>
  new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Seoul" }).format(now);
export function daysBefore(to: string, days: number) {
  const date = new Date(to + "T00:00:00Z");
  date.setUTCDate(date.getUTCDate() - days);
  return date.toISOString().slice(0, 10);
}
export function yearsBefore(to: string, years: number) {
  const date = new Date(to + "T00:00:00Z");
  date.setUTCFullYear(date.getUTCFullYear() - years);
  return date.toISOString().slice(0, 10);
}
