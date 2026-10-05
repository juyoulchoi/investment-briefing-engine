"use client";
import Link from "next/link";
import { usePathname,useRouter } from "next/navigation";
import React,{ useState } from "react";
import { nav,type Page } from "../lib/pages";
import { AccountTypeProvider } from "../shared/account-context";
import { NotificationContext } from "../shared/notifications";

export default function InvestmentShell({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const page = usePathname().split("/")[1];
  const [toast, setToast] = useState(""),
    [refreshing, setRefreshing] = useState(false),
    [contentVersion, setContentVersion] = useState(0);
  const notify = (m: string) => {
      setToast(m);
      setTimeout(() => setToast(""), 2200);
    },
    refreshMarketData = async () => {
      setRefreshing(true);
      try {
        const response = await fetch("/api/market-data/holdings/refresh", {
          method: "POST",
        });
        const result = (await response.json()) as {
          success: boolean;
          krxReceivedCounts: Record<string, number>;
          overseasRequestedCount: number;
          overseasSuccessCount: number;
          completedSteps: string[];
          failures: string[];
        };
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        setContentVersion((v) => v + 1);
        router.refresh();
        const domestic = Object.values(result.krxReceivedCounts || {}).reduce(
          (sum, count) => sum + count,
          0,
        );
        notify(
          result.success
            ? `1~10단계 ${result.completedSteps.length}개 완료 · 국내·ETF ${domestic.toLocaleString("ko-KR")}건 · 해외 ${result.overseasSuccessCount}/${result.overseasRequestedCount}개`
            : `일부 갱신 실패: ${result.failures.join(" · ")}`,
        );
      } catch (e) {
        notify(
          e instanceof Error ? e.message : "시장 데이터 갱신에 실패했습니다.",
        );
      } finally {
        setRefreshing(false);
      }
    };
  return (
    <AccountTypeProvider>
      <NotificationContext.Provider value={notify}>
        <div className="shell">
          <aside>
            <Link className="brand" href="/dashboard">
              <b>F</b>
              <span>
                <strong>FINBRIEF</strong>
                <small>Investment OS</small>
              </span>
            </Link>
            <nav>
              <p>OVERVIEW</p>
              {nav.slice(0, 1).map((x) => (
                <Nav key={x[0]} x={x} active={page === x[0]} />
              ))}
              <p>INVEST</p>
              {nav.slice(1, 4).map((x) => (
                <Nav key={x[0]} x={x} active={page === x[0]} />
              ))}
              <p>RECORDS</p>
              {nav.slice(4).map((x) => (
                <Nav key={x[0]} x={x} active={page === x[0]} />
              ))}
            </nav>
            <div className="user">
              <b>민</b>
              <span>
                <strong>김투자</strong>
                <small>개인 포트폴리오</small>
              </span>
            </div>
          </aside>
          <main key={contentVersion}>
            <header className="top">
              <div>
                <h1>{nav.find((x) => x[0] === page)?.[1]}</h1>
              </div>
              <div>
                <span className="latest">
                  {page === "volumescreen"
                    ? "● 조회 범위·공백 확인"
                    : "● 데이터 최신"}
                </span>
                <button
                  className="primary"
                  disabled={refreshing}
                  onClick={refreshMarketData}
                >
                  {refreshing ? "갱신 중..." : "↻ 브리핑 갱신"}
                </button>
              </div>
            </header>
            {children}
          </main>
          <nav className="mobile" aria-label="모바일 전체 메뉴">
            {nav.map((x) => (
              <Link
                key={x[0]}
                className={page === x[0] ? "active" : ""}
                href={`/${x[0]}`}
                aria-current={page === x[0] ? "page" : undefined}
              >
                <b>{x[2]}</b>
                {x[1]}
              </Link>
            ))}
          </nav>
          {toast && <div className="toast">{toast}</div>}
        </div>
      </NotificationContext.Provider>
    </AccountTypeProvider>
  );
}

function Nav({ x, active }: { x: [Page, string, string]; active: boolean }) {
  return (
    <Link
      className={`nav ${active ? "active" : ""}`}
      href={`/${x[0]}`}
      aria-current={active ? "page" : undefined}
    >
      <b>{x[2]}</b>
      {x[1]}
    </Link>
  );
}
