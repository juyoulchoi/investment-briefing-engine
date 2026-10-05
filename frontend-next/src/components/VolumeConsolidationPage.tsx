import React, { useEffect, useRef, useState } from "react";
import "./volume-consolidation.css";

type Metrics = {
  returnPct: number;
  closeRangePct: number;
  volumeMultiple: number;
  volumeIncreasePct: number;
  spikeDays: number;
  sustainedDays: number;
  maxDailyVolumeMultiple: number;
  averageValue: number;
  boxHigh: number;
  boxLow: number;
  close: number;
  movingAverage20: number;
  aboveMovingAverage20: boolean;
  return60Pct: number;
  drawdown60Pct: number;
};
type Candidate = {
  market: string;
  stockCode: string;
  stockName: string;
  baseDate: string;
  masterDate: string | null;
  quantitativeMatch: boolean;
  verificationStatus: string;
  exclusionReasons: string[];
  checksRequired: string[];
  metrics: Metrics | null;
  foreignNetAmount: number | null;
  institutionNetAmount: number | null;
  flowStatus: string;
  disclosureStatus: string;
};
type Screen = {
  requestedDate: string;
  baseDate: string | null;
  observationFrom: string | null;
  baselineFrom: string | null;
  baselineTo: string | null;
  availableDays: number;
  dateCoverageStatus: string;
  unverifiedWeekdays: string[];
  rules: { version: string };
  coverage: {
    market: string;
    latestDate: string | null;
    masterDate: string | null;
    latestStockCount: number;
  }[];
  universeCount: number;
  matchCount: number;
  exclusionCounts: Record<string, number>;
  warnings: string[];
  rows: Candidate[];
};
type Tracking = {
  stockCode: string;
  selectionDate: string;
  evaluatedThrough: string;
  boxHigh: number;
  boxLow: number;
  state: string;
  events: {
    date: string;
    type: string;
    close: number;
    volumeMultiple: number;
  }[];
  warnings: string[];
};
type SearchRun = {
  runId: string;
  triggerCode: string;
  collectionBaseDate: string | null;
  baseDate: string | null;
  status: string;
  matchCount: number | null;
  startedAt: string;
  finishedAt: string | null;
  failureReason: string | null;
  hasResult: boolean;
};
const runStatus: Record<string, string> = {
  RUNNING: "검색 중",
  CALCULATED: "수치 계산 완료 · 검증 필요",
  PARTIAL: "잠정 계산 · 데이터 보완 필요",
  FAILED: "검색 실패",
  SKIPPED: "검색 생략",
};
const endpoint = "/api/v1/krx/screens/volume-consolidation";
const todayInSeoul = () =>
  new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Seoul" }).format(
    new Date(),
  );
const num = (n: number | null | undefined, digits = 2) =>
  n == null
    ? "확인 불가"
    : n.toLocaleString("ko-KR", { maximumFractionDigits: digits });
const stateName: Record<string, string> = {
  WATCHING: "돌파 대기",
  BREAKOUT: "돌파 관측",
  RETEST_HELD: "재지지 관측",
  FAILED: "돌파 실패",
  EXPIRED: "10거래일 관찰 만료",
  DATA_UNAVAILABLE: "데이터 확인 필요",
};
async function request<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal });
  const body = await response.json();
  if (!response.ok || body?.success === false)
    throw new Error(body?.error?.message || `HTTP ${response.status}`);
  return (body?.data ?? body) as T;
}

