type MermaidApi = {
  initialize: (config: Record<string, unknown>) => void;
  __blogPatched?: boolean;
};

/** 与 Vditor 默认预览一致：蓝色节点，仅去掉画布白底。 */
export const mermaidConfig: Record<string, unknown> = {
  startOnLoad: false,
  securityLevel: "loose",
  theme: "default",
  fontFamily: "inherit",
  themeVariables: {
    background: "transparent",
  },
  flowchart: {
    htmlLabels: true,
    useMaxWidth: true,
  },
  sequence: {
    useMaxWidth: true,
    diagramMarginX: 8,
    diagramMarginY: 8,
    boxMargin: 8,
    showSequenceNumbers: true,
  },
};

export function applyMermaidConfig(mermaid: { initialize: (config: Record<string, unknown>) => void }) {
  mermaid.initialize(mermaidConfig);
}

function patchMermaid(mermaid: MermaidApi) {
  if (mermaid.__blogPatched) return mermaid;
  const original = mermaid.initialize.bind(mermaid);
  mermaid.initialize = (config: Record<string, unknown>) => {
    original({
      ...config,
      ...mermaidConfig,
      theme: "default",
      themeVariables: {
        ...(config.themeVariables as object | undefined),
        background: "transparent",
      },
    });
  };
  mermaid.__blogPatched = true;
  return mermaid;
}

/**
 * Vditor 会单独加载 window.mermaid 并覆盖 initialize。
 * 在创建编辑器前打补丁，保证预览图与公开页同一套主题。
 */
export function installMermaidConfigPatch() {
  if (typeof window === "undefined") return;

  const holder = window as Window & { mermaid?: MermaidApi };
  const existing = Object.getOwnPropertyDescriptor(holder, "mermaid");
  if (existing && !existing.configurable && existing.value) {
    patchMermaid(existing.value);
    return;
  }

  let current = holder.mermaid;
  if (current) patchMermaid(current);

  Object.defineProperty(holder, "mermaid", {
    configurable: true,
    enumerable: true,
    get() {
      return current;
    },
    set(value: MermaidApi | undefined) {
      current = value ? patchMermaid(value) : value;
    },
  });
}
