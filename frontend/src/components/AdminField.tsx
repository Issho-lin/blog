import type { InputHTMLAttributes, TextareaHTMLAttributes } from "react";

function cx(...parts: Array<string | false | undefined>) {
  return parts.filter(Boolean).join(" ");
}

export function AdminInput({
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx("admin-field", className)} {...props} />;
}

export function AdminTitleInput({
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx("admin-title-field", className)} {...props} />;
}

export function AdminTextarea({
  className,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cx("admin-field admin-field-area", className)} {...props} />;
}
