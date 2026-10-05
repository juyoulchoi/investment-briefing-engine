export type Account = {
  accountId: number;
  accountType: "DOMESTIC" | "OVERSEAS" | "ISA" | "PENSION";
  cashAmount: number | null;
  reservedCashAmount: number | null;
};

export type PageData<T> = { content: T[] };

export type ApiResult<T> = {
  success: boolean;
  data: T;
  error?: { message?: string };
};

export type CommonCode = {
  code: string;
  name: string;
  description: string | null;
  displayOrder: number;
};

export type AccountTypeContextValue = {
  accountTypes: Account["accountType"][];
  accountLabel: (type: Account["accountType"]) => string;
  dashboardLabel: (code: string) => string;
};
