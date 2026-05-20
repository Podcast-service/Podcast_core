#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

"${ROOT_DIR}/scripts/generate-dev-token.sh" \
  --user-id "00000000-0000-0000-0000-000000000001" \
  --email "dev.user@example.local" \
  --roles "user,author" \
  --ttl "10080" \
  --output ".dev/jwt/author-token.txt" \
  "$@"
