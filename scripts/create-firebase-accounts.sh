#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
default_project=$(grep -oE '"default"[[:space:]]*:[[:space:]]*"[^"]+"' "$root/.firebaserc" | cut -d'"' -f4)
project="${ZORG_FIREBASE_PROJECT:-$default_project}"
token="${ZORG_FIREBASE_ADMIN_TOKEN:-}"

if [[ -z "$project" || "$project" == demo-* ]]; then
    echo "No real project id; set it in .firebaserc or ZORG_FIREBASE_PROJECT (use create-emulator-accounts.sh for the emulator)"
    exit 1
fi
if [[ -z "$token" ]]; then
    echo "Set ZORG_FIREBASE_ADMIN_TOKEN to a Google access token, e.g. \$(gcloud auth application-default print-access-token)"
    exit 1
fi

password="${ZORG_ACCOUNT_PASSWORD:-}"
if [[ -z "$password" ]]; then
    read -rsp "Password for the new logins in $project: " password
    echo
fi
if (( ${#password} < 6 )) || [[ "$password" == *\"* || "$password" == *\\* ]]; then
    echo "The password needs at least 6 characters and no \" or \\"
    exit 1
fi

ids=$(grep -oE '"[A-Za-z0-9_-]+"[[:space:]]*:[[:space:]]*\{' "$root/users.json" | cut -d'"' -f2 | grep -vx users)

for id in $ids; do
    response=$(curl -s -w '\n%{http_code}' -X POST \
        -H "Authorization: Bearer $token" \
        -H "X-Goog-User-Project: $project" \
        -H "Content-Type: application/json" \
        -d "{\"localId\":\"$id\",\"email\":\"$id@zorg.test\",\"password\":\"$password\"}" \
        "https://identitytoolkit.googleapis.com/v1/projects/$project/accounts")
    status="${response##*$'\n'}"
    body="${response%$'\n'*}"
    if [[ "$status" == 200 ]]; then
        echo "Created $id@zorg.test"
    elif [[ "$body" == *DUPLICATE_LOCAL_ID* || "$body" == *EMAIL_EXISTS* ]]; then
        echo "Already exists: $id@zorg.test"
    else
        echo "Failed for $id: HTTP $status"
        echo "$body"
        exit 1
    fi
done
