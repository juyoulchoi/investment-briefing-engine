"use client";
import { useEffect, useState } from "react";
import { useNotify } from "../../shared/notifications";
import { getReferenceRows, saveReference } from "./api";
import { defaults, meta } from "./model";
import type { Kind, Row } from "./types";
export function useReference() {
  const notify = useNotify();
  const [kind, setKind] = useState<Kind>("indices"),
    [rows, setRows] = useState<Row[]>([]),
    [indices, setIndices] = useState<Row[]>([]),
    [loading, setLoading] = useState(true),
    [editing, setEditing] = useState<Row | null>(null),
    [form, setForm] = useState<Row>({}),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false),
    [query, setQuery] = useState("");
  const load = async (k = kind) => {
    setLoading(true);
    setError("");
    try {
      const data = await getReferenceRows(k);
      setRows(data);
      if (k === "indices") setIndices(data);
      else if (!indices.length) setIndices(await getReferenceRows("indices"));
    } catch (e) {
      setError(e instanceof Error ? e.message : "조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load(kind);
  }, [kind]);
  const open = (row?: Row) => {
    setEditing(row || {});
    setForm(row ? { ...row } : { ...defaults[kind] });
    setError("");
  };
  const save = async () => {
    for (const f of meta[kind].fields)
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
      const id = form[meta[kind].id];
      await saveReference(kind, id, form);
      setEditing(null);
      await load();
      notify(`${meta[kind].title} 정보가 저장되었습니다.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };
  const shown = rows.filter((r) =>
      Object.values(r).some((v) =>
        String(v ?? "")
          .toLowerCase()
          .includes(query.toLowerCase()),
      ),
    ),
    display = (key: string, v: any, r: Row) =>
      key === "baseIndexCode"
        ? indices.find((i) => i.indexCode === v)?.indexName || "-"
        : key.endsWith("Amount")
          ? Number(v || 0).toLocaleString("ko-KR")
          : v === "Y"
            ? "예"
            : v === "N"
              ? "아니오"
              : String(v ?? "-");
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
    query,
    setQuery,
    open,
    save,
    shown,
    display,
  };
}
