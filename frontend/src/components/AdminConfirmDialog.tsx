"use client";

import * as AlertDialog from "@radix-ui/react-alert-dialog";
import { AdminButton } from "@/components/AdminButton";

type AdminConfirmDialogProps = {
  title: string;
  message: string;
  confirmLabel: string;
  confirmVariant?: "danger" | "primary";
  onCancel: () => void;
  onConfirm: () => void;
};

export function AdminConfirmDialog({
  title,
  message,
  confirmLabel,
  confirmVariant = "danger",
  onCancel,
  onConfirm,
}: AdminConfirmDialogProps) {
  return (
    <AlertDialog.Root
      open
      onOpenChange={(next) => {
        if (!next) onCancel();
      }}
    >
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="admin-dialog-overlay" />
        <AlertDialog.Content className="admin-dialog-content">
          <AlertDialog.Title className="admin-dialog-title">{title}</AlertDialog.Title>
          <AlertDialog.Description className="admin-dialog-desc">
            {message}
          </AlertDialog.Description>
          <div className="admin-dialog-actions">
            <AlertDialog.Cancel asChild>
              <AdminButton type="button">取消</AdminButton>
            </AlertDialog.Cancel>
            <AdminButton
              type="button"
              variant={confirmVariant}
              onClick={onConfirm}
            >
              {confirmLabel}
            </AdminButton>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
