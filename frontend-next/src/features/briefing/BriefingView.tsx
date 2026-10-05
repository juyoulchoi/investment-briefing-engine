import type { DashboardData } from "../dashboard/types";

export default function BriefingView({
  initialData,
}: {
  initialData: DashboardData | null;
}) {
  const data = initialData;
  if (!data?.title || !data.briefingArticles?.length)
    return (
      <div className="page">
        <section className="card data-state">
          발행된 최신 투자 브리핑이 없습니다.
        </section>
      </div>
    );
  return (
    <div className="page">
      <section className="briefHero">
        <h2>{data.briefingBaseDate}</h2>
        <p>
          {data.summary || "최신 투자 판단을 바탕으로 생성된 브리핑입니다."}
        </p>
        <div>
          {data.briefingArticles.slice(0, 3).map((item, i) => (
            <article key={item.itemCode}>
              <b>{String(i + 1).padStart(2, "0")}</b>
              {item.summary}
            </article>
          ))}
        </div>
      </section>
      <section className="card briefing-sections">
        {data.briefingArticles.map((item, i) => (
          <article key={item.itemCode}>
            <b>{String(i + 1).padStart(2, "0")}</b>
            <div>
              <h3>{item.summary}</h3>
              <p>{item.content}</p>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
