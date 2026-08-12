"use client";

import { useEffect, useState } from "react";
import { SealMark } from "@/components/SealMark";

type Phase = "boot" | "splash" | "reveal" | "ready";

export function HomeEntrance({
  siteName,
  children,
}: {
  siteName: string;
  children: React.ReactNode;
}) {
  const [phase, setPhase] = useState<Phase>("boot");

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced) {
      setPhase("ready");
      return;
    }

    const splashTimer = window.setTimeout(() => setPhase("splash"), 40);
    const revealTimer = window.setTimeout(() => setPhase("reveal"), 1700);
    const readyTimer = window.setTimeout(() => setPhase("ready"), 2400);

    return () => {
      window.clearTimeout(splashTimer);
      window.clearTimeout(revealTimer);
      window.clearTimeout(readyTimer);
    };
  }, []);

  const showOverlay = phase === "boot" || phase === "splash" || phase === "reveal";
  const showContent = phase === "reveal" || phase === "ready";
  const play = phase === "splash" || phase === "reveal";

  return (
    <div className="relative min-h-full">
      <div
        className="min-h-full"
        style={{
          opacity: showContent ? 1 : 0,
          transition: "opacity 520ms ease",
        }}
      >
        {children}
      </div>

      {showOverlay ? (
        <div
          className={
            phase === "reveal"
              ? "splash-overlay splash-overlay-exit"
              : "splash-overlay"
          }
          style={{ zIndex: 9999 }}
          role="presentation"
        >
          {play ? (
            <div className="splash-stage" key="splash-stage">
              <div className="splash-seal-wrap">
                <SealMark size={96} className="text-seal" />
              </div>
              <p className="splash-title font-serif">{siteName}</p>
              <span className="splash-rule" aria-hidden />
              <p className="splash-caption">开卷</p>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
