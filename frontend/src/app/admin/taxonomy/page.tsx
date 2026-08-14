import { AdminTaxonomyView } from "./TaxonomyClient";

type PageProps = {
  params: Promise<Record<string, never>>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function AdminTaxonomyPage({ params, searchParams }: PageProps) {
  await params;
  await searchParams;
  return <AdminTaxonomyView />;
}
