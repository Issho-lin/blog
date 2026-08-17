import { AdminDashboard } from "./DashboardClient";

type PageProps = {
  params: Promise<Record<string, never>>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function AdminHomePage({ params, searchParams }: PageProps) {
  await params;
  await searchParams;
  return <AdminDashboard />;
}
