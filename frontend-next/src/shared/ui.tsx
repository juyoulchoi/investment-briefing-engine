import React from "react";
import { won } from "./format";
export const Head = ({
  k: _k,
  t,
  a,
  click,
}: {
  k: string;
  t: string;
  a: string;
  click?: () => void;
}) => (
  <header className="head">
    <div>
      <h3>{t}</h3>
    </div>
    <button type="button" onClick={click}>
      {a} →
    </button>
  </header>
);

export function Metric({
  t,
  v,
  d,
  i,
}: {
  t: string;
  v: string;
  d: string;
  i: string;
}) {
  return (
    <article className="metric">
      <b>{i}</b>
      <span>
        <small>{t}</small>
        <strong>{v}</strong>
        <em>{d}</em>
      </span>
    </article>
  );
}

export function Account({
  n,
  v,
  p,
  g,
  cash,
}: {
  n: string;
  v: string;
  p: string;
  g: string;
  cash: string;
}) {
  return (
    <div>
      <i />
      <b className="account-name">
        {n}
        <small>예수금 {cash}</small>
      </b>
      <span className="account-value">{v}</span>
      <small>{p}</small>
      <em className={g[0] === "+" ? "pos" : g[0] === "-" ? "neg" : ""}>{g}</em>
    </div>
  );
}

export function Signal({ tag, n, v }: { tag: string; n: string; v: string }) {
  return (
    <div className="signal">
      <b>{tag}</b>
      <span>
        <strong>{n}</strong>
      </span>
      <em>{v}</em>
    </div>
  );
}

export function Article({ no, t }: { no: string; t: string }) {
  return (
    <article>
      <b>{no}</b>
      <span>
        <strong>{t}</strong>
      </span>
    </article>
  );
}

export function Tabs({
  vals,
  active,
  set,
}: {
  vals: string[];
  active: string;
  set: (s: string) => void;
}) {
  return (
    <div className="tabs">
      {vals.map((x) => (
        <button
          key={x}
          className={x === active ? "active" : ""}
          onClick={() => set(x)}
        >
          {x}
        </button>
      ))}
    </div>
  );
}

export const Summary = ({
  evaluation,
  profit,
  format = won,
}: {
  evaluation: number;
  profit: number;
  format?: (n: number) => string;
}) => (
  <section className="summary" style={{ gridTemplateColumns: "repeat(2,1fr)" }}>
    <div>
      <small>평가금액</small>
      <strong>{format(evaluation)}</strong>
    </div>
    <div>
      <small>평가손익</small>
      <strong className={profit >= 0 ? "pos" : "neg"}>
        {profit >= 0 ? "+" : ""}
        {format(profit)}
      </strong>
    </div>
  </section>
);

export const Badge = ({
  children,
  buy,
}: {
  children: React.ReactNode;
  buy?: boolean;
}) => <span className={`badge ${buy ? "buy" : ""}`}>{children}</span>;

export const Table = ({
  heads,
  children,
}: {
  heads: string[];
  children: React.ReactNode;
}) => (
  <div className="tablewrap">
    <table>
      <thead>
        <tr>
          {heads.map((x) => (
            <th key={x}>{x}</th>
          ))}
        </tr>
      </thead>
      <tbody>{children}</tbody>
    </table>
  </div>
);
