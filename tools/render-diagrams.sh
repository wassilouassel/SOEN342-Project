#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="$ROOT/diagrams/iteration-2"
OUT="$BASE/rendered"
FORMAT="${1:-svg}"
case "$FORMAT" in svg|png) ;; *) echo "Usage: $0 [svg|png]" >&2; exit 2 ;; esac
mkdir -p "$OUT"
if [[ -n "${PLANTUML_JAR:-}" ]]; then
  RENDER=(java -Djava.awt.headless=true -jar "$PLANTUML_JAR")
elif command -v plantuml >/dev/null 2>&1; then
  RENDER=(plantuml)
else
  printf '%s\n' 'Install PlantUML or set PLANTUML_JAR=/absolute/path/to/plantuml.jar.' >&2
  exit 1
fi
# Separate categories prevent identical SSD/interaction names from colliding.
for category in analysis ssd interactions design; do
  mkdir -p "$OUT/$category"
  for source in "$BASE/$category/"*.puml; do
    [[ -f "$source" ]] || continue
    "${RENDER[@]}" -charset UTF-8 -failfast2 "-t$FORMAT" -o "$OUT/$category" "$source"
  done
done
printf 'Rendered %s diagrams under %s\n' "$FORMAT" "$OUT"
