export type Kind = "holdings" | "regular-buys" | "cash-reserves";
export type Row = Record<string, any>;
export type Field = {
  key: string;
  label: string;
  help?: string;
  type?: string;
  required?: boolean;
  options?: [string, string][];
  wide?: boolean;
  domesticOnly?: boolean;
};
