import { Suspense } from "react";
import { AdminSiteSettings } from "./SettingsClient";

type PageProps = {
  params: Promise<Record<string, never>>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function AdminSettingsPage({ params, searchParams }: PageProps) {
  await params;
  await searchParams;
  return (
    <Suspense fallback={<p className="px-5 py-10 text-sm text-mist">加载中…</p>}>
      <AdminSiteSettings />
    </Suspense>
  );
}

