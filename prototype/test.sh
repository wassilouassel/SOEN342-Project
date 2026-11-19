#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD="$(mktemp -d "${TMPDIR:-/tmp}/clinic-test.XXXXXX")"
trap 'rm -rf "$BUILD"' EXIT
javac --release 17 -encoding UTF-8 -Xlint:all -d "$BUILD" "$ROOT"/src/clinic/*.java "$ROOT"/test/clinic/*.java
java -ea -Dfile.encoding=UTF-8 -cp "$BUILD" clinic.ClinicSystemTest "$ROOT"
