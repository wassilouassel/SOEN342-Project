#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD="$(mktemp -d "${TMPDIR:-/tmp}/clinic-run.XXXXXX")"
trap 'rm -rf "$BUILD"' EXIT
javac --release 17 -encoding UTF-8 -d "$BUILD" "$ROOT"/src/clinic/*.java
java -Dfile.encoding=UTF-8 -cp "$BUILD" clinic.Main "$@"
