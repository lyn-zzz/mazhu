#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_DIR="$ROOT_DIR/skills/mazhu-knowledge"
TARGET_BASE="${CODEX_HOME:-$HOME/.codex}/skills"
TARGET_DIR="$TARGET_BASE/mazhu-knowledge"

if [[ ! -f "$SOURCE_DIR/SKILL.md" ]]; then
  echo "Cannot find skill source: $SOURCE_DIR" >&2
  exit 1
fi

mkdir -p "$TARGET_BASE"
rm -rf "$TARGET_DIR"
cp -R "$SOURCE_DIR" "$TARGET_DIR"
python3 - "$TARGET_DIR/SKILL.md" "$ROOT_DIR" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
root = sys.argv[2]
text = path.read_text(encoding="utf-8")
path.write_text(text.replace("__MAZHU_PROJECT_ROOT__", root), encoding="utf-8")
PY

echo "Installed mazhu-knowledge skill to $TARGET_DIR"
