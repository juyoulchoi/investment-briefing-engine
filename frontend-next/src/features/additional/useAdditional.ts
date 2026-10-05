"use client";
import { useState } from "react";
import { useAccountTypes } from "../../shared/account-context";
import type { Account } from "../../shared/types";
import { selectAdditionalAccount } from "./model";
import type { Result } from "./types";
export function useAdditional(initialData: Result) {
  const { accountTypes, accountLabel } = useAccountTypes();
  const data = initialData,
    error = "";
  const [accountType, setAccountType] =
    useState<Account["accountType"]>("DOMESTIC");
  const { rows, accountSummary } = selectAdditionalAccount(data, accountType);
  return {
    rows,
    accountSummary,
    accountTypes,
    accountLabel,
    data,
    error,
    accountType,
    setAccountType,
  };
}
