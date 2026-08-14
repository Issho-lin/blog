import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";

const variants = {
  primary: "admin-btn admin-btn-primary",
  ghost: "admin-btn admin-btn-ghost",
  soft: "admin-btn admin-btn-soft",
  danger: "admin-btn admin-btn-danger",
} as const;

type Variant = keyof typeof variants;

type Shared = {
  variant?: Variant;
  className?: string;
  children: ReactNode;
};

type ButtonProps = Shared &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className"> & {
    href?: undefined;
  };

type LinkProps = Shared & {
  href: string;
  disabled?: boolean;
};

export function AdminButton(props: ButtonProps | LinkProps) {
  const variant = props.variant ?? "ghost";
  const className = [variants[variant], props.className].filter(Boolean).join(" ");

  if (props.href) {
    if (props.disabled) {
      return <span className={`${className} pointer-events-none opacity-50`}>{props.children}</span>;
    }
    return (
      <Link href={props.href} className={className}>
        {props.children}
      </Link>
    );
  }

  const { variant: _variant, className: _className, children, type = "button", ...rest } =
    props as ButtonProps;

  return (
    <button type={type} className={className} {...rest}>
      {children}
    </button>
  );
}
