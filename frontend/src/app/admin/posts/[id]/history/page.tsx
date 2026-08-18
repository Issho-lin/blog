import { PostHistoryView } from "./HistoryClient";

type PageProps = {
  params: Promise<{ id: string }>;
};

export default async function PostHistoryPage({ params }: PageProps) {
  const { id } = await params;
  return <PostHistoryView postId={id} />;
}
