import type { Metadata } from "next";
import "../components/styles.css";

export const metadata: Metadata = {
  title: "FINBRIEF | Investment OS",
  description: "투자 브리핑과 포트폴리오 관리",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="ko"><body>{children}</body></html>;
}
