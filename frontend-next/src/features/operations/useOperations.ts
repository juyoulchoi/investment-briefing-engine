"use client";
import { useEffect, useState } from "react";
import { useNotify } from "../../shared/notifications";
import {
  getOperationRows,
  getPauseReasons,
  getReferenceRows,
  saveOperation,
} from "./api";
import {
  config,
  fixedAmountRegularBuyBase,
  fixedQuantityRegularBuyBase,
  labels,
  normalizedRegularBuyForm,
  schedule,
} from "./model";
import type { Field, Kind, Row } from "./types";
export function useOperations() {
  const notify = useNotify();
  const [kind] = useState<Kind>("regular-buys"),
    [rows, setRows] = useState<Row[]>([]),
    [accounts, setAccounts] = useState<Row[]>([]),
    [stocks, setStocks] = useState<Row[]>([]),
    [accountHoldings, setAccountHoldings] = useState<Row[]>([]),
    [investmentGrades, setInvestmentGrades] = useState<Row[]>([]),
    [pauseReasons, setPauseReasons] = useState<Row[]>([]),
    [loading, setLoading] = useState(true),
    [editing, setEditing] = useState<Row | null>(null),
    [form, setForm] = useState<Row>({}),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false),
    [query, setQuery] = useState(""),
    [appliedCycleFilter, setAppliedCycleFilter] = useState(""),
    [buyStatusFilter, setBuyStatusFilter] = useState(""),
    [userPauseFilter, setUserPauseFilter] = useState(""),
    [selectedAccount, setSelectedAccount] = useState<string>("DOMESTIC");
  const load = async (k = kind) => {
    setLoading(true);
    setError("");
    try {
      const [data, a, s, h, grades, reasons] = await Promise.all([
        getOperationRows(k),
        accounts.length
          ? Promise.resolve(accounts)
          : getReferenceRows("accounts"),
        stocks.length ? Promise.resolve(stocks) : getReferenceRows("stocks"),
        accountHoldings.length
          ? Promise.resolve(accountHoldings)
          : getOperationRows("holdings"),
        investmentGrades.length
          ? Promise.resolve(investmentGrades)
          : getOperationRows("investment-grades"),
        pauseReasons.length ? Promise.resolve(pauseReasons) : getPauseReasons(),
      ]);
      setRows(data);
      setAccounts(a);
      setStocks(s);
      setAccountHoldings(h);
      setInvestmentGrades(grades);
      setPauseReasons(reasons);
    } catch (e) {
      setError(e instanceof Error ? e.message : "데이터 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load(kind);
  }, [kind]);

  const open = (r?: Row) => {
    const accountType = r?.accountType ?? selectedAccount,
      initial = r
        ? { ...r }
        : {
            ...config[kind].defaults,
            ...(kind === "regular-buys"
              ? {
                  accountId: accounts.find(
                    (a) => a.accountType === selectedAccount,
                  )?.accountId,
                }
              : {}),
          };
    setEditing(r || {});
    setForm({
      ...initial,
      ...(kind === "regular-buys" && !r
        ? ["ISA", "PENSION"].includes(accountType)
          ? fixedQuantityRegularBuyBase
          : fixedAmountRegularBuyBase
        : {}),
    });
    setError("");
  };

  const save = async () => {
    const formAccountType =
      form.accountType ??
      accounts.find((a) => Number(a.accountId) === Number(form.accountId))
        ?.accountType;
    for (const f of config[kind].fields)
      if (
        f.required &&
        (!f.domesticOnly || formAccountType === "DOMESTIC") &&
        (form[f.key] === undefined ||
          form[f.key] === null ||
          String(form[f.key]).trim() === "")
      ) {
        setError(`${f.label}을(를) 입력하세요.`);
        return;
      }
    setSaving(true);
    setError("");
    try {
      const id = form[config[kind].id];
      await saveOperation(kind, id, normalizedRegularBuyForm(kind, form));
      setEditing(null);
      await load();
      notify(`${config[kind].title} 정보가 저장되었습니다.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const viewRows: Row[] = rows.map(
      (r) =>
        ({
          ...r,
          baseSchedule: schedule(r.baseCycle, r.baseWeekDays, r.baseMonthDays),
          appliedSchedule: schedule(
            r.appliedCycle,
            r.appliedWeekDays,
            r.appliedMonthDays,
          ),
          baseValue: r.baseAmount,
          recommendedValue: r.recommendedAmount,
          appliedValue: ["ISA", "PENSION"].includes(r.accountType)
            ? r.appliedQuantity
            : r.appliedAmount,
        }) as Row,
    ),
    normalizedQuery = query.trim().toLowerCase(),
    shown = viewRows
      .filter(
        (r) =>
          (kind !== "regular-buys" || r.accountType === selectedAccount) &&
          (kind !== "regular-buys" ||
            !appliedCycleFilter ||
            r.appliedCycle === appliedCycleFilter) &&
          (kind !== "regular-buys" ||
            !buyStatusFilter ||
            r.buyStatus === buyStatusFilter) &&
          (kind !== "regular-buys" ||
            !userPauseFilter ||
            r.userPauseYn === userPauseFilter) &&
          (kind !== "regular-buys" ||
            !normalizedQuery ||
            String(r.stockName ?? "")
              .toLowerCase()
              .includes(normalizedQuery) ||
            String(r.stockCode ?? "")
              .toLowerCase()
              .includes(normalizedQuery)),
      )
      .sort((a, b) => {
        const accountOrder = ["DOMESTIC", "OVERSEAS", "ISA", "PENSION"],
          accountDiff =
            accountOrder.indexOf(String(a.accountType)) -
            accountOrder.indexOf(String(b.accountType)),
          aPriority =
            a.priority == null ? Number.NEGATIVE_INFINITY : Number(a.priority),
          bPriority =
            b.priority == null ? Number.NEGATIVE_INFINITY : Number(b.priority);
        return (
          accountDiff ||
          bPriority - aPriority ||
          String(a.stockName ?? "").localeCompare(
            String(b.stockName ?? ""),
            "ko",
          )
        );
      }),
    moneyKeys = new Set([
      "averagePrice",
      "currentPrice",
      "minimumBuyAmount",
      "appliedAmount",
      "reserveAmount",
      "accumulatedAmount",
      "usedAmount",
      "availableAmount",
      "baseValue",
      "recommendedValue",
      "appliedValue",
    ]),
    show = (k: string, v: any, r: Row) =>
      k === "investmentGrade" && !v
        ? "미설정"
        : k === "targetWeight" || k === "currentWeight"
          ? v == null
            ? "-"
            : `${Number(v).toFixed(2)}%`
          : k === "buyBasis"
            ? v === "QUANTITY"
              ? "수량"
              : "금액"
            : k === "activeYn"
              ? v === "Y"
                ? "적용"
                : "미적용"
              : k === "marketCode"
                ? v === "KO"
                  ? "국내"
                  : v === "US"
                    ? "미국"
                    : String(v ?? "-")
                : moneyKeys.has(k)
                  ? v == null
                    ? "-"
                    : k === "recommendedValue"
                      ? `${Number(v).toLocaleString("ko-KR", { maximumFractionDigits: 0 })}원`
                      : k === "appliedValue"
                        ? ["ISA", "PENSION"].includes(r.accountType)
                          ? Number(v).toLocaleString("ko-KR", {
                              maximumFractionDigits: 8,
                            })
                          : `${Number(v).toLocaleString("ko-KR", { maximumFractionDigits: 0 })}원`
                        : r.accountType === "OVERSEAS"
                          ? `USD ${Number(v).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                          : Number(v).toLocaleString("ko-KR")
                  : labels[String(v)] || String(v ?? "-");
  const regularBuyDetails: [string, string][] = [
    ["stockGrade", "종목등급"],
    ["benchmarkName", "기준지수"],
    ["targetWeight", "목표비중"],
    ["currentWeight", "계좌 전체 비중"],
    ["recommendedValue", "추천금액"],
  ];
  const regularBuyDetailRow: Row = {
    ...form,
    baseSchedule: schedule(
      form.buyCycle,
      form.buyDayCode,
      form.buyDayNumbers || form.buyDayNumber,
    ),
    baseValue: form.minimumBuyAmount,
    recommendedValue: form.recommendedAmount ?? form.recommendedBuyAmount,
    activeYn: form.activeYn ?? (form.buyStatus === "ACTIVE" ? "Y" : "N"),
    appliedSchedule: schedule(
      form.appliedCycle,
      form.appliedWeekDays,
      form.appliedMonthDays,
    ),
    appliedValue: form.appliedAmount,
  };
  const selectedAccountType = accounts.find(
      (a) => a.accountId === Number(form.accountId),
    )?.accountType,
    stockMarket =
      selectedAccountType === "OVERSEAS"
        ? "US"
        : selectedAccountType
          ? "KO"
          : null,
    stockChoices = stocks
      .filter(
        (s) =>
          s.useYn === "Y" &&
          Boolean(selectedAccountType) &&
          s.marketCode === stockMarket &&
          (Boolean(form.regularBuyKey) ||
            accountHoldings.some(
              (h) =>
                Number(h.accountId) === Number(form.accountId) &&
                Number(h.stockId) === Number(s.stockId),
            )) &&
          (Boolean(form.regularBuyKey) ||
            !rows.some(
              (r) =>
                Number(r.accountId) === Number(form.accountId) &&
                Number(r.stockId) === Number(s.stockId),
            )),
      )
      .sort((a, b) =>
        String(a.stockName).localeCompare(String(b.stockName), "ko"),
      ),
    fixedBaseField = (key: string) =>
      kind === "regular-buys" &&
      Boolean(selectedAccountType) &&
      [
        "minimumBuyAmount",
        "baseBuyQuantity",
        "buyCycle",
        "buyDayCode",
      ].includes(key);
  const csvValues = (key: string) =>
      String(form[key] || "")
        .split(",")
        .filter(Boolean),
    toggleCsv = (key: string, value: string, order: string[]) => {
      const values = csvValues(key),
        next = values.includes(value)
          ? values.filter((v) => v !== value)
          : [...values, value];
      setForm({
        ...form,
        [key]: order.filter((v) => next.includes(v)).join(",") || null,
      });
    };
  const changeFormAccount = (f: Field, rawValue: string) => {
    const accountId = Number(rawValue),
      accountType = accounts.find(
        (a) => Number(a.accountId) === accountId,
      )?.accountType;
    setForm({
      ...form,
      accountId,
      stockId: null,
      wholeSharePurchaseAmount: accountType === "DOMESTIC" ? 0 : null,
      fractionalSharePurchaseAmount: accountType === "DOMESTIC" ? 0 : null,
      ...(["DOMESTIC", "OVERSEAS"].includes(String(accountType))
        ? fixedAmountRegularBuyBase
        : ["ISA", "PENSION"].includes(String(accountType))
          ? fixedQuantityRegularBuyBase
          : {}),
    });
  };
  const changeFormGrade = (f: Field, rawValue: string) => {
    const grade = investmentGrades.find(
      (item) => item.investmentGrade === rawValue,
    );
    setForm({
      ...form,
      [f.key]: grade?.investmentGrade ?? null,
      weightScore: grade?.weightScore ?? null,
    });
  };
  const changeFormSelection = (f: Field, rawValue: string) => {
    const value = rawValue || null;
    if (kind === "regular-buys" && f.key === "buyStatus")
      setForm({
        ...form,
        buyStatus: value,
        ...(value !== "STOPPED" && form.userPauseYn !== "Y"
          ? { pauseReason: null }
          : {}),
        ...(value === "STOPPED"
          ? { appliedWeekDays: null, appliedMonthDays: null }
          : {}),
      });
    else if (kind === "regular-buys" && f.key === "userPauseYn")
      setForm({
        ...form,
        userPauseYn: value,
        ...(value !== "Y" && form.buyStatus !== "STOPPED"
          ? { pauseReason: null }
          : {}),
        ...(value === "Y"
          ? { appliedWeekDays: null, appliedMonthDays: null }
          : {}),
      });
    else if (kind === "regular-buys" && f.key === "buyBasis")
      setForm({
        ...form,
        buyBasis: value,
        ...(value === "AMOUNT"
          ? { minimumBuyAmount: 10000 }
          : { baseBuyQuantity: 1 }),
      });
    else if (kind === "regular-buys" && f.key === "appliedCycle")
      setForm(
        normalizedRegularBuyForm(kind, {
          ...form,
          appliedCycle: value,
        }),
      );
    else setForm({ ...form, [f.key]: value });
  };
  return {
    changeFormAccount,
    changeFormGrade,
    changeFormSelection,
    kind,
    accounts,
    investmentGrades,
    pauseReasons,
    loading,
    editing,
    setEditing,
    form,
    setForm,
    error,
    saving,
    query,
    setQuery,
    appliedCycleFilter,
    setAppliedCycleFilter,
    buyStatusFilter,
    setBuyStatusFilter,
    userPauseFilter,
    setUserPauseFilter,
    selectedAccount,
    setSelectedAccount,
    fixedAmountRegularBuyBase,
    fixedQuantityRegularBuyBase,
    open,
    normalizedRegularBuyForm,
    save,
    shown,
    show,
    regularBuyDetails,
    regularBuyDetailRow,
    stockChoices,
    fixedBaseField,
    csvValues,
    toggleCsv,
  };
}
