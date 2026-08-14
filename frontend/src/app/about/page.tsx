import type { Metadata } from "next";
import { PageIntro, PublicShell } from "@/components/PublicShell";
import { PostContent } from "@/components/PostContent";
import { loadPublicSiteSettings } from "@/lib/site-settings";

export async function generateMetadata(): Promise<Metadata> {
  const settings = await loadPublicSiteSettings();
  return {
    title: "关于",
    description: settings.siteDescription || `关于 ${settings.siteName}`,
  };
}

export default async function AboutPage() {
  const settings = await loadPublicSiteSettings();

  return (
    <PublicShell>
      <PageIntro
        eyebrow={settings.siteSubtitle || "關於"}
        title="关于"
        description={settings.authorName ? `作者 · ${settings.authorName}` : undefined}
      />
      <section className="soft-in-delay pt-10">
        {settings.authorAvatarUrl ? (
          <img
            src={settings.authorAvatarUrl}
            alt={settings.authorName || settings.siteName}
            className="mb-8 h-20 w-20 rounded-full border border-line object-cover"
          />
        ) : null}
        {settings.aboutHtml ? (
          <PostContent html={settings.aboutHtml} />
        ) : (
          <p className="text-mist">{settings.siteDescription || "关于页还没有内容。"}</p>
        )}
      </section>
    </PublicShell>
  );
}
