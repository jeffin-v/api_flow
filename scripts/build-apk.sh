#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$ROOT_DIR/dist"

cd "$ROOT_DIR"
./gradlew --no-daemon clean :app:assembleDebug
mkdir -p "$DIST_DIR"
cp "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk" "$DIST_DIR/api-flow-debug.apk"
echo "Built $DIST_DIR/api-flow-debug.apk"
