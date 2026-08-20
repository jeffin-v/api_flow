#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$ROOT_DIR/dist"

if [[ ! -f "$ROOT_DIR/keystore.properties" ]]; then
  echo "No release signing configuration found. Run scripts/create-upload-key.sh first." >&2
  exit 1
fi

cd "$ROOT_DIR"
./gradlew --no-daemon clean :app:bundleRelease
mkdir -p "$DIST_DIR"
cp "$ROOT_DIR/app/build/outputs/bundle/release/app-release.aab" "$DIST_DIR/api-flow-release.aab"
jarsigner -verify -strict -certs "$DIST_DIR/api-flow-release.aab"
echo "Built signed $DIST_DIR/api-flow-release.aab"
