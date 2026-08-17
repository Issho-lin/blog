"use client";

import Link from "next/link";
import { Slot } from "@radix-ui/react-slot";
import {
  type ButtonHTMLAttributes,
  type ReactNode,
  forwardRef,
} from "react";

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
    asChild?: boolean;
  };

type LinkProps = Shared & {
  href: string;
  disabled?: boolean;
};

function cx(...parts: Array<string | false | undefined>) {
  return parts.filter(Boolean).join(" ");
}

export const AdminButton = forwardRef<HTMLButtonElement, ButtonProps | LinkProps>(
  function AdminButton(props, ref) {
    const variant = props.variant ?? "ghost";
    const className = cx(variants[variant], props.className);

    if ("href" in props && props.href) {
      if (props.disabled) {
        return (
          <span className={cx(className, "pointer-events-none opacity-50")}>
            {props.children}
          </span>
        );
      }
      return (
        <Link href={props.href} className={className}>
          {props.children}
        </Link>
      );
    }

    const {
      variant: _variant,
      className: _className,
      children,
      type = "button",
      asChild,
      ...rest
    } = props as ButtonProps;

    const Comp = asChild ? Slot : "button";

    return (
      <Comp
        type={asChild ? undefined : type}
        className={className}
        ref={ref}
        {...rest}
      >
        {children}
      </Comp>
    );
  }
);