export default function VolumeConsolidationPage() {
  const [baseDate, setBaseDate] = useState(todayInSeoul);
  const [screen, setScreen] = useState<Screen | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("matches");
  const [market, setMarket] = useState("");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<Candidate | null>(null);
  const [tracking, setTracking] = useState<Tracking | null>(null);
  const [trackingDate, setTrackingDate] = useState("");
  const [trackingError, setTrackingError] = useState("");
  const [trackingBusy, setTrackingBusy] = useState(false);
  const [page, setPage] = useState(1);
  const [runs, setRuns] = useState<SearchRun[]>([]);
  const [runsError, setRunsError] = useState("");
  const [savedLabel, setSavedLabel] = useState("");
  const active = useRef<AbortController | null>(null);
  const activeTracking = useRef<AbortController | null>(null);

  const load = async (date: string, saved?: SearchRun) => {
    const queryDate = date === todayInSeoul() ? "" : date;
    active.current?.abort();
    activeTracking.current?.abort();
    const controller = new AbortController();
    active.current = controller;
    setLoading(true);
    setError("");
    setScreen(null);
    setSelected(null);
    setTracking(null);
    setTrackingBusy(false);
    setPage(1);
    setSavedLabel(
      saved
        ? `${new Date(saved.startedAt).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" })} 실행 당시 저장 결과`
        : "현재 저장된 시세로 조회한 결과",
    );
    try {
      setScreen(
        await request<Screen>(
          saved
            ? `${endpoint}/runs/${saved.runId}/result`
            : `${endpoint}${queryDate ? `?baseDate=${queryDate}` : ""}`,
          controller.signal,
        ),
      );
    } catch (e) {
      if (!controller.signal.aborted)
        setError(e instanceof Error ? e.message : "조회 실패");
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  };
  useEffect(() => {
    void load(todayInSeoul());
    const historyController = new AbortController();
    request<SearchRun[]>(`${endpoint}/runs`, historyController.signal)
      .then(setRuns)
      .catch((e) => {
        if (!historyController.signal.aborted)
          setRunsError(
            e instanceof Error ? e.message : "자동 검색 이력 조회 실패",
          );
      });
    return () => {
      historyController.abort();
      active.current?.abort();
      activeTracking.current?.abort();
    };
  }, []);
  useEffect(() => {
    setPage(1);
  }, [query, market, filter]);

  const track = async () => {
    if (!selected) return;
    activeTracking.current?.abort();
    const controller = new AbortController();
    activeTracking.current = controller;
    setTrackingBusy(true);
    setTrackingError("");
    setTracking(null);
    const params = new URLSearchParams({
      market: selected.market,
      stockCode: selected.stockCode,
      selectionDate: selected.baseDate,
    });
    if (trackingDate) params.set("asOf", trackingDate);
    try {
      setTracking(
        await request<Tracking>(
          `${endpoint}/tracking?${params}`,
          controller.signal,
        ),
      );
    } catch (e) {
      if (!controller.signal.aborted)
        setTrackingError(e instanceof Error ? e.message : "추적 실패");
    } finally {
      if (!controller.signal.aborted) setTrackingBusy(false);
    }
  };
  const choose = (row: Candidate) => {
    activeTracking.current?.abort();
    setSelected(row);
    setTracking(null);
    setTrackingError("");
    setTrackingBusy(false);
  };
  const shown = (screen?.rows ?? []).filter(
    (r) =>
      (filter === "all" ||
        (filter === "matches" ? r.quantitativeMatch : !r.quantitativeMatch)) &&
      (!market || r.market === market) &&
      `${r.stockName} ${r.stockCode}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const pages = Math.max(1, Math.ceil(shown.length / 50));
  const pageRows = shown.slice((page - 1) * 50, page * 50);

  return (
    <div className="page ref-page volume-screen">
      <section className="card table ref-table">
        <header className="head">
          <div>
            <h2>에너지 응축 후보 · 수집 후 자동 검색</h2>
            <p>
              월~토 오전 8시 20분 시세 수집이 끝나면 거래량·횡보 조건을 계산하고
              결과를 저장합니다. 매집 확정이나 매수 지시를 뜻하지 않습니다.
            </p>
          </div>
        </header>
        {runsError && (
          <p role="alert" className="form-error">
            {runsError}
          </p>
        )}
        {!runsError && runs.length === 0 && (
          <p>아직 저장된 검색 실행 이력이 없습니다.</p>
        )}
        {runs.length > 0 && (
          <div className="tablewrap">
            <table>
              <thead>
                <tr>
                  <th>실행 시간 (한국)</th>
                  <th>실행 구분</th>
                  <th>시세 기준일</th>
                  <th>상태</th>
                  <th>수치 통과</th>
                  <th>결과</th>
                </tr>
              </thead>
              <tbody>
                {runs.map((run) => (
                  <tr key={run.runId}>
                    <td>
                      {new Date(run.startedAt).toLocaleString("ko-KR", {
                        timeZone: "Asia/Seoul",
                      })}
                    </td>
                    <td>
                      {run.triggerCode === "AFTER_KRX_COLLECTION"
                        ? "시세 수집 후"
                        : "수동 계산"}
                    </td>
                    <td>{run.baseDate ?? run.collectionBaseDate ?? "—"}</td>
                    <td>
                      {runStatus[run.status] ?? run.status}
                      {run.failureReason && (
                        <small className="volume-code">
                          {run.failureReason}
                        </small>
                      )}
                    </td>
                    <td>
                      {run.matchCount == null ? "—" : `${run.matchCount}종목`}
                    </td>
                    <td>
                      <button
                        disabled={!run.hasResult || loading}
                        onClick={() => void load("", run)}
                      >
                        저장 결과 보기
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <section className="card volume-panel">
        <h2>거래량 증가·횡보 후보</h2>
        <p>
          최근 15거래일 가격 횡보와 거래 증가를 검색합니다. 순위는 조건 부합
          순서이며 상승 확률이 아닙니다.
        </p>
        <form
          className="volume-controls"
          onSubmit={(e) => {
            e.preventDefault();
            void load(baseDate);
          }}
        >
          <label>
            조회 기준일{" "}
            <input
              type="date"
              max={todayInSeoul()}
              value={baseDate}
              onChange={(e) => setBaseDate(e.target.value)}
            />
          </label>
          <button className="primary" disabled={loading}>
            {loading ? "계산 중…" : "후보 검색"}
          </button>
          <span>
            오늘 또는 빈 날짜는 어제까지 저장된 최신 시세로 조회합니다. 실제
            시세 기준일은 결과에 표시됩니다.
          </span>
        </form>
        <details>
          <summary>검색 기준과 판정 방법</summary>
          <ul>
            <li>3주 수익률 −5%~+5%, 15일 최고·최저 종가 범위 10% 이하</li>
            <li>15일 평균 거래량 / 겹치지 않는 직전 60일 평균 ≥ 2배</li>
            <li>각 날짜 직전 20일 평균 대비 5배 이상 ≥ 1일, 2배 이상 ≥ 3일</li>
            <li>
              15일 평균 거래대금 ≥ 20억원. 보통주만 포함하며 확인된 스팩·위험
              지정, 데이터 누락·거래 없음, 상장주식수 변동 제외
            </li>
            <li>정렬: 2배 지속일수 ↓ → 평균 거래량 배수 ↓ → 종가 횡보 폭 ↑</li>
            <li>
              선정일 15일 최고 고가·최저 저가를 고정합니다. 이후 10거래일 내
              상단 종가 돌파와 거래량 2배를 확인하고, 돌파 후 5거래일 내 상단
              ±1% 저가 재접촉 및 상단 이상 종가를 재지지로 판정합니다.
            </li>
            <li>
              돌파 후 종가가 상단 아래로 내려오면 실패입니다. 신호 확인 후 다음
              거래일 체결을 검토하는 분석 기능입니다.
            </li>
          </ul>
        </details>
        {error && (
          <p role="alert" className="form-error">
            {error}
          </p>
        )}
        {loading && (
          <p role="status">저장된 코스피·코스닥 시세를 계산하고 있습니다.</p>
        )}
      </section>
      {screen && (
        <>
          <section className="card volume-panel">
            <p>
              <strong>{savedLabel}</strong>
            </p>
            <div className="volume-summary">
              <div>
                <small>실제 기준일</small>
                <strong>{screen.baseDate ?? "데이터 없음"}</strong>
              </div>
              <div>
                <small>수치 조건 통과 · 검증 필요</small>
                <strong>{screen.matchCount}종목</strong>
              </div>
              <div>
                <small>조회 대상</small>
                <strong>{num(screen.universeCount, 0)}종목</strong>
              </div>
              <div>
                <small>저장된 거래일</small>
                <strong>{screen.availableDays} / 75일</strong>
              </div>
            </div>
            <p>
              관찰: {screen.observationFrom ?? "—"} ~ {screen.baseDate ?? "—"} ·
              비교: {screen.baselineFrom ?? "—"} ~ {screen.baselineTo ?? "—"}
            </p>
            <div className="volume-notice">
              <strong>
                {screen.dateCoverageStatus === "DATE_GAPS_UNVERIFIED"
                  ? "날짜 공백 확인 필요 · 저장일 기준 잠정 계산"
                  : "후보 최종 검증이 필요합니다"}
              </strong>
              <ul>
                {screen.warnings.map((w) => (
                  <li key={w}>{w}</li>
                ))}
              </ul>
              {screen.unverifiedWeekdays.length > 0 && (
                <p>
                  시세·휴장 여부 미확인 평일:{" "}
                  {screen.unverifiedWeekdays.join(", ")}
                </p>
              )}
            </div>
            <details>
              <summary>시장별 수집 범위와 제외 사유 집계</summary>
              {screen.coverage.map((c) => (
                <p key={c.market}>
                  {c.market}: 시세 {c.latestDate ?? "없음"} · 기본정보{" "}
                  {c.masterDate ?? "없음"} · 최신 시세 {c.latestStockCount}종목
                </p>
              ))}
              <ul>
                {Object.entries(screen.exclusionCounts).map(
                  ([reason, count]) => (
                    <li key={reason}>
                      {reason}: {count}종목
                    </li>
                  ),
                )}
              </ul>
              <p>
                한 종목에 여러 제외 사유가 있을 수 있습니다. 규칙:{" "}
                {screen.rules.version}
              </p>
            </details>
          </section>
          <section className="card table ref-table volume-results">
            <header className="head list-tools">
              <h3>
                종목 검색 결과{" "}
                <small>
                  {num(shown.length, 0)}건 / 전체 {num(screen.universeCount, 0)}
                  종목
                </small>
              </h3>
              <div className="list-actions volume-controls">
                <label>
                  결과{" "}
                  <select
                    className="ref-search"
                    value={filter}
                    onChange={(e) => setFilter(e.target.value)}
                  >
                    <option value="matches">조건 통과</option>
                    <option value="excluded">제외 종목</option>
                    <option value="all">전체</option>
                  </select>
                </label>
                <label>
                  시장{" "}
                  <select
                    className="ref-search"
                    value={market}
                    onChange={(e) => setMarket(e.target.value)}
                  >
                    <option value="">전체</option>
                    <option>KOSPI</option>
                    <option>KOSDAQ</option>
                  </select>
                </label>
                <label>
                  종목 검색{" "}
                  <input
                    className="ref-search"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="종목명 또는 코드"
                  />
                </label>
              </div>
            </header>
            {screen.matchCount === 0 && screen.universeCount > 0 && (
              <p className="volume-empty-notice" role="status">
                {num(screen.universeCount, 0)}종목을 조회했으며 현재 조건을 모두
                통과한 종목은 없습니다.
                {filter !== "all" && (
                  <button
                    className="edit-button"
                    onClick={() => setFilter("all")}
                  >
                    전체 종목과 제외 사유 보기
                  </button>
                )}
              </p>
            )}
            <div className="tablewrap">
              <table>
                <thead>
                  <tr>
                    <th>순서</th>
                    <th>종목</th>
                    <th>3주 수익률</th>
                    <th>횡보 폭</th>
                    <th>평균 거래량</th>
                    <th>5배 / 2배 일수</th>
                    <th>평균 거래대금</th>
                    <th>판정</th>
                  </tr>
                </thead>
                <tbody>
                  {pageRows.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="data-state">
                        {filter !== "matches" || market || query
                          ? "선택한 시장·종목 검색 조건에 해당하는 결과가 없습니다."
                          : screen.dateCoverageStatus === "DATE_GAPS_UNVERIFIED"
                            ? "저장일 기준 잠정 계산에서 통과 종목이 없습니다. 날짜 공백 확인 전에는 실제 최근 3주 결과로 확정할 수 없습니다."
                            : screen.availableDays < 75
                              ? "계산에 필요한 거래일이 부족합니다."
                              : "해당 조건의 종목이 없습니다."}
                      </td>
                    </tr>
                  ) : (
                    pageRows.map((r, i) => (
                      <tr key={`${r.market}:${r.stockCode}`}>
                        <td>{(page - 1) * 50 + i + 1}</td>
                        <td>
                          <button
                            className="volume-stock"
                            onClick={() => choose(r)}
                          >
                            {r.stockName}
                          </button>
                          <small className="volume-code">
                            {r.market} · {r.stockCode}
                          </small>
                        </td>
                        <td>
                          {r.metrics ? `${num(r.metrics.returnPct)}%` : "—"}
                        </td>
                        <td>
                          {r.metrics ? `${num(r.metrics.closeRangePct)}%` : "—"}
                        </td>
                        <td>
                          {r.metrics ? (
                            <>
                              {num(r.metrics.volumeMultiple)}배
                              <small className="volume-code">
                                {num(r.metrics.volumeIncreasePct)}% 증가
                              </small>
                            </>
                          ) : (
                            "—"
                          )}
                        </td>
                        <td>
                          {r.metrics
                            ? `${r.metrics.spikeDays} / ${r.metrics.sustainedDays}일`
                            : "—"}
                        </td>
                        <td>
                          {r.metrics
                            ? `${num(r.metrics.averageValue / 100000000)}억원`
                            : "—"}
                        </td>
                        <td className="volume-reasons">
                          {r.quantitativeMatch
                            ? "수치 통과 · 검증 필요"
                            : r.exclusionReasons.join(" / ")}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <footer>
              <span>
                총 {num(shown.length, 0)}건 ·{" "}
                {shown.length ? (page - 1) * 50 + 1 : 0}–
                {Math.min(page * 50, shown.length)}건 표시
              </span>
              <div className="volume-pagination">
                <button
                  disabled={page <= 1}
                  onClick={() => setPage((p) => p - 1)}
                >
                  이전
                </button>
                <span>
                  {page} / {pages}
                </span>
                <button
                  disabled={page >= pages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  다음
                </button>
              </div>
            </footer>
          </section>
          {selected && (
            <section className="panel" aria-label="선택 종목 상세">
              <h3>
                {selected.stockName} · {selected.stockCode}
              </h3>
              <p>
                {selected.quantitativeMatch
                  ? "수치 조건 통과 · 최종 검증 필요"
                  : selected.exclusionReasons.join(" / ")}
              </p>
              {selected.metrics && (
                <div className="volume-summary">
                  <div>
                    <small>고정 박스 상단 / 하단</small>
                    <strong>
                      {num(selected.metrics.boxHigh, 0)} /{" "}
                      {num(selected.metrics.boxLow, 0)}원
                    </strong>
                  </div>
                  <div>
                    <small>종가 / 20일 이동평균</small>
                    <strong>
                      {num(selected.metrics.close, 0)} /{" "}
                      {num(selected.metrics.movingAverage20)}원
                    </strong>
                  </div>
                  <div>
                    <small>60일 수익률 / 고점 대비</small>
                    <strong>
                      {num(selected.metrics.return60Pct)}% /{" "}
                      {num(selected.metrics.drawdown60Pct)}%
                    </strong>
                  </div>
                </div>
              )}
              <p>
                외국인 순매수: 확인 불가 · 기관 순매수: 확인 불가 · 공시: 미확인
              </p>
              <ul>
                {selected.checksRequired.map((c) => (
                  <li key={c}>{c}</li>
                ))}
              </ul>
              {selected.quantitativeMatch &&
                screen.dateCoverageStatus === "DATE_GAPS_UNVERIFIED" && (
                  <p>
                    날짜 공백을 확인하기 전에는 돌파·재지지 추적을 실행할 수
                    없습니다.
                  </p>
                )}
              {selected.quantitativeMatch &&
                screen.dateCoverageStatus !== "DATE_GAPS_UNVERIFIED" && (
                  <>
                    <h4>선정일 {selected.baseDate}의 박스 추적</h4>
                    <div className="volume-controls">
                      <label>
                        추적 종료일{" "}
                        <input
                          type="date"
                          min={selected.baseDate}
                          value={trackingDate}
                          onChange={(e) => setTrackingDate(e.target.value)}
                        />
                      </label>
                      <button
                        onClick={() => void track()}
                        disabled={trackingBusy}
                      >
                        {trackingBusy ? "추적 중…" : "돌파·재지지 확인"}
                      </button>
                      <span>빈 날짜는 어제까지, 선정일은 1년 이내</span>
                    </div>
                    {trackingError && (
                      <p className="form-error" role="alert">
                        {trackingError}
                      </p>
                    )}
                    {tracking && (
                      <div className="volume-notice">
                        <strong>
                          {stateName[tracking.state] ?? tracking.state}
                        </strong>
                        <p>판정한 마지막 거래일: {tracking.evaluatedThrough}</p>
                        {tracking.events.length ? (
                          <ul>
                            {tracking.events.map((e) => (
                              <li key={`${e.date}:${e.type}`}>
                                {e.date} · {stateName[e.type]} · 종가{" "}
                                {num(e.close, 0)}원 · 거래량{" "}
                                {num(e.volumeMultiple)}배
                              </li>
                            ))}
                          </ul>
                        ) : (
                          <p>관측된 돌파·재지지 이벤트가 없습니다.</p>
                        )}
                        {tracking.warnings.map((w) => (
                          <p key={w}>{w}</p>
                        ))}
                      </div>
                    )}
                  </>
                )}
            </section>
          )}
        </>
      )}
    </div>
  );
}
