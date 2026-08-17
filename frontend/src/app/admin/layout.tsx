"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { ApiError } from "@/lib/api/client";
import { getCurrentUser } from "@/lib/api/posts";

export default function AdminLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const onLoginPage = pathname === "/admin/login";

    getCurrentUser()
      .then(() => {
        if (cancelled) return;
        if (onLoginPage) {
          router.replace("/admin");
          return;
        }
        setReady(true);
      })
      .catch((err) => {
        if (cancelled) return;
        if (!onLoginPage && err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
          return;
        }
        if (onLoginPage) {
          setReady(true);
          return;
        }
        router.replace("/admin/login");
      });

    return () => {
      cancelled = true;
    };
  }, [pathname, router]);

  if (!ready) {
    return (
      <div className="flex min-h-full items-center justify-center px-5 text-sm text-mist">
        正在确认登录状态…
      </div>
    );
  }

  return children;
}
