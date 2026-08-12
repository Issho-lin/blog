export function SealMark({
  className = "",
  size = 36,
}: {
  className?: string;
  size?: number;
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      className={className}
    >
      <rect
        x="3"
        y="3"
        width="58"
        height="58"
        rx="4"
        stroke="currentColor"
        strokeWidth="3.5"
      />
      <rect
        x="9"
        y="9"
        width="46"
        height="46"
        rx="2"
        stroke="currentColor"
        strokeWidth="1.25"
        opacity="0.55"
      />
      <text
        x="32"
        y="42.5"
        textAnchor="middle"
        fill="currentColor"
        fontSize="28"
        fontWeight="700"
        fontFamily="var(--font-noto-serif), 'Noto Serif SC', serif"
        style={{ userSelect: "none" }}
      >
        林
      </text>
    </svg>
  );
}
