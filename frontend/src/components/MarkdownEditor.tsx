"use client";

import { useEffect, useRef } from "react";
import Vditor from "vditor";
import "vditor/dist/index.css";

type MarkdownEditorProps = {
  /** 仅在挂载 / 强制重置时写入编辑器 */
  initialValue: string;
  /** 内容变化（自动保存用） */
  onChange: (markdown: string) => void;
  /** 外部强制重置时递增，例如冲突后重新加载 */
  resetToken?: number;
  disabled?: boolean;
};

async function uploadToMediaApi(files: File[]) {
  const succMap: Record<string, string> = {};
  const errFiles: string[] = [];

  for (const file of files) {
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await fetch("/api/admin/media/images", {
        method: "POST",
        body,
        credentials: "include",
      });
      const payload = (await response.json()) as {
        code?: string;
        message?: string;
        data?: { url?: string; originalFilename?: string };
      };
      if (!response.ok || payload.code !== "OK" || !payload.data?.url) {
        errFiles.push(file.name);
        continue;
      }
      const name = payload.data.originalFilename || file.name;
      succMap[name] = payload.data.url;
    } catch {
      errFiles.push(file.name);
    }
  }

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
    // 避免 React Strict Mode 重复挂载时残留 DOM
    host.innerHTML = "";

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
          const { succMap, errFiles } = await uploadToMediaApi(list);
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
        if (disposed) {
          safeDestroy(editor);
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
    <div className="markdown-editor-shell">
      <div ref={hostRef} className="vditor-host" />
    </div>
  );
}
