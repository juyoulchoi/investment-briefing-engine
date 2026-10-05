"use client";

import dynamic from "next/dynamic";
import type { Page } from "../lib/pages";

// The existing UI initializes browser storage. Mount it only in the browser
// while migrating each screen independently to Next.js.
const InvestmentApp = dynamic(() => import("./InvestmentApp"), {
  ssr: false,
  loading: () => <main aria-live="polite" style={{ padding: "2rem" }}>화면을 불러오는 중입니다…</main>,
});

export default function InvestmentClient({ page }: { page: Page }) {
  return <InvestmentApp page={page} />;
}
