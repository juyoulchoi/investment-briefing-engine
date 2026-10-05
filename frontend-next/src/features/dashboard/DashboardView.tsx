"use client";
import type { DashboardData } from "./types";

import { usd, won } from "../../shared/format";
import {
  Account as AccountCard,
  Article,
  Head,
  Metric,
  Signal,
} from "../../shared/ui";

import { useDashboard } from "./useDashboard";
export default function DashboardView({
  initialData,
}: {
  initialData: DashboardData | null;
}) {
  const {
    go,
    accountLabel,
    dashboardLabel,
    assetAccounts,
    assetLoading,
    assetError,
    dashboard,
    dashboardLoading,
    dashboardError,
    total,
    totalCash,
    totalRate,
    latestAssetDate,
    cashRate,
    rateText,
  } = useDashboard(initialData);
  return (
    <div className="page">
      {dashboardError && <div className="batch-error">{dashboardError}</div>}
      <section className="hero">
        <div>
          <h2>
            {dashboardLoading
              ? "최신 투자 판단을 불러오는 중입니다."
              : dashboard?.title || "최신 투자 판단 데이터가 없습니다."}
          </h2>
          <p>
            {dashboard?.summary ||
              "브리핑 생성이 완료되면 최신 시장 판단이 표시됩니다."}
          </p>
        </div>
        <div className="ring">
          <strong>
            {dashboard ? Math.round(Number(dashboard.marketScore || 0)) : "-"}
          </strong>
          <small>시장점수</small>
        </div>
      </section>
      <section className="metrics">
        <Metric
          t="시장심리"
          v={dashboard ? dashboardLabel(dashboard.sentimentPhase) : "-"}
          d={
            dashboard
              ? `심리점수 ${Number(dashboard.sentimentScore || 0).toFixed(1)}`
              : "데이터 없음"
          }
          i="◒"
        />
        <Metric
          t="시장국면"
          v={dashboard ? dashboardLabel(dashboard.marketRegime) : "-"}
          d={
            dashboard
              ? `위험등급 ${dashboardLabel(dashboard.riskGrade)}`
              : "데이터 없음"
          }
          i="↗"
        />
        <Metric
          t="행동신호"
          v={dashboard ? dashboardLabel(dashboard.overallSignal) : "-"}
          d={dashboard ? `기준일 ${dashboard.baseDate}` : "데이터 없음"}
          i="⚑"
        />
        <Metric
          t="현금비중"
          v={assetLoading ? "-" : `${cashRate.toFixed(1)}%`}
          d={assetLoading ? "계산 중" : `현금·대기현금 ${won(totalCash)}`}
          i="₩"
        />
      </section>
      <div className="grid">
        <section className="card">
          <Head
            k="MY ASSETS"
            t={`계좌 현황${latestAssetDate ? ` · ${latestAssetDate} 기준` : ""}`}
            a="전체보기"
            click={() => go("holdings")}
          />
          {assetLoading ? (
            <div className="data-state">계좌 현황을 불러오는 중입니다.</div>
          ) : assetError ? (
            <div className="data-state error">{assetError}</div>
          ) : (
            <>
              <div className="total">
                <span>
                  총 자산<strong>₩ {total.toLocaleString("ko-KR")}</strong>
                </span>
                <b className={totalRate >= 0 ? "pos" : "neg"}>
                  {totalRate >= 0 ? "+" : ""}
                  {totalRate.toFixed(1)}%<small>총 수익률</small>
                </b>
              </div>
              <div className="alloc">
                {assetAccounts.map((a) => (
                  <i
                    key={a.type}
                    style={{
                      width: `${total > 0 ? (a.value / total) * 100 : 25}%`,
                    }}
                  />
                ))}
              </div>
              <div className="accounts">
                {assetAccounts.map((a) => (
                  <AccountCard
                    key={a.type}
                    n={accountLabel(a.type)}
                    v={
                      a.type === "OVERSEAS" ? usd(a.displayValue) : won(a.value)
                    }
                    p={`${(total > 0 ? (a.value / total) * 100 : 0).toFixed(1)}%`}
                    g={rateText(a)}
                    cash={
                      a.type === "OVERSEAS" ? usd(a.displayCash) : won(a.cash)
                    }
                  />
                ))}
              </div>
            </>
          )}
        </section>
        <section className="card">
          <Head
            k="ACTION SIGNAL"
            t="오늘의 행동신호"
            a="상세보기"
            click={() => go("additional")}
          />
          {dashboardLoading ? (
            <div className="data-state">행동신호를 불러오는 중입니다.</div>
          ) : dashboard?.actionSignals?.length ? (
            dashboard.actionSignals.map((signal) => (
              <Signal
                key={`${signal.stockCode}-${signal.actionSignal}`}
                tag={signal.actionSignal === "INCREASE" ? "BUY" : "●"}
                n={`${signal.stockName} ${dashboardLabel(signal.actionSignal)}`}
                v={
                  signal.accountType === "OVERSEAS"
                    ? usd(Number(signal.recommendedAmount || 0))
                    : won(Number(signal.recommendedAmount || 0))
                }
              />
            ))
          ) : (
            <div className="data-state">최신 행동신호가 없습니다.</div>
          )}
          <button className="full" onClick={() => go("operations")}>
            오늘 추천매수 총{" "}
            {won(
              Number(dashboard?.regularBuyTotal || 0) +
                Number(dashboard?.additionalBuyTotal || 0),
            )}
            <span>계획 확인 →</span>
          </button>
        </section>
      </div>
      <section className="card briefing">
        <Head
          k="DAILY BRIEFING"
          t="오늘 브리핑"
          a="전체 브리핑 읽기"
          click={() => go("briefing")}
        />
        <div className="columns">
          {dashboardLoading ? (
            <div className="data-state">최신 브리핑을 불러오는 중입니다.</div>
          ) : dashboard?.briefingArticles?.length ? (
            dashboard.briefingArticles
              .slice(0, 3)
              .map((article, index) => (
                <Article
                  key={article.itemCode}
                  no={`0${index + 1}`}
                  t={article.summary}
                />
              ))
          ) : (
            <div className="data-state">발행된 최신 브리핑이 없습니다.</div>
          )}
        </div>
      </section>
    </div>
  );
}
