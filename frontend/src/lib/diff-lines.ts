export type DiffLine = {
  type: "same" | "add" | "remove";
  text: string;
};

export function diffLines(before: string, after: string): DiffLine[] {
  const a = before.split("\n");
  const b = after.split("\n");
  const n = a.length;
  const m = b.length;
  const dp: number[][] = Array.from({ length: n + 1 }, () => Array(m + 1).fill(0));
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      dp[i][j] = a[i] === b[j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1]);
    }
  }
  const lines: DiffLine[] = [];
  let i = 0;
  let j = 0;
  while (i < n && j < m) {
    if (a[i] === b[j]) {
      lines.push({ type: "same", text: a[i] });
      i += 1;
      j += 1;
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      lines.push({ type: "remove", text: a[i] });
      i += 1;
    } else {
      lines.push({ type: "add", text: b[j] });
      j += 1;
    }
  }
  while (i < n) {
    lines.push({ type: "remove", text: a[i] });
    i += 1;
  }
  while (j < m) {
    lines.push({ type: "add", text: b[j] });
    j += 1;
  }
  return lines;
}
