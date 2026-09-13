#!/usr/bin/env bash
# Upload Android release signing secrets to GitHub Actions (fromchat-messenger/app).
# Run locally on a machine that has the keystore and google-services.json.
set -euo pipefail

REPO="${GITHUB_REPO:-fromchat-messenger/app}"
KEYSTORE="${ANDROID_KEYSTORE_PATH:-app/android/keys/release.jks}"
GOOGLE_SERVICES="${ANDROID_GOOGLE_SERVICES_PATH:-app/android/google-services.json}"
PROPS="${ANDROID_KEYSTORE_PROPS_PATH:-app/android/keys/keystore.properties}"

usage() {
  cat <<'EOF'
Usage: scripts/set-android-github-secrets.sh

Environment:
  GITHUB_REPO                  default: fromchat-messenger/app
  ANDROID_KEYSTORE_PATH        default: app/android/keys/release.jks
  ANDROID_GOOGLE_SERVICES_PATH default: app/android/google-services.json
  ANDROID_KEYSTORE_PROPS_PATH  default: app/android/keys/keystore.properties
  ANDROID_RELEASE_STORE_PASSWORD  optional if present in keystore.properties
  ANDROID_KEY_PASSWORD            optional if present in keystore.properties

Requires: gh auth login, base64, keytool (optional)
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI is required. Install: https://cli.github.com/" >&2
  exit 1
fi

if [[ ! -f "$KEYSTORE" ]]; then
  echo "Missing release keystore: $KEYSTORE" >&2
  exit 1
fi
if [[ ! -f "$GOOGLE_SERVICES" ]]; then
  echo "Missing google-services.json: $GOOGLE_SERVICES" >&2
  exit 1
fi

read_prop() {
  local key="$1"
  if [[ -f "$PROPS" ]]; then
    grep -E "^${key}=" "$PROPS" | head -n1 | cut -d= -f2- || true
  fi
}

STORE_PASSWORD="${ANDROID_RELEASE_STORE_PASSWORD:-$(read_prop releaseStorePassword)}"
KEY_PASSWORD="${ANDROID_KEY_PASSWORD:-$(read_prop releaseKeyPassword)}"

if [[ -z "$STORE_PASSWORD" || -z "$KEY_PASSWORD" ]]; then
  echo "Set ANDROID_RELEASE_STORE_PASSWORD and ANDROID_KEY_PASSWORD, or add them to $PROPS" >&2
  exit 1
fi

KEYSTORE_B64="$(base64 <"$KEYSTORE" | tr -d '\n')"
GOOGLE_B64="$(base64 <"$GOOGLE_SERVICES" | tr -d '\n')"

echo "Setting secrets on $REPO ..."
gh secret set ANDROID_KEYSTORE_BASE64 --repo "$REPO" --body "$KEYSTORE_B64"
gh secret set ANDROID_GOOGLE_SERVICES_JSON --repo "$REPO" --body "$GOOGLE_B64"
gh secret set ANDROID_RELEASE_STORE_PASSWORD --repo "$REPO" --body "$STORE_PASSWORD"
gh secret set ANDROID_KEY_PASSWORD --repo "$REPO" --body "$KEY_PASSWORD"

echo "Done. Verify with: gh secret list --repo $REPO"
