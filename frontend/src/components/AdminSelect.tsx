"use client";

import * as Select from "@radix-ui/react-select";

export type AdminSelectOption = {
  value: string;
  label: string;
};

const EMPTY = "__empty__";

function cx(...parts: Array<string | false | undefined>) {
  return parts.filter(Boolean).join(" ");
}

export function AdminSelect({
  value,
  onValueChange,
  options,
  placeholder,
  "aria-label": ariaLabel,
  className,
}: {
  value: string;
  onValueChange: (value: string) => void;
  options: AdminSelectOption[];
  placeholder?: string;
  "aria-label"?: string;
  className?: string;
}) {
  const resolved = value === "" ? EMPTY : value;

  return (
    <Select.Root
      value={resolved}
      onValueChange={(next) => onValueChange(next === EMPTY ? "" : next)}
    >
      <Select.Trigger
        aria-label={ariaLabel}
        className={cx("admin-field admin-select-trigger", className)}
      >
        <Select.Value placeholder={placeholder} />
        <Select.Icon className="admin-select-icon" aria-hidden>
          <svg viewBox="0 0 12 12" width="12" height="12">
            <path
              d="M2.5 4.5 6 8l3.5-3.5"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.4"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </Select.Icon>
      </Select.Trigger>
      <Select.Portal>
        <Select.Content
          className="admin-select-content"
          position="popper"
          sideOffset={6}
        >
          <Select.Viewport className="admin-select-viewport">
            {options.map((option) => (
              <Select.Item
                key={option.value === "" ? EMPTY : option.value}
                value={option.value === "" ? EMPTY : option.value}
                className="admin-select-item"
              >
                <Select.ItemText>{option.label}</Select.ItemText>
              </Select.Item>
            ))}
          </Select.Viewport>
        </Select.Content>
      </Select.Portal>
    </Select.Root>
  );
}
