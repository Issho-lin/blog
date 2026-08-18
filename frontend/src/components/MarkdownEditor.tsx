"use client";

import { useEffect, useRef, useState } from "react";
import Vditor from "vditor";
import "vditor/dist/index.css";
import { AdminUploadProgress } from "@/components/AdminUploadProgress";
import { uploadImage } from "@/lib/api/posts";
import { installMermaidConfigPatch } from "@/lib/mermaid";

type MarkdownEditorProps = {
  /** 仅在挂载 / 强制重置时写入编辑器 */
  initialValue: string;
  /** 内容变化（自动保存用） */
  onChange: (markdown: string) => void;
  /** 外部强制重置时递增，例如冲突后重新加载 */
  resetToken?: number;
  disabled?: boolean;
};

type UploadUi = {
  label: string;
  percent: number;
};

async function uploadToMediaApi(
  files: File[],
  onProgress: (ui: UploadUi | null) => void
) {
  const succMap: Record<string, string> = {};
  const errFiles: string[] = [];

  for (let index = 0; index < files.length; index++) {
    const file = files[index];
    onProgress({
      label: `上传 ${file.name}（${index + 1}/${files.length}）`,
      percent: 0,
    });
    try {
      const uploaded = await uploadImage(file, {
        onProgress: (percent) =>
          onProgress({
            label: `上传 ${file.name}（${index + 1}/${files.length}）`,
            percent,
          }),
      });
      const name = uploaded.originalFilename || file.name;
      succMap[name] = uploaded.url;
    } catch {
      errFiles.push(file.name);
    }
  }

  onProgress(null);
  return { succMap, errFiles };
}

function safeDestroy(editor: Vditor | null) {
  if (!editor) return;
  try {
    // Strict Mode 下可能在 after 之前卸载，内部尚未就绪
    if (editor.vditor) {
      editor.destroy();
    }
  } catch {
    /* ignore */
  }
}

/**
 * 语雀风格即时渲染 Markdown 编辑器（Vditor IR，可切 WYSIWYG / 分屏）。
 */
export function MarkdownEditor({
  initialValue,
  onChange,
  resetToken = 0,
  disabled = false,
}: MarkdownEditorProps) {
  const hostRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<Vditor | null>(null);
  const readyRef = useRef(false);
  const onChangeRef = useRef(onChange);
  const initialRef = useRef(initialValue);
  const disabledRef = useRef(disabled);
  const pendingResetRef = useRef<number | null>(null);
  const generationRef = useRef(0);
  const [uploadUi, setUploadUi] = useState<UploadUi | null>(null);
  const uploadUiRef = useRef<(ui: UploadUi | null) => void>(() => {});
  uploadUiRef.current = setUploadUi;

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  useEffect(() => {
    initialRef.current = initialValue;
  }, [initialValue]);

  useEffect(() => {
    disabledRef.current = disabled;
    const editor = editorRef.current;
    if (!readyRef.current || !editor) return;
    if (disabled) {
      editor.disabled();
    } else {
      editor.enable();
    }
  }, [disabled]);

  useEffect(() => {
    if (!hostRef.current) return;

    let disposed = false;
    const host = hostRef.current;
    const generation = ++generationRef.current;
    // 避免 React Strict Mode 重复挂载时残留 DOM
    host.innerHTML = "";
    installMermaidConfigPatch();

    const editor = new Vditor(host, {
      // 本地静态资源，含 mermaid / plantuml 等绘图脚本
      cdn: "/vditor",
      height: Math.max(520, window.innerHeight - 300),
      mode: "ir",
      placeholder:
        "开始写作… 支持 ```mermaid 文本绘图，可用工具栏插入示意图",
      cache: { enable: false },
      toolbarConfig: { pin: true },
      counter: { enable: true, type: "text" },
      preview: {
        delay: 200,
        hljs: { style: "github", lineNumber: true },
        markdown: {
          toc: true,
          mark: true,
        },
      },
      tab: "    ",
      outline: { enable: true, position: "right" },
      toolbar: [
        "headings",
        "bold",
        "italic",
        "strike",
        "link",
        "|",
        "list",
        "ordered-list",
        "check",
        "outdent",
        "indent",
        "|",
        "quote",
        "line",
        "code",
        "inline-code",
        "table",
        "|",
        {
          name: "mermaid",
          tip: "插入 Mermaid 图",
          className: "vditor-menu__mermaid",
          icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M10 6.5h4M6.5 10v4M17.5 10v4M10 17.5h4"/></svg>',
          click() {
            if (!editorRef.current || !readyRef.current) return;
            editorRef.current.insertValue(
              [
                "",
                "```mermaid",
                "flowchart TD",
                "  A[开始] --> B{判断}",
                "  B -->|是| C[处理]",
                "  B -->|否| D[结束]",
                "  C --> D",
                "```",
                "",
              ].join("\n")
            );
          },
        },
        "upload",
        "|",
        "undo",
        "redo",
        "|",
        "fullscreen",
        "edit-mode",
        "outline",
        "preview",
      ],
      upload: {
        accept: "image/jpeg,image/png,image/gif,image/webp",
        multiple: true,
        handler: async (files) => {
          const list = Array.from(files);
          const { succMap, errFiles } = await uploadToMediaApi(list, (ui) =>
            uploadUiRef.current(ui)
          );
          const names = Object.keys(succMap);
          if (names.length > 0 && editorRef.current && readyRef.current) {
            const markdown = names
              .map((name) => `![${name}](${succMap[name]})`)
              .join("\n");
            editorRef.current.insertValue(markdown);
          }
          if (errFiles.length > 0) {
            return `上传失败：${errFiles.join("、")}`;
          }
          return names.length > 0 ? "图片已插入" : "没有可上传的图片";
        },
      },
      input: (value) => {
        onChangeRef.current(value);
      },
      after: () => {
        // Strict Mode 下第一次实例的 after 会晚于第二次挂载到达；
        // 不能 destroy，否则会拆掉当前正在用的同一 host。
        if (disposed || generation !== generationRef.current) {
          return;
        }
        readyRef.current = true;
        editorRef.current = editor;
        editor.setValue(initialRef.current || "", true);
        if (disabledRef.current) {
          editor.disabled();
        }
        // 初始化完成前若已有重置请求，补一次
        if (pendingResetRef.current !== null) {
          editor.setValue(initialRef.current || "", true);
          pendingResetRef.current = null;
        }
      },
    });

    return () => {
      disposed = true;
      if (generation !== generationRef.current) {
        return;
      }
      readyRef.current = false;
      editorRef.current = null;
      safeDestroy(editor);
    };
  }, []);

  useEffect(() => {
    if (!readyRef.current || !editorRef.current) {
      pendingResetRef.current = resetToken;
      return;
    }
    editorRef.current.setValue(initialRef.current || "", true);
  }, [resetToken]);

  return (
    <div className="markdown-editor-shell space-y-2">
      {uploadUi ? (
        <AdminUploadProgress label={uploadUi.label} percent={uploadUi.percent} />
      ) : null}
      <div ref={hostRef} className="vditor-host" />
    </div>
  );
}
