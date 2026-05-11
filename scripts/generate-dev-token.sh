#!/usr/bin/env bash
set -euo pipefail

USER_ID="550e8400-e29b-41d4-a716-446655440000"
EMAIL="dev@example.com"
ROLES="user,author"
TTL_MINUTES="60"
ISSUER="auth-service"
SECRET="${ACCESS_TOKEN_SECRET:-}"
RAW="false"

usage() {
  cat <<'EOF'
Usage: ./scripts/generate-dev-token.sh [options]

Options:
  --user-id UUID       JWT user_id claim. Must exist in user_profiles for most private endpoints.
  --email EMAIL        JWT email claim. Default: dev@example.com
  --roles LIST         Comma-separated roles. Default: user,author
  --ttl MINUTES        Token lifetime in minutes. Default: 60
  --issuer ISSUER      JWT issuer. Default: auth-service
  --secret SECRET      Signing secret. Defaults to ACCESS_TOKEN_SECRET env var.
  --raw                Print only token.
  -h, --help           Show this help.

Example:
  ACCESS_TOKEN_SECRET=dev-access-token-secret-change-me ./scripts/generate-dev-token.sh --roles user,author,admin
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --user-id)
      USER_ID="${2:?Missing value for --user-id}"
      shift 2
      ;;
    --email)
      EMAIL="${2:?Missing value for --email}"
      shift 2
      ;;
    --roles)
      ROLES="${2:?Missing value for --roles}"
      shift 2
      ;;
    --ttl)
      TTL_MINUTES="${2:?Missing value for --ttl}"
      shift 2
      ;;
    --issuer)
      ISSUER="${2:?Missing value for --issuer}"
      shift 2
      ;;
    --secret)
      SECRET="${2:?Missing value for --secret}"
      shift 2
      ;;
    --raw)
      RAW="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$SECRET" ]]; then
  SECRET="dev-access-token-secret-change-me"
fi

if [[ ! "$USER_ID" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]]; then
  echo "UserId must be a valid UUID. Received: $USER_ID" >&2
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required to sign the dev token" >&2
  exit 1
fi

b64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

json_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "$value"
}

roles_json=""
IFS=',' read -ra ROLE_ITEMS <<< "$ROLES"
for role in "${ROLE_ITEMS[@]}"; do
  role="$(echo "$role" | xargs | tr '[:upper:]' '[:lower:]')"
  if [[ -z "$role" ]]; then
    continue
  fi
  if [[ -n "$roles_json" ]]; then
    roles_json+=","
  fi
  roles_json+="\"$(json_escape "$role")\""
done

if [[ -z "$roles_json" ]]; then
  echo "At least one role is required" >&2
  exit 1
fi

exp="$(date -u -d "+${TTL_MINUTES} minutes" +%s 2>/dev/null || date -u -v+"${TTL_MINUTES}"M +%s)"
header='{"alg":"HS256","typ":"JWT"}'
payload="{\"user_id\":\"$(json_escape "$USER_ID")\",\"email\":\"$(json_escape "$EMAIL")\",\"roles\":[${roles_json}],\"iss\":\"$(json_escape "$ISSUER")\",\"exp\":${exp}}"

encoded_header="$(printf '%s' "$header" | b64url)"
encoded_payload="$(printf '%s' "$payload" | b64url)"
data="${encoded_header}.${encoded_payload}"
signature="$(printf '%s' "$data" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64url)"
token="${data}.${signature}"

if [[ "$RAW" == "true" ]]; then
  printf '%s\n' "$token"
  exit 0
fi

cat <<EOF
Dev JWT:
$token

Use in Swagger Authorize as:
Bearer $token

podcast-core must be started with the same secret:
export ACCESS_TOKEN_SECRET="$SECRET"
EOF
