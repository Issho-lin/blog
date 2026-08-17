"use client";

import * as Checkbox from "@radix-ui/react-checkbox";

function cx(...parts: Array<string | false | undefined>) {
  return parts.filter(Boolean).join(" ");
}

export function AdminCheckbox({
  checked,
  onCheckedChange,
  className,
  "aria-label": ariaLabel,
}: {
  checked: boolean | "indeterminate";
  onCheckedChange: (checked: boolean) => void;
  className?: string;
  "aria-label"?: string;
}) {
  return (
    <Checkbox.Root
      checked={checked}
      onCheckedChange={(value) => onCheckedChange(value === true)}
      aria-label={ariaLabel}
      className={cx("admin-check", className)}
    >
      <Checkbox.Indicator className="admin-check-mark">
        {checked === "indeterminate" ? (
          <span className="admin-check-dash" />
        ) : (
          <svg viewBox="0 0 12 12" width="10" height="10" aria-hidden>
            <path
              d="M2 6.2 4.7 9 10 3"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.6"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        )}
      </Checkbox.Indicator>
    </Checkbox.Root>
  );
}
