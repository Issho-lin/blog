import type { Metadata } from "next";
import Script from "next/script";
import localFont from "next/font/local";
import { Noto_Sans_SC, Noto_Serif_SC } from "next/font/google";
import { SiteEntrance } from "@/components/SiteEntrance";
import "./globals.css";

const notoSerif = Noto_Serif_SC({
  variable: "--font-noto-serif",
  weight: ["400", "600", "700"],
  subsets: ["latin"],
  display: "swap",
});

const notoSans = Noto_Sans_SC({
  variable: "--font-noto-sans",
  weight: ["400", "500", "700"],
  subsets: ["latin"],
  display: "swap",
});

const zhuanTi = localFont({
  src: "../fonts/YiShanBeiZhuanTi.ttf",
  variable: "--font-zhuan",
  display: "swap",
});

const siteName = process.env.NEXT_PUBLIC_SITE_NAME ?? "Linqibin Blog";

export const metadata: Metadata = {
  title: {
    default: siteName,
    template: `%s · ${siteName}`,
  },
  description: "个人技术博客：记录学习、工程实践与写作。",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="zh-CN"
      data-scroll-behavior="smooth"
      className={`${notoSerif.variable} ${notoSans.variable} ${zhuanTi.variable} h-full`}
      suppressHydrationWarning
    >
      <body className="min-h-full antialiased">
        <Script id="splash-on-reload" strategy="beforeInteractive">
          {`(function(){try{var n=performance.getEntriesByType("navigation")[0];if(n&&n.type==="reload")document.documentElement.dataset.splash="1";}catch(e){}})();`}
        </Script>
        <SiteEntrance>{children}</SiteEntrance>
      </body>
    </html>
  );
}
