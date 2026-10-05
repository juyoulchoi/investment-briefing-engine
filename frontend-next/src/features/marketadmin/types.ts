export type Kind = "snapshots" | "sentiments";
export type Row = Record<string, any>;
export type Field = {
  key: string;
  label: string;
  type?: string;
  required?: boolean;
  options?: [string, string][];
};
