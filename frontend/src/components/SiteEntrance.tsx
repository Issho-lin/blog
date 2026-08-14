"use client";

import { useEffect, useState } from "react";
import { SealMark } from "@/components/SealMark";

type Phase = "pending" | "splash" | "reveal" | "done";

const siteName = process.env.NEXT_PUBLIC_SITE_NAME ?? "Linqibin Blog";

export function SiteEntrance({ children }: { children: React.ReactNode }) {
  const [phase, setPhase] = useState<Phase>("pending");

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const reload = document.documentElement.dataset.splash === "1";

    if (!reload || reduced) {
      document.documentElement.removeAttribute("data-splash");
      setPhase("done");
      return;
    }

    setPhase("splash");
    const revealTimer = window.setTimeout(() => setPhase("reveal"), 1660);
    const doneTimer = window.setTimeout(() => {
      document.documentElement.removeAttribute("data-splash");
      setPhase("done");
    }, 2360);

    return () => {
      window.clearTimeout(revealTimer);
      window.clearTimeout(doneTimer);
    };
  }, []);

  if (phase === "done") {
    return <>{children}</>;
  }

  return (
    <>
      {children}
      <div
        className={
          phase === "reveal" ? "splash-overlay splash-overlay-exit" : "splash-overlay"
        }
        role="presentation"
      >
        {phase === "splash" || phase === "reveal" ? (
          <div className="splash-stage">
            <div className="splash-seal-wrap">
              <SealMark size={96} className="text-seal" />
            </div>
            <p className="splash-title font-serif">{siteName}</p>
            <span className="splash-rule" aria-hidden />
            <p className="splash-caption">開卷</p>
          </div>
        ) : null}
      </div>
    </>
  );
}
