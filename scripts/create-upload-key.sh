#!/usr/bin/env bash
set -euo pipefail

# Run once before the first Play release. Back up the resulting keystore securely; losing it can
# prevent future updates outside of Play App Signing. Do not commit this key or its properties.
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
KEY_DIR="$ROOT_DIR/.local-signing"
KEYSTORE="$KEY_DIR/api-flow-upload.jks"
PROPERTIES_FILE="$ROOT_DIR/keystore.properties"

: "${API_FLOW_STORE_PASSWORD:?Set API_FLOW_STORE_PASSWORD to a strong, unique password.}"
: "${API_FLOW_KEY_PASSWORD:?Set API_FLOW_KEY_PASSWORD to a strong, unique password.}"
: "${API_FLOW_KEY_ALIAS:=api-flow-upload}"
: "${API_FLOW_KEY_DNAME:=CN=API Flow, OU=Mobile, O=Your Publisher, C=US}"

if [[ -e "$KEYSTORE" || -e "$PROPERTIES_FILE" ]]; then
  echo "A release keystore or keystore.properties already exists; refusing to overwrite it." >&2
  exit 1
fi

mkdir -p "$KEY_DIR"
keytool -genkeypair -v -keystore "$KEYSTORE" -storetype JKS \
  -storepass "$API_FLOW_STORE_PASSWORD" -keypass "$API_FLOW_KEY_PASSWORD" \
  -alias "$API_FLOW_KEY_ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "$API_FLOW_KEY_DNAME"

printf 'storeFile=.local-signing/api-flow-upload.jks\nstorePassword=%s\nkeyAlias=%s\nkeyPassword=%s\n' \
  "$API_FLOW_STORE_PASSWORD" "$API_FLOW_KEY_ALIAS" "$API_FLOW_KEY_PASSWORD" > "$PROPERTIES_FILE"
chmod 600 "$KEYSTORE" "$PROPERTIES_FILE"
echo "Created a private upload key and keystore.properties. Back up $KEYSTORE securely."
