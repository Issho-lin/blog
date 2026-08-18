import { AdminCommentsView } from "./CommentsClient";

type PageProps = {
  params: Promise<Record<string, never>>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function AdminCommentsPage({ params, searchParams }: PageProps) {
  await params;
  await searchParams;
  return <AdminCommentsView />;
}
