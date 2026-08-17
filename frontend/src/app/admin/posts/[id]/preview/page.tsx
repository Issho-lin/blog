import { AdminPostPreviewView } from "./PostPreviewClient";

type PageProps = {
  params: Promise<{ id: string }>;
};

export default async function AdminPostPreviewPage({ params }: PageProps) {
  const { id } = await params;
  return <AdminPostPreviewView postId={id} />;
}
