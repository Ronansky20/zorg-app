#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
host="${FIREBASE_AUTH_EMULATOR_HOST:-127.0.0.1:9099}"
project="${ZORG_FIREBASE_PROJECT:-demo-zorg-app}"
password="${ZORG_DEV_PASSWORD:-zorg-dev-password}"

if [[ ! "$host" =~ ^(127\.0\.0\.1|localhost): ]]; then
    echo "Refusing to create accounts on $host: this script only talks to a local Auth emulator"
    exit 1
fi

ids=$(grep -oE '"[A-Za-z0-9_-]+"[[:space:]]*:[[:space:]]*\{' "$root/users.json" | cut -d'"' -f2 | grep -vx users)

curl -sf -X DELETE "http://$host/emulator/v1/projects/$project/accounts" > /dev/null
for id in $ids; do
    curl -sf -X POST -H "Authorization: Bearer owner" -H "Content-Type: application/json" \
        -d "{\"localId\":\"$id\",\"email\":\"$id@zorg.test\",\"password\":\"$password\"}" \
        "http://$host/identitytoolkit.googleapis.com/v1/projects/$project/accounts" > /dev/null
    echo "Created $id@zorg.test"
done
