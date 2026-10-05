import { notFound } from "next/navigation";
import InvestmentClient from "../../components/InvestmentClient";
import { pageIds, type Page } from "../../lib/pages";

export function generateStaticParams() {
  return pageIds.map((page) => ({ page }));
}

export default async function InvestmentPage({ params }: { params: Promise<{ page: string }> }) {
  const { page } = await params;
  if (!pageIds.includes(page as Page)) notFound();
  return <InvestmentClient page={page as Page} />;
}
