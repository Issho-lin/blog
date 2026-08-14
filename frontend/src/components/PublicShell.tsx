import { SiteFooter, SiteHeader } from "@/components/SiteChrome";
import { loadPublicSiteSettings } from "@/lib/site-settings";

export async function PublicShell({ children }: { children: React.ReactNode }) {
  const settings = await loadPublicSiteSettings();

  return (
    <div className="flex min-h-full flex-col">
      <SiteHeader siteName={settings.siteName} />
      <main className="mx-auto w-full max-w-3xl flex-1 px-5 pb-20 sm:px-6">
        {children}
      </main>
      <SiteFooter siteName={settings.siteName} />
    </div>
  );
}

export function PageIntro({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  children?: React.ReactNode;
}) {
  return (
    <header className="soft-in border-b border-line pb-10 pt-10 sm:pt-14">
      <p className="mb-4 text-xs tracking-[0.45em] text-seal">{eyebrow}</p>
      <h1 className="font-serif text-[clamp(2rem,5vw,2.75rem)] tracking-wide text-ink">
        {title}
      </h1>
      <span className="gold-rule gold-rule-draw mt-6" aria-hidden />
      {description ? (
        <p className="mt-5 max-w-md text-lg leading-8 text-mist">{description}</p>
      ) : null}
      {children ? <div className="mt-8">{children}</div> : null}
    </header>
  );
}
