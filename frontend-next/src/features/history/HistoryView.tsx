"use client";
import { Badge, Tabs } from "../../shared/ui";

import { useHistory } from "./useHistory";
export default function HistoryView() {
  const {
    period,
    set,
    rows,
    loading,
    error,
    detail,
    setDetail,
    detailLoading,
    openDetail,
    statusLabel,
  } = useHistory();
  if (detail)
    return (
      <div className="page">
        <button className="history-back" onClick={() => setDetail(null)}>
          ← 브리핑 이력
        </button>
        <section className="briefHero">
          <h2>{detail.baseDate}</h2>
          <p>
            {detail.summary || detail.body || "저장된 브리핑 요약이 없습니다."}
          </p>
        </section>
        <section className="card briefing-sections">
          {detail.items.length ? (
            detail.items.map((item, i) => (
              <article key={item.itemCode}>
                <b>{String(i + 1).padStart(2, "0")}</b>
                <div>
                  <h3>{item.itemTitle}</h3>
                  <p>{item.summary}</p>
                  <p>{item.content}</p>
                </div>
              </article>
            ))
          ) : (
            <p className="data-state">등록된 브리핑 세부 항목이 없습니다.</p>
          )}
        </section>
      </div>
    );
  return (
    <div className="page">
      <Tabs vals={["일일", "주간", "월간"]} active={period} set={set} />
      <section className="card history">
        {error ? (
          <p className="data-state error">{error}</p>
        ) : loading ? (
          <p className="data-state">브리핑 이력을 불러오는 중입니다.</p>
        ) : rows.length === 0 ? (
          <p className="data-state">등록된 {period} 브리핑 이력이 없습니다.</p>
        ) : (
          rows.map((r) => (
            <article key={r.briefingId}>
              <span>
                <button
                  className="history-title"
                  onClick={() => openDetail(r.briefingId)}
                  disabled={detailLoading}
                >
                  {r.baseDate}
                </button>
              </span>
              <em>
                {r.marketScore == null ? (
                  "시장점수 -"
                ) : (
                  <>
                    시장점수 <b>{Number(r.marketScore).toFixed(0)}</b>
                  </>
                )}
              </em>
              <Badge buy={r.publishedYn === "Y"}>
                {r.statusLabel || statusLabel[r.status] || r.status}
              </Badge>
              <button
                className="history-arrow"
                onClick={() => openDetail(r.briefingId)}
                disabled={detailLoading}
              >
                →
              </button>
            </article>
          ))
        )}
      </section>
    </div>
  );
}
