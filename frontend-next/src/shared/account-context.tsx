"use client";
import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import type { Account, AccountTypeContextValue, CommonCode } from "./types";
export const accountTypeContext = createContext<AccountTypeContextValue>({
  accountTypes: [],
  accountLabel: (type) => type,
  dashboardLabel: (code) => code,
});

export const isAccountType = (code: string): code is Account["accountType"] =>
  ["DOMESTIC", "OVERSEAS", "ISA", "PENSION"].includes(code);

export const useAccountTypes = () => useContext(accountTypeContext);

export const accountTabOrder: Account["accountType"][] = [
  "DOMESTIC",
  "OVERSEAS",
  "ISA",
  "PENSION",
];

export const orderedAccountTypes = (types: Account["accountType"][]) =>
  accountTabOrder.filter((type) => types.includes(type));

export const accountTabStorageKey = "investment-briefing-account-tab";

export const savedAccountTab = () => {
  const value = localStorage.getItem(accountTabStorageKey);
  return value && isAccountType(value) ? value : "DOMESTIC";
};

export function AccountTypeProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [accountCodes, setAccountCodes] = useState<CommonCode[]>([]);
  const [dashboardCodes, setDashboardCodes] = useState<CommonCode[]>([]);
  useEffect(() => {
    const loadCodes = async (group: string) => {
      const response = await fetch(`/api/v1/common-codes/${group}`);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return response.json() as Promise<CommonCode[]>;
    };
    loadCodes("ACCOUNT_TYPE")
      .then(setAccountCodes)
      .catch(() => setAccountCodes([]));
    loadCodes("DASHBOARD_LABEL")
      .then(setDashboardCodes)
      .catch(() => setDashboardCodes([]));
  }, []);
  const value = useMemo<AccountTypeContextValue>(() => {
    const activeCodes = accountCodes.filter((code) => isAccountType(code.code));
    const labels = Object.fromEntries(
      activeCodes.map((code) => [code.code, code.name]),
    ) as Partial<Record<Account["accountType"], string>>;
    const dashboardLabels = Object.fromEntries(
      dashboardCodes.map((code) => [code.code, code.name]),
    ) as Record<string, string>;
    return {
      accountTypes: activeCodes.map(
        (code) => code.code as Account["accountType"],
      ),
      accountLabel: (type) => labels[type] ?? type,
      dashboardLabel: (code) => dashboardLabels[code] ?? code,
    };
  }, [accountCodes, dashboardCodes]);
  return (
    <accountTypeContext.Provider value={value}>
      {children}
    </accountTypeContext.Provider>
  );
}
