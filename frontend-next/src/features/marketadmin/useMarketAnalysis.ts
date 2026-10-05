"use client";
import { useEffect, useState } from "react";
import { useNotify } from "../../shared/notifications";
import { getMarketIndices, getMarketRows, saveMarketRow } from "./api";
import { cfg, label } from "./model";
import type { Kind, Row } from "./types";
export function useMarketAnalysis() {
  const notify = useNotify();
  const [kind, setKind] = useState<Kind>("snapshots"),
    [rows, setRows] = useState<Row[]>([]),
    [indices, setIndices] = useState<Row[]>([]),
    [loading, setLoading] = useState(true),
    [editing, setEditing] = useState<Row | null>(null),
    [form, setForm] = useState<Row>({}),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false),
    [baseDateQuery, setBaseDateQuery] = useState(""),
    [marketQuery, setMarketQuery] = useState(""),
    [marketOptions, setMarketOptions] = useState<
      { code: string; name: string }[]
    >([]);
  const load = async (k = kind) => {
    setLoading(true);
    setError("");
    try {
      const [d, i, m] = await Promise.all([
        getMarketRows(k),
        indices.length ? Promise.resolve(indices) : getMarketIndices(),
        getMarketRows("snapshots"),
      ]);
      setRows(
        [...d].sort(
          (a, b) =>
            String(b.baseDate ?? "").localeCompare(String(a.baseDate ?? "")) ||
            String(a.marketSnapshotCode ?? "").localeCompare(
              String(b.marketSnapshotCode ?? ""),
            ),
        ),
      );
      setIndices(i);
      setMarketOptions(
        m
          .filter(
            (row, index, all) =>
              all.findIndex(
                (candidate) =>
                  candidate.marketSnapshotCode === row.marketSnapshotCode,
              ) === index,
          )
          .map((row) => ({
            code: String(row.marketSnapshotCode),
            name: String(row.marketName),
          })),
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : "조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load(kind);
  }, [kind]);
  const open = (r?: Row) => {
    setEditing(r || {});
    setForm(r ? { ...r } : { ...cfg[kind].defaults });
    setError("");
  };
  const save = async () => {
    for (const f of cfg[kind].fields)
      if (
        f.required &&
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
      const id = form[cfg[kind].id];
      await saveMarketRow(kind, id, form);
      setEditing(null);
      await load();
      notify(`${cfg[kind].title} 정보가 저장되었습니다.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };
  const shown = rows.filter(
      (r) =>
        (!baseDateQuery || String(r.baseDate ?? "") === baseDateQuery) &&
        (!marketQuery || String(r.marketSnapshotCode ?? "") === marketQuery),
    ),
    show = (k: string, v: any) => label[String(v)] || String(v ?? "-");
  return {
    kind,
    setKind,
    indices,
    loading,
    editing,
    setEditing,
    form,
    setForm,
    error,
    saving,
    baseDateQuery,
    setBaseDateQuery,
    marketQuery,
    setMarketQuery,
    marketOptions,
    save,
    shown,
    show,
  };
}
