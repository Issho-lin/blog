import { cpSync, mkdirSync, rmSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const source = join(root, "node_modules", "vditor", "dist");
const target = join(root, "public", "vditor", "dist");

rmSync(join(root, "public", "vditor"), { recursive: true, force: true });
mkdirSync(dirname(target), { recursive: true });
cpSync(source, target, { recursive: true });
console.log("copied vditor assets -> public/vditor/dist");
