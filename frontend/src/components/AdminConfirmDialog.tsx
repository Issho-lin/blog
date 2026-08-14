"use client";

import { AdminButton } from "@/components/AdminButton";

type AdminConfirmDialogProps = {
  title: string;
  message: string;
  confirmLabel: string;
  onCancel: () => void;
  onConfirm: () => void;
};

export function AdminConfirmDialog({
  title,
  message,
  confirmLabel,
  onCancel,
  onConfirm,
}: AdminConfirmDialogProps) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/35 px-5"
      role="dialog"
      aria-modal="true"
      aria-labelledby="admin-confirm-title"
    >
      <div className="w-full max-w-md rounded-xl border border-line bg-paper px-5 py-5 shadow-lg">
        <h2 id="admin-confirm-title" className="text-base font-medium text-ink">
          {title}
        </h2>
        <p className="mt-2 text-sm leading-6 text-mist">{message}</p>
        <div className="mt-5 flex justify-end gap-2">
          <AdminButton type="button" onClick={onCancel}>
            取消
          </AdminButton>
          <AdminButton type="button" variant="danger" onClick={onConfirm}>
            {confirmLabel}
          </AdminButton>
        </div>
      </div>
    </div>
  );
}
