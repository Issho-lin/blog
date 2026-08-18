type AdminUploadProgressProps = {
  label: string;
  percent: number;
};

export function AdminUploadProgress({ label, percent }: AdminUploadProgressProps) {
  const clamped = Math.min(100, Math.max(0, Math.round(percent)));
  return (
    <div className="space-y-1 text-xs text-mist">
      <div className="flex justify-between gap-2">
        <span className="min-w-0 truncate">{label}</span>
        <span className="shrink-0 tabular-nums">{clamped}%</span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-line">
        <div
          className="h-full rounded-full bg-seal transition-[width] duration-150"
          style={{ width: `${clamped}%` }}
        />
      </div>
    </div>
  );
}
