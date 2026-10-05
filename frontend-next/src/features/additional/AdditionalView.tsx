"use client";

import { orderedAccountTypes } from "../../shared/account-context";
import { usd, won } from "../../shared/format";
import type { Result } from "./types";

import { useAdditional } from "./useAdditional";
export default function AdditionalView({
  initialData,
}: {
  initialData: Result;
}) {
  const {
    rows,
    accountSummary,
    accountTypes,
    accountLabel,
    data,
    error,
    accountType,
    setAccountType,
  } = useAdditional(initialData);
  return (
    <div className="page">
      <div className="tabs account-tabs">
        {orderedAccountTypes(accountTypes).map((type) => (
          <button
            key={type}
            className={accountType === type ? "active" : ""}
            onClick={() => setAccountType(type)}
          >
            {accountLabel(type)}
          </button>
        ))}
      </div>
      <section className="cash">
        <div>
          <small>{accountLabel(accountType)} 추가매수 확보현금</small>
          <strong>{won(accountSummary.reserveAmount)}</strong>
          <p>
            {data.baseDate
              ? `${data.baseDate} 계산 기준`
              : "계산된 추가매수 계획 없음"}{" "}
            · 계좌 최대 사용 권장액 {won(accountSummary.recommendedTotal)}
          </p>
        </div>
        <div>
          <i>
            <b
              style={{ width: `${Math.min(100, accountSummary.usageRate)}%` }}
            />
          </i>
          <span>
            계좌 확보현금 사용 권장률{" "}
            <strong>{accountSummary.usageRate.toFixed(1)}%</strong>
          </span>
        </div>
      </section>
      <section className="card recommends">
        {rows.length === 0 ? (
          <p className="data-state">
            선택한 계좌의 추가매수 평가 결과가 없습니다.
          </p>
        ) : (
          rows.map((r) => (
            <article
              key={r.additionalBuyId}
              className={r.eligibleYn === "Y" ? "" : "muted-row"}
            >
              <span>
                <strong>{r.stockCode}</strong>
                <small>{r.stockName || r.stockCode}</small>
              </span>
              <em>
                {r.eligibleYn === "Y"
                  ? "추천점수 " + Number(r.score).toFixed(0)
                  : "제외"}
              </em>
              <strong>
                {r.accountType === "OVERSEAS"
                  ? usd(Number(r.recommendedAmount))
                  : won(Number(r.recommendedAmount))}
              </strong>
            </article>
          ))
        )}
      </section>
    </div>
  );
}
