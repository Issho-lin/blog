import { AdminPostsList } from "./PostsListClient";

type PageProps = {
  params: Promise<Record<string, never>>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function AdminPostsPage({ params, searchParams }: PageProps) {
  await params;
  await searchParams;
  return <AdminPostsList />;
}
