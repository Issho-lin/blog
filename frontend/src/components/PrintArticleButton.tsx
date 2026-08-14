"use client";

export function PrintArticleButton() {
  return (
    <button
      type="button"
      className="print-hide cursor-pointer text-sm text-mist transition-colors duration-200 hover:text-seal"
      onClick={() => window.print()}
    >
      打印本文
    </button>
  );
}
