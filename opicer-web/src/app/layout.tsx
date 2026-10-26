import type { Metadata } from "next";
import { JetBrains_Mono, Jua } from "next/font/google";
import "pretendard/dist/web/variable/pretendardvariable-dynamic-subset.css";
import "./globals.css";

// Body text uses Pretendard (see globals.css); Jua is only for the "opicer" wordmark.
const jua = Jua({
  variable: "--font-jua",
  weight: "400",
  subsets: ["latin"],
});

const jetBrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Opicer — AI OPIC 스피킹 코치",
  description: "내가 어떻게 말하는지 기억하는 AI OPIC 코치. 주제별 연습부터 레벨업까지.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${jua.variable} ${jetBrainsMono.variable}`}>
      <body>{children}</body>
    </html>
  );
}
