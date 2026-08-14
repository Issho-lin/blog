import { AdminSiteSettings } from "./SettingsClient";

type PageProps = {
  params: Promise<Record<string, never>>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function AdminSettingsPage({ params, searchParams }: PageProps) {
  await params;
  await searchParams;
  return <AdminSiteSettings />;
}
