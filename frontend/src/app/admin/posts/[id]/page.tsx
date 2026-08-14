import { AdminPostEditor } from "./PostEditorClient";

type PageProps = {
  params: Promise<{ id: string }>;
};

export default async function AdminPostEditorPage({ params }: PageProps) {
  const { id } = await params;
  return <AdminPostEditor postId={id} />;
}
