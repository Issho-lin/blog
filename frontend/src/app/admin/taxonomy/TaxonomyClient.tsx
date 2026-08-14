"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminButton } from "@/components/AdminButton";
import { AdminChrome } from "@/components/AdminChrome";
import { CategoryMark, TagMark } from "@/components/TaxonomyMarks";
import { ApiError } from "@/lib/api/client";
import {
  createCategory,
  createTag,
  deleteCategory,
  deleteTag,
  listCategories,
  listTags,
  updateCategory,
  updateCategorySlug,
  updateTag,
  updateTagSlug,
} from "@/lib/api/posts";
import type { Category, Tag } from "@/lib/api/types";

export function AdminTaxonomyView() {
  const router = useRouter();
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const [categoryList, tagList] = await Promise.all([listCategories(), listTags()]);
      setCategories(categoryList);
      setTags(tagList);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/admin/login");
        return;
      }
      setError(err instanceof ApiError ? err.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  function handleError(err: unknown, fallback: string) {
    if (err instanceof ApiError && err.status === 401) {
      router.replace("/admin/login");
      return;
    }
    setError(err instanceof ApiError ? err.message : fallback);
  }

  return (
    <div className="mx-auto min-h-full w-full max-w-3xl px-5 py-10 sm:px-6">
      <AdminChrome title="分类标签" />

      {error ? (
        <p className="mb-6 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn">{error}</p>
      ) : null}

      {loading ? (
        <p className="text-sm text-mist">加载中…</p>
      ) : (
        <div className="grid gap-14">
          <section>
            <p className="text-[0.65rem] tracking-[0.32em] text-gold">卷</p>
            <h2 className="mt-1 font-serif text-xl tracking-wide text-ink">分类</h2>
            <p className="mt-2 max-w-md text-sm leading-6 text-mist">
              一篇文章只归入一卷。浏览用卷签，改名、路径或简介时再点开。
            </p>
            <CreateCategoryForm
              onCreated={(item) => setCategories((current) => [item, ...current])}
              onError={handleError}
            />
            {categories.length === 0 ? (
              <p className="mt-6 border-y border-line py-8 text-sm text-mist">还没有分类。</p>
            ) : (
              <ul className="mt-6 flex flex-wrap gap-2">
                {categories.map((category) => (
                  <CategoryRow
                    key={category.id}
                    category={category}
                    onChange={(updated) =>
                      setCategories((current) =>
                        current.map((item) => (item.id === updated.id ? updated : item))
                      )
                    }
                    onDeleted={(id) =>
                      setCategories((current) => current.filter((item) => item.id !== id))
                    }
                    onError={handleError}
                  />
                ))}
              </ul>
            )}
          </section>

          <section>
            <p className="text-[0.65rem] tracking-[0.32em] text-gold">鈐印</p>
            <h2 className="mt-1 font-serif text-xl tracking-wide text-ink">标签</h2>
            <p className="mt-2 max-w-md text-sm leading-6 text-mist">
              一篇文章可以盖多枚。浏览用印面，改名或 slug 时再展开。
            </p>
            <CreateTagForm
              onCreated={(item) => setTags((current) => [item, ...current])}
              onError={handleError}
            />
            {tags.length === 0 ? (
              <p className="mt-6 border-y border-line py-8 text-sm text-mist">还没有标签。</p>
            ) : (
              <ul className="mt-6 flex flex-wrap gap-2">
                {tags.map((tag) => (
                  <TagRow
                    key={tag.id}
                    tag={tag}
                    onChange={(updated) =>
                      setTags((current) =>
                        current.map((item) => (item.id === updated.id ? updated : item))
                      )
                    }
                    onDeleted={(id) =>
                      setTags((current) => current.filter((item) => item.id !== id))
                    }
                    onError={handleError}
                  />
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

const fieldClass =
  "min-h-11 w-full rounded-xl border border-line bg-white/70 px-3 text-ink outline-none focus:border-seal";

function CreateCategoryForm({
  onCreated,
  onError,
}: {
  onCreated: (item: Category) => void;
  onError: (err: unknown, fallback: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      const created = await createCategory(name.trim(), slug.trim(), description.trim());
      onCreated(created);
      setName("");
      setSlug("");
      setDescription("");
      setOpen(false);
    } catch (err) {
      onError(err, "创建分类失败");
    } finally {
      setSaving(false);
    }
  }

  if (!open) {
    return (
      <AdminButton type="button" className="mt-5" onClick={() => setOpen(true)}>
        新建分类
      </AdminButton>
    );
  }

  return (
    <form onSubmit={onSubmit} className="mt-5 grid gap-3 rounded-sm border border-line bg-white/55 p-4 sm:grid-cols-2">
      <label className="grid gap-1.5 text-sm">
        <span className="text-mist">名称</span>
        <input
          required
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="例如：工程笔记"
          className={fieldClass}
        />
      </label>
      <label className="grid gap-1.5 text-sm">
        <span className="text-mist">路径 slug（可空）</span>
        <input
          value={slug}
          onChange={(event) => setSlug(event.target.value)}
          placeholder="engineering"
          className={`${fieldClass} font-mono`}
        />
      </label>
      <label className="grid gap-1.5 text-sm sm:col-span-2">
        <span className="text-mist">简介（可选）</span>
        <input
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="这一卷写什么"
          className={fieldClass}
        />
      </label>
      <div className="flex flex-wrap gap-2 sm:col-span-2">
        <AdminButton type="submit" variant="primary" disabled={saving}>
          {saving ? "创建中…" : "添加"}
        </AdminButton>
        <AdminButton type="button" onClick={() => setOpen(false)}>
          取消
        </AdminButton>
      </div>
    </form>
  );
}

function CreateTagForm({
  onCreated,
  onError,
}: {
  onCreated: (item: Tag) => void;
  onError: (err: unknown, fallback: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [saving, setSaving] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      const created = await createTag(name.trim(), slug.trim());
      onCreated(created);
      setName("");
      setSlug("");
      setOpen(false);
    } catch (err) {
      onError(err, "创建标签失败");
    } finally {
      setSaving(false);
    }
  }

  if (!open) {
    return (
      <AdminButton type="button" className="mt-5" onClick={() => setOpen(true)}>
        新建标签
      </AdminButton>
    );
  }

  return (
    <form onSubmit={onSubmit} className="mt-5 grid gap-3 rounded-sm border border-line bg-white/55 p-4 sm:grid-cols-2">
      <label className="grid gap-1.5 text-sm">
        <span className="text-mist">名称</span>
        <input
          required
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="例如：Next.js"
          className={fieldClass}
        />
      </label>
      <label className="grid gap-1.5 text-sm">
        <span className="text-mist">路径 slug（可空）</span>
        <input
          value={slug}
          onChange={(event) => setSlug(event.target.value)}
          placeholder="nextjs"
          className={`${fieldClass} font-mono`}
        />
      </label>
      <div className="flex flex-wrap gap-2 sm:col-span-2">
        <AdminButton type="submit" variant="primary" disabled={saving}>
          {saving ? "创建中…" : "添加"}
        </AdminButton>
        <AdminButton type="button" onClick={() => setOpen(false)}>
          取消
        </AdminButton>
      </div>
    </form>
  );
}

function CategoryRow({
  category,
  onChange,
  onDeleted,
  onError,
}: {
  category: Category;
  onChange: (item: Category) => void;
  onDeleted: (id: string) => void;
  onError: (err: unknown, fallback: string) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(category.name);
  const [slug, setSlug] = useState(category.slug);
  const [description, setDescription] = useState(category.description ?? "");
  const [saving, setSaving] = useState(false);

  async function onSave() {
    setSaving(true);
    try {
      let updated = await updateCategory(category.id, name.trim(), description.trim());
      if (slug.trim() && slug.trim() !== category.slug) {
        updated = await updateCategorySlug(category.id, slug.trim());
      }
      onChange(updated);
      setEditing(false);
    } catch (err) {
      onError(err, "保存分类失败");
    } finally {
      setSaving(false);
    }
  }

  async function onDelete() {
    if (!window.confirm(`删除分类「${category.name}」？`)) return;
    setSaving(true);
    try {
      await deleteCategory(category.id);
      onDeleted(category.id);
    } catch (err) {
      onError(err, "删除分类失败");
    } finally {
      setSaving(false);
    }
  }

  if (!editing) {
    return (
      <li>
        <button
          type="button"
          onClick={() => setEditing(true)}
          title={`编辑「${category.name}」`}
          className="cursor-pointer"
        >
          <CategoryMark name={category.name} />
        </button>
      </li>
    );
  }

  return (
    <li className="w-full basis-full">
      <div className="grid gap-3 rounded-sm border border-line bg-white/70 p-4 sm:grid-cols-[1fr_8rem_auto]">
        <label className="grid gap-1.5 text-sm">
          <span className="text-mist">名称</span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            className={fieldClass}
          />
        </label>
        <label className="grid gap-1.5 text-sm">
          <span className="text-mist">slug</span>
          <input
            value={slug}
            onChange={(event) => setSlug(event.target.value)}
            className={`${fieldClass} font-mono`}
          />
        </label>
        <div className="flex flex-wrap items-end gap-2">
          <AdminButton
            type="button"
            variant="primary"
            disabled={saving}
            onClick={() => void onSave()}
          >
            保存
          </AdminButton>
          <AdminButton
            type="button"
            onClick={() => {
              setName(category.name);
              setSlug(category.slug);
              setDescription(category.description ?? "");
              setEditing(false);
            }}
          >
            取消
          </AdminButton>
          <AdminButton
            type="button"
            variant="danger"
            disabled={saving}
            onClick={() => void onDelete()}
          >
            删除
          </AdminButton>
        </div>
        <label className="grid gap-1.5 text-sm sm:col-span-3">
          <span className="text-mist">简介（可选）</span>
          <input
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            className={fieldClass}
          />
        </label>
      </div>
    </li>
  );
}

function TagRow({
  tag,
  onChange,
  onDeleted,
  onError,
}: {
  tag: Tag;
  onChange: (item: Tag) => void;
  onDeleted: (id: string) => void;
  onError: (err: unknown, fallback: string) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(tag.name);
  const [slug, setSlug] = useState(tag.slug);
  const [saving, setSaving] = useState(false);

  async function onSave() {
    setSaving(true);
    try {
      let updated = await updateTag(tag.id, name.trim());
      if (slug.trim() && slug.trim() !== tag.slug) {
        updated = await updateTagSlug(tag.id, slug.trim());
      }
      onChange(updated);
      setEditing(false);
    } catch (err) {
      onError(err, "保存标签失败");
    } finally {
      setSaving(false);
    }
  }

  async function onDelete() {
    if (!window.confirm(`删除标签「${tag.name}」？`)) return;
    setSaving(true);
    try {
      await deleteTag(tag.id);
      onDeleted(tag.id);
    } catch (err) {
      onError(err, "删除标签失败");
    } finally {
      setSaving(false);
    }
  }

  if (!editing) {
    return (
      <li>
        <button
          type="button"
          onClick={() => setEditing(true)}
          title={`编辑 #${tag.name}`}
          className="cursor-pointer"
        >
          <TagMark name={tag.name} />
        </button>
      </li>
    );
  }

  return (
    <li className="w-full basis-full">
      <div className="grid gap-3 rounded-sm border border-line bg-white/70 p-4 sm:grid-cols-[1fr_8rem_auto]">
        <label className="grid gap-1.5 text-sm">
          <span className="text-mist">名称</span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            className={fieldClass}
          />
        </label>
        <label className="grid gap-1.5 text-sm">
          <span className="text-mist">slug</span>
          <input
            value={slug}
            onChange={(event) => setSlug(event.target.value)}
            className={`${fieldClass} font-mono`}
          />
        </label>
        <div className="flex flex-wrap items-end gap-2">
          <AdminButton
            type="button"
            variant="primary"
            disabled={saving}
            onClick={() => void onSave()}
          >
            保存
          </AdminButton>
          <AdminButton
            type="button"
            onClick={() => {
              setName(tag.name);
              setSlug(tag.slug);
              setEditing(false);
            }}
          >
            取消
          </AdminButton>
          <AdminButton
            type="button"
            variant="danger"
            disabled={saving}
            onClick={() => void onDelete()}
          >
            删除
          </AdminButton>
        </div>
      </div>
    </li>
  );
}
