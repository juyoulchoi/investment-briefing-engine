import InvestmentShell from "../../components/InvestmentShell";

export default function InvestmentLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <InvestmentShell>{children}</InvestmentShell>;
}
