#!/usr/bin/env bash
# Starts the backend and the Vite dev server together; Ctrl+C stops everything.
# Usage: ./scripts/dev.sh             JSON mode (patients.json / users.json)
#        ./scripts/dev.sh --firebase  local Firebase emulators, seeded from the JSON files
# From frontend/: npm run dev / npm run dev:firebase
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
trap 'trap - INT TERM EXIT; kill $(jobs -p) 2> /dev/null; wait' INT TERM EXIT

wait_for() {
    until curl -s -o /dev/null "http://$1"; do sleep 1; done
}

if [[ "${1:-}" == "--firebase" ]]; then
    export ZORG_FIREBASE_URL="http://127.0.0.1:9000?ns=demo-zorg-app-default-rtdb"
    export FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099

    (cd "$root" && exec npx -y firebase-tools@15.33.0 emulators:start --only database,auth --project demo-zorg-app) &
    wait_for 127.0.0.1:9000
    wait_for 127.0.0.1:9099

    "$root/scripts/create-emulator-accounts.sh"
    (cd "$root" && ZORG_FIREBASE_ADMIN_TOKEN=owner ./gradlew migrateToFirebase -q --console=plain)
fi

[[ -d "$root/frontend/node_modules" ]] || (cd "$root/frontend" && npm install)

(cd "$root" && exec ./gradlew bootRun --console=plain) &
(cd "$root/frontend" && exec ./node_modules/.bin/vite) &
wait
